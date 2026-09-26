package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.EstadoNotificacion;
import org.dentalcrm.domain.automatizacion.EventoAutomatizacion;
import org.dentalcrm.domain.automatizacion.Notificacion;
import org.dentalcrm.domain.automatizacion.NotificacionRepository;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaModificadaEvent;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.EstadoSesionWhatsapp;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.domain.whatsapp.WhatsappSesionRepository;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private AutomatizacionService automatizacionService;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private MessagingProvider messagingProvider;
    @Mock
    private WhatsappSesionRepository sesionRepository;
    @Mock
    private CorreoService correoService;

    private NotificacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionService(notificacionRepository, automatizacionService,
                citaRepository, messagingProvider, sesionRepository, correoService);
    }

    private Notificacion notificacion(Long id, String telefono) {
        Cita cita = new Cita();
        cita.setId(100L);
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        cita.setPaciente(paciente);
        Notificacion n = new Notificacion();
        n.setId(id);
        n.setCita(cita);
        n.setPaciente(paciente);
        n.setTelefono(telefono);
        n.setMensajeGenerado("Hola {{nombre}}");
        n.setEstado(EstadoNotificacion.PENDIENTE);
        n.setProgramadaAt(Instant.now().minusSeconds(10));
        return n;
    }

    private WhatsappSesion sesionConectada() {
        WhatsappSesion s = new WhatsappSesion();
        s.setId(1L);
        s.setSesionId("principal");
        s.setEstado(EstadoSesionWhatsapp.CONECTADA);
        return s;
    }

    @Test
    void procesarEnviaYMarcaEnviada() {
        Notificacion n = notificacion(1L, "+593998877665");
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA))
                .thenReturn(List.of(sesionConectada()));

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.ENVIADA, n.getEstado());
        assertNotNull(n.getEnviadaAt());
        verify(messagingProvider).enviarMensaje("principal", "+593998877665", "Hola {{nombre}}");
        verify(notificacionRepository, times(2)).save(n);
    }

    @Test
    void procesarSinTelefonoQuedaEnError() {
        Notificacion n = notificacion(2L, null);

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.ERROR, n.getEstado());
        verify(messagingProvider, never()).enviarMensaje(anyString(), anyString(), anyString());
    }

    @Test
    void procesarSinSesionConectadaReintenta() {
        Notificacion n = notificacion(3L, "+593998877665");
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA)).thenReturn(List.of());

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
        assertEquals(1, n.getIntentos());
        assertNotNull(n.getError());
    }

    @Test
    void procesarSinSesionTrasMaxIntentosQuedaEnError() {
        Notificacion n = notificacion(4L, "+593998877665");
        n.setIntentos(3);
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA)).thenReturn(List.of());

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.ERROR, n.getEstado());
        assertEquals(3, n.getIntentos());
    }

    @Test
    void procesarConFalloDelProviderReintenta() {
        Notificacion n = notificacion(5L, "+593998877665");
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA))
                .thenReturn(List.of(sesionConectada()));
        doThrow(new RuntimeException("OpenWA caído")).when(messagingProvider)
                .enviarMensaje("principal", "+593998877665", "Hola {{nombre}}");

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
        assertEquals(1, n.getIntentos());
        assertTrue(n.getError().contains("OpenWA"));
    }

    @Test
    void procesarConFalloFinalQuedaEnError() {
        Notificacion n = notificacion(6L, "+593998877665");
        n.setIntentos(3);
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA))
                .thenReturn(List.of(sesionConectada()));
        doThrow(new RuntimeException("Fallo")).when(messagingProvider)
                .enviarMensaje("principal", "+593998877665", "Hola {{nombre}}");

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.ERROR, n.getEstado());
        assertEquals(3, n.getIntentos());
    }

    @Test
    void cancelarActivasDeCitaMarcaCancelada() {
        Notificacion pendiente = notificacion(7L, "+593998877665");
        when(notificacionRepository.activasDeCita(100L)).thenReturn(List.of(pendiente));

        servicio.cancelarActivasDeCita(100L, "Cita confirmada");

        assertEquals(EstadoNotificacion.CANCELADA, pendiente.getEstado());
        verify(notificacionRepository).save(pendiente);
    }

    // ------------------------------------------------------------------
    // Aviso de cambio de hora/fecha
    // ------------------------------------------------------------------

    @Test
    void reprogramarLaCitaGeneraElAvisoConElHorarioAnterior() {
        Cita cita = citaConPaciente();
        when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(notificacionRepository.activasDeCita(100L)).thenReturn(List.of());

        servicio.onCitaModificada(new CitaModificadaEvent(100L,
                LocalDate.of(2026, 9, 15), LocalTime.of(10, 0),
                LocalDate.of(2026, 9, 17), LocalTime.of(16, 30)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> extra = ArgumentCaptor.forClass(Map.class);
        verify(automatizacionService).generar(any(Cita.class), extra.capture(),
                eq(EventoAutomatizacion.CITA_MODIFICADA),
                eq(EventoAutomatizacion.CITA_CREADA),
                eq(EventoAutomatizacion.CITA_PROXIMA));
        assertEquals("15/09/2026", extra.getValue().get("fecha_anterior"));
        assertEquals("10:00", extra.getValue().get("hora_anterior"));
    }

    @Test
    void cambiarElServicioSinMoverElHorarioNoPideAvisoDeReprogramacion() {
        Cita cita = citaConPaciente();
        when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(notificacionRepository.activasDeCita(100L)).thenReturn(List.of());

        servicio.onCitaModificada(new CitaModificadaEvent(100L,
                LocalDate.of(2026, 9, 15), LocalTime.of(10, 0),
                LocalDate.of(2026, 9, 15), LocalTime.of(10, 0)));

        verify(automatizacionService).generar(any(Cita.class),
                eq(EventoAutomatizacion.CITA_CREADA), eq(EventoAutomatizacion.CITA_PROXIMA));
    }

    @Test
    void elAvisoDeReprogramacionTambienSalePorCorreo() {
        Notificacion n = notificacion(8L, "+593998877665");
        n.getPaciente().setEmail("maria@ejemplo.com");
        n.setAutomatizacion(automatizacion(EventoAutomatizacion.CITA_MODIFICADA));
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA))
                .thenReturn(List.of(sesionConectada()));
        when(correoService.configurado()).thenReturn(true);

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.ENVIADA, n.getEstado());
        verify(correoService).enviar(eq("maria@ejemplo.com"), anyString(), eq("Hola {{nombre}}"));
    }

    @Test
    void elCorreoSeOmiteSiElPacienteNoTieneEmail() {
        Notificacion n = notificacion(9L, "+593998877665");
        n.setAutomatizacion(automatizacion(EventoAutomatizacion.CITA_MODIFICADA));
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA))
                .thenReturn(List.of(sesionConectada()));
        lenient().when(correoService.configurado()).thenReturn(true);

        servicio.procesar(n);

        assertEquals(EstadoNotificacion.ENVIADA, n.getEstado());
        verify(correoService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void elCorreoNoSeMandaParaLosAvisosDeSiempre() {
        Notificacion n = notificacion(10L, "+593998877665");
        n.getPaciente().setEmail("maria@ejemplo.com");
        n.setAutomatizacion(automatizacion(EventoAutomatizacion.CITA_CREADA));
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA))
                .thenReturn(List.of(sesionConectada()));
        lenient().when(correoService.configurado()).thenReturn(true);

        servicio.procesar(n);

        verify(correoService, never()).enviar(anyString(), anyString(), anyString());
    }

    private static Cita citaConPaciente() {
        Cita cita = new Cita();
        cita.setId(100L);
        Paciente p = new Paciente();
        p.setId(1L);
        p.setNombres("María");
        cita.setPaciente(p);
        return cita;
    }

    private static org.dentalcrm.domain.automatizacion.Automatizacion automatizacion(EventoAutomatizacion evento) {
        org.dentalcrm.domain.automatizacion.Automatizacion a = new org.dentalcrm.domain.automatizacion.Automatizacion();
        a.setEvento(evento);
        return a;
    }
}
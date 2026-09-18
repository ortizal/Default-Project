package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.EstadoNotificacion;
import org.dentalcrm.domain.automatizacion.Notificacion;
import org.dentalcrm.domain.automatizacion.NotificacionRepository;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.EstadoSesionWhatsapp;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.domain.whatsapp.WhatsappSesionRepository;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
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

    private NotificacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionService(notificacionRepository, automatizacionService,
                citaRepository, messagingProvider, sesionRepository);
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
}
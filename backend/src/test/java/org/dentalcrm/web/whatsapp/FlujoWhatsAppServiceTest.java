package org.dentalcrm.web.whatsapp;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlujoWhatsAppServiceTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private CitaService citaService;

    private FlujoWhatsAppService servicio;

    @BeforeEach
    void setUp() {
        servicio = new FlujoWhatsAppService(citaRepository, citaService);
    }

    private Conversacion conversacionConPaciente() {
        Paciente p = new Paciente();
        p.setId(1L);
        Conversacion c = new Conversacion();
        c.setId(10L);
        c.setPaciente(p);
        return c;
    }

    private Cita citaProxima() {
        Cita c = new Cita();
        c.setId(100L);
        c.setEstado(EstadoCita.PENDIENTE);
        c.setFecha(LocalDate.now().plusDays(1));
        return c;
    }

    private void stubProxima() {
        when(citaRepository.proximasDelPaciente(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(citaProxima()));
    }

    @Test
    void haySinPacienteQuedaIgnorada() {
        Conversacion c = new Conversacion();
        c.setId(1L);

        assertEquals(FlujoWhatsAppService.AccionFlujo.IGNORADA, servicio.procesarEntrada(c, "1"));
        verifyNoInteractions(citaRepository);
    }

    @Test
    void digitoUnoConfirma() {
        stubProxima();
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.CONFIRMADA, servicio.procesarEntrada(c, "1"));
        verify(citaService).confirmar(100L, "WHATSAPP");
    }

    @Test
    void textoConfirmarConAcentosConfirma() {
        stubProxima();
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.CONFIRMADA, servicio.procesarEntrada(c, "Sí, confirmo"));
        verify(citaService).confirmar(100L, "WHATSAPP");
    }

    @Test
    void digitoTresCancela() {
        stubProxima();
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.CANCELADA, servicio.procesarEntrada(c, "3"));
        verify(citaService).cancelar(eq(100L), any(CancelarRequest.class));
    }

    @Test
    void textoCancelarEscritoCancela() {
        stubProxima();
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.CANCELADA, servicio.procesarEntrada(c, "Quiero cancelar"));
        verify(citaService).cancelar(eq(100L), any(CancelarRequest.class));
    }

    @Test
    void mensajeIrrelevanteQuedaIgnorado() {
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.IGNORADA, servicio.procesarEntrada(c, "Hola buenas tardes"));
        verifyNoInteractions(citaService);
    }

    @Test
    void sinCitaProximaQuedaIgnorado() {
        when(citaRepository.proximasDelPaciente(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.IGNORADA, servicio.procesarEntrada(c, "1"));
        verifyNoInteractions(citaService);
    }

    @Test
    void falloDelServicioDaIgnorada() {
        stubProxima();
        doThrow(new RuntimeException("Cita no confirmable")).when(citaService).confirmar(100L, "WHATSAPP");
        Conversacion c = conversacionConPaciente();

        assertEquals(FlujoWhatsAppService.AccionFlujo.IGNORADA, servicio.procesarEntrada(c, "1"));
    }
}
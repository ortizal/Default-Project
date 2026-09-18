package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.*;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.automatizacion.dto.AutomatizacionRequest;
import org.dentalcrm.web.automatizacion.dto.AutomatizacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomatizacionServiceTest {

    @Mock
    private AutomatizacionRepository automatizacionRepository;
    @Mock
    private PlantillaMensajeRepository plantillaRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private VariableResolver variableResolver;
    @Mock
    private AuditService auditService;

    private AutomatizacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AutomatizacionService(automatizacionRepository, plantillaRepository,
                notificacionRepository, variableResolver, auditService, "America/Guayaquil");
    }

    private PlantillaMensaje plantilla(Long id) {
        PlantillaMensaje p = new PlantillaMensaje();
        p.setId(id);
        p.setNombre("recordatorio_24h");
        p.setContenido("Hola {{nombre}}");
        p.setActiva(true);
        return p;
    }

    private Automatizacion automatizacion(Long id, EventoAutomatizacion evento) {
        Automatizacion a = new Automatizacion();
        a.setId(id);
        a.setNombre("Regla " + id);
        a.setEvento(evento);
        a.setMinutosAntes(evento == EventoAutomatizacion.CITA_PROXIMA ? 1440 : 0);
        a.setPlantilla(plantilla(1L));
        a.setActiva(true);
        return a;
    }

    private Cita cita(Long id, Paciente paciente) {
        Cita c = new Cita();
        c.setId(id);
        c.setPaciente(paciente);
        c.setFecha(LocalDate.now().plusDays(1));
        c.setHoraInicio(LocalTime.of(10, 0));
        return c;
    }

    private Paciente paciente(Long id) {
        Paciente p = new Paciente();
        p.setId(id);
        p.setTelefono("+593 99 887 7665");
        return p;
    }

    @Test
    void crearAsignaEventoPlantillaYActiva() {
        when(plantillaRepository.findById(1L)).thenReturn(Optional.of(plantilla(1L)));
        when(automatizacionRepository.save(any(Automatizacion.class))).thenAnswer(i -> {
            Automatizacion a = i.getArgument(0);
            a.setId(7L);
            return a;
        });

        AutomatizacionResponse r = servicio.crear(new AutomatizacionRequest(
                "Recordatorio", "CITA_PROXIMA", 1440, 1L, null, null));

        assertEquals(7L, r.id());
        assertEquals("CITA_PROXIMA", r.evento());
        assertEquals(1440, r.minutosAntes());
        assertEquals(Boolean.TRUE, r.activa());
        verify(auditService).registrar(eq("CREAR_AUTOMATIZACION"), eq("AUTOMATIZACIONES"), eq("AUTOMATIZACION"), eq(7L));
    }

    @Test
    void crearLanzaSiPlantillaNoExiste() {
        when(plantillaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> servicio.crear(new AutomatizacionRequest(
                "R", "CITA_CREADA", 0, 99L, null, true)));
    }

    @Test
    void crearRechazaEventoInvalido() {
        BusinessException ex = assertThrows(BusinessException.class, () -> servicio.crear(new AutomatizacionRequest(
                "R", "NO_EXISTE", 0, 1L, null, true)));

        assertEquals("EVENTO_INVALIDO", ex.getCode());
    }

    @Test
    void activarYDesactivarCambiaBandera() {
        Automatizacion a = automatizacion(2L, EventoAutomatizacion.CITA_CREADA);
        a.setActiva(false);
        when(automatizacionRepository.findById(2L)).thenReturn(Optional.of(a));

        assertTrue(servicio.activar(2L).activa());
        assertFalse(servicio.desactivar(2L).activa());
    }

    @Test
    void generarCreaNotificacionesPendientesConVariables() {
        Paciente p = paciente(1L);
        Cita c = cita(100L, p);
        when(automatizacionRepository.findByActivaTrueAndEvento(EventoAutomatizacion.CITA_CREADA))
                .thenReturn(List.of(automatizacion(2L, EventoAutomatizacion.CITA_CREADA)));
        when(automatizacionRepository.findByActivaTrueAndEvento(EventoAutomatizacion.CITA_PROXIMA))
                .thenReturn(List.of(automatizacion(3L, EventoAutomatizacion.CITA_PROXIMA)));
        when(variableResolver.renderizar(anyString(), anyMap())).thenReturn("Hola Juan");

        int n = servicio.generar(c, EventoAutomatizacion.CITA_CREADA, EventoAutomatizacion.CITA_PROXIMA);

        assertEquals(2, n);
        verify(notificacionRepository, times(2)).save(any(Notificacion.class));
        verify(variableResolver).variables(c);
    }
}
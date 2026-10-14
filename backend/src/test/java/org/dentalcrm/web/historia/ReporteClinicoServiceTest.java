package org.dentalcrm.web.historia;

import org.dentalcrm.domain.archivo.ArchivoClinicoRepository;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.consentimiento.ConsentimientoRepository;
import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.*;
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.historia.dto.ReporteClinicoResponse;
import org.dentalcrm.web.historia.dto.ReporteClinicoResponse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReporteClinicoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    private ReporteClinicoService servicio;

    private PacienteRepository pacienteRepo;
    private EvolucionRepository evolucionRepo;
    private ExamenClinicoRepository examenRepo;
    private DiagnosticoClinicoRepository diagnosticoRepo;
    private OdontogramaRepository odontogramaRepo;
    private ExamenPeriodontalRepository periodonciaRepo;
    private RecetaRepository recetaRepo;
    private ConsentimientoRepository consentimientoRepo;
    private ArchivoClinicoRepository archivoRepo;
    private PlanTratamientoRepository planRepo;
    private PlanLineaRepository planLineaRepo;
    private CitaRepository citaRepo;

    @BeforeEach
    void setUp() {
        pacienteRepo = mock(PacienteRepository.class);
        evolucionRepo = mock(EvolucionRepository.class);
        examenRepo = mock(ExamenClinicoRepository.class);
        diagnosticoRepo = mock(DiagnosticoClinicoRepository.class);
        odontogramaRepo = mock(OdontogramaRepository.class);
        periodonciaRepo = mock(ExamenPeriodontalRepository.class);
        recetaRepo = mock(RecetaRepository.class);
        consentimientoRepo = mock(ConsentimientoRepository.class);
        archivoRepo = mock(ArchivoClinicoRepository.class);
        planRepo = mock(PlanTratamientoRepository.class);
        planLineaRepo = mock(PlanLineaRepository.class);
        citaRepo = mock(CitaRepository.class);

        servicio = new ReporteClinicoService(pacienteRepo, evolucionRepo, examenRepo,
                diagnosticoRepo, odontogramaRepo, periodonciaRepo, recetaRepo,
            consentimientoRepo, archivoRepo, planRepo, planLineaRepo, citaRepo, ZONA.getId());
    }

    // ------------------------------ Rango --------------------------------

    @Test
    void rangoInvertidoRespondeError() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.reporte(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1), 10));

        assertEquals("RANGO_INVALIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("La fecha de inicio no puede ser posterior a la fecha final", e.getMessage());
    }

    @Test
    void sinRangoUsaUnAnioHastaHoy() {
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        servicio.reporte(null, null, 0);

        verify(planRepo).countByFechaBetween(captor.capture(), captor.capture());
        List<LocalDate> limites = captor.getAllValues();
        LocalDate hoy = LocalDate.now(ZONA);

        assertEquals(hoy, limites.get(1));
        assertEquals(hoy.minusYears(1), limites.get(0));
    }

    @Test
    void rangoDeInstantesUsaLaZonaDelConsultorio() {
        LocalDate desde = LocalDate.of(2026, 6, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);

        servicio.reporte(desde, hasta, 10);

        verify(evolucionRepo).findByFechaBetween(
                desde.atStartOfDay(ZONA).toInstant(), hasta.plusDays(1).atStartOfDay(ZONA).toInstant());
        verify(archivoRepo).countByCreatedAtBetween(
                desde.atStartOfDay(ZONA).toInstant(), hasta.plusDays(1).atStartOfDay(ZONA).toInstant());
    }

    @Test
    void limiteSeAcotaEntre1Y50() {
        servicio.reporte(LocalDate.now(ZONA).minusDays(1), LocalDate.now(ZONA), 500);
        verify(planLineaRepo).buscarPendientes(any(), any(), eq(Pageable.ofSize(50)));

        servicio.reporte(LocalDate.now(ZONA).minusDays(1), LocalDate.now(ZONA), -3);
        verify(planLineaRepo).buscarPendientes(any(), any(), eq(Pageable.ofSize(10)));
    }

    // ------------------------------ Totales ------------------------------

    @Test
    void totalesSeCalculanDesdeLosRepositorios() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);

        when(evolucionRepo.countByFechaBetween(any(), any())).thenReturn(7L);
        when(examenRepo.countByFechaBetween(desde, hasta)).thenReturn(2L);
        when(diagnosticoRepo.countByFechaBetween(desde, hasta)).thenReturn(5L);
        when(odontogramaRepo.countByFechaBetween(desde, hasta)).thenReturn(3L);
        when(periodonciaRepo.countByFechaBetween(desde, hasta)).thenReturn(4L);
        when(recetaRepo.countByFechaBetween(desde, hasta)).thenReturn(6L);
        when(consentimientoRepo.countByFechaBetween(desde, hasta)).thenReturn(1L);
        when(archivoRepo.countByCreatedAtBetween(any(), any())).thenReturn(8L);
        when(planRepo.countByFechaBetween(desde, hasta)).thenReturn(2L);
        when(citaRepo.countByEstadoAndFechaBetween(EstadoCita.ATENDIDA, desde, hasta)).thenReturn(11L);

        ReporteClinicoResponse r = servicio.reporte(desde, hasta, 10);

        assertEquals(desde, r.desde());
        assertEquals(hasta, r.hasta());
        assertEquals(7, r.totales().evoluciones());
        assertEquals(2, r.totales().examenes());
        assertEquals(5, r.totales().diagnosticos());
        assertEquals(3, r.totales().odontogramas());
        assertEquals(4, r.totales().periodontos());
        assertEquals(6, r.totales().recetas());
        assertEquals(1, r.totales().consentimientos());
        assertEquals(8, r.totales().archivos());
        assertEquals(2, r.totales().planes());
        assertEquals(11, r.totales().citasAtendidas());
    }

    @Test
    void pacientesAtendidosCuentaPacientesUnicos() {
        Paciente ana = paciente(1L, "Ana", "Pérez");
        Paciente luis = paciente(2L, "Luis", "Gómez");

        when(evolucionRepo.findByFechaBetween(any(), any())).thenReturn(List.of(evolucion(ana), evolucion(luis)));
        when(recetaRepo.findByFechaBetween(any(), any())).thenReturn(List.of(receta(ana)));

        ReporteClinicoResponse r = servicio.reporte(LocalDate.now(ZONA).minusDays(30), LocalDate.now(ZONA), 10);

        assertEquals(2, r.totales().pacientesAtendidos());
    }

    // ------------------------------ Frecuentes ---------------------------

    @Test
    void diagnosticosFrecuentesSeMapean() {
        DiagnosticoClinicoRepository.DiagnosticoFrecuente frecuente =
                mock(DiagnosticoClinicoRepository.DiagnosticoFrecuente.class);
        when(frecuente.getNombre()).thenReturn("Caries");
        when(frecuente.getTotal()).thenReturn(4L);
        when(frecuente.getPacientes()).thenReturn(3L);
        when(diagnosticoRepo.masFrecuentes(any(), any(), any())).thenReturn(List.of(frecuente));

        ReporteClinicoResponse r = servicio.reporte(null, null, 10);

        assertEquals(1, r.diagnosticosFrecuentes().size());
        assertEquals("Caries", r.diagnosticosFrecuentes().get(0).nombre());
        assertEquals(4, r.diagnosticosFrecuentes().get(0).total());
        assertEquals(3, r.diagnosticosFrecuentes().get(0).pacientes());
    }

    // -------------------------- Pendientes / producción -------------------

    @Test
    void tratamientosPendientesSeMapeanConPaciente() {
        PlanLinea linea = lineaPlan(paciente(9L, "Marta", "Ríos"), "Empaste", 21, "OCLUSAL",
                PrioridadLinea.URGENTE, EstadoLineaPlan.PENDIENTE, new BigDecimal("40.00"));
        when(planLineaRepo.buscarPendientes(any(), any(), any())).thenReturn(List.of(linea));

        ReporteClinicoResponse r = servicio.reporte(null, null, 10);

        TratamientoPendiente p = r.tratamientosPendientes().get(0);
        assertEquals(9L, p.pacienteId());
        assertEquals("Marta Ríos", p.paciente());
        assertEquals("Empaste", p.procedimiento());
        assertEquals(21, p.diente());
        assertEquals("OCLUSAL", p.superficie());
        assertEquals("URGENTE", p.prioridad());
        assertEquals("PENDIENTE", p.estado());
        assertEquals(new BigDecimal("40.00"), p.precio());
    }

    @Test
    void produccionClinicaSeMapea() {
        PlanLineaRepository.ProduccionFila fila = mock(PlanLineaRepository.ProduccionFila.class);
        when(fila.getProcedimiento()).thenReturn("Extracción");
        when(fila.getCantidad()).thenReturn(2L);
        when(fila.getTotal()).thenReturn(new BigDecimal("90.00"));
        when(planLineaRepo.produccion(any(), any(), any())).thenReturn(List.of(fila));

        ReporteClinicoResponse r = servicio.reporte(LocalDate.now(ZONA).minusDays(1), LocalDate.now(ZONA), 10);

        ProduccionClinica produccion = r.produccionClinica().get(0);
        assertEquals("Extracción", produccion.procedimiento());
        assertEquals(2, produccion.cantidad());
        assertEquals(new BigDecimal("90.00"), produccion.total());
    }

    // ----------------------------- Continuidad ----------------------------

    @Test
    void continuidadAgrupaCitasPorEstado() {
        LocalDate desde = LocalDate.of(2026, 2, 1);
        LocalDate hasta = LocalDate.of(2026, 4, 30);

        when(citaRepo.countByEstadoAndFechaBetween(EstadoCita.ATENDIDA, desde, hasta)).thenReturn(5L);
        when(citaRepo.countByEstadoAndFechaBetween(EstadoCita.CONFIRMADA, desde, hasta)).thenReturn(3L);
        when(citaRepo.countByEstadoAndFechaBetween(EstadoCita.CANCELADA, desde, hasta)).thenReturn(1L);
        when(citaRepo.countByEstadoAndFechaBetween(EstadoCita.NO_ASISTIO, desde, hasta)).thenReturn(2L);
        when(pacienteRepo.countSinProximaCita(any(), any())).thenReturn(4L);

        Continuidad c = servicio.reporte(desde, hasta, 10).continuidad();

        assertEquals(5, c.citasAtendidas());
        assertEquals(3, c.citasConfirmadas());
        assertEquals(1, c.citasCanceladas());
        assertEquals(2, c.citasNoAsistio());
        assertEquals(4, c.pacientesSinProximaCita());
    }

    @Test
    void sinActividad90dSóloIncluyePacientesSinRegistrosRecientes() {
        Paciente activo = paciente(1L, "Ana", "Pérez");
        Paciente inactivo = paciente(2L, "Luis", "Gómez");
        when(pacienteRepo.findAll()).thenReturn(List.of(inactivo, activo));
        when(evolucionRepo.findByFechaBetween(any(), any())).thenReturn(List.of(evolucion(activo)));

        Continuidad c = servicio.reporte(LocalDate.now(ZONA).minusDays(30), LocalDate.now(ZONA), 10).continuidad();

        assertEquals(1, c.sinActividad90d().size());
        assertEquals(2L, c.sinActividad90d().get(0).pacienteId());
        assertEquals("Luis Gómez", c.sinActividad90d().get(0).paciente());
    }

    @Test
    void actividadPacienteUsaHistoriaYRecetas() {
        Paciente paciente = paciente(1L, "Ana", "Pérez");
        when(evolucionRepo.findByFechaBetween(any(), any())).thenReturn(List.of(evolucion(paciente)));
        when(examenRepo.findByFechaBetween(any(), any())).thenReturn(List.of(examen(paciente)));
        when(odontogramaRepo.findByFechaBetween(any(), any())).thenReturn(List.of(odontograma(paciente)));
        when(recetaRepo.findByFechaBetween(any(), any())).thenReturn(List.of(receta(paciente)));

        servicio.reporte(LocalDate.now(ZONA).minusDays(1), LocalDate.now(ZONA), 10);

        verify(odontogramaRepo, atLeastOnce()).findByFechaBetween(any(), any());
        verify(examenRepo, atLeastOnce()).findByFechaBetween(any(), any());
        verify(recetaRepo, atLeastOnce()).findByFechaBetween(any(), any());
    }

    // ------------------------------ Base ---------------------------------

    private static Paciente paciente(Long id, String nombres, String apellidos) {
        Paciente p = new Paciente();
        p.setId(id);
        p.setNombres(nombres);
        p.setApellidos(apellidos);
        return p;
    }

    private static HistoriaClinica historia(Paciente paciente) {
        HistoriaClinica h = new HistoriaClinica();
        h.setPaciente(paciente);
        return h;
    }

    private static Evolucion evolucion(Paciente paciente) {
        Evolucion e = new Evolucion();
        e.setHistoria(historia(paciente));
        e.setFecha(Instant.now().minusSeconds(3600));
        return e;
    }

    private static ExamenClinico examen(Paciente paciente) {
        ExamenClinico x = new ExamenClinico();
        x.setHistoria(historia(paciente));
        x.setFecha(LocalDate.now(ZONA).minusDays(1));
        return x;
    }

    private static Odontograma odontograma(Paciente paciente) {
        Odontograma o = new Odontograma();
        o.setHistoria(historia(paciente));
        o.setFecha(LocalDate.now(ZONA).minusDays(1));
        return o;
    }

    private static Receta receta(Paciente paciente) {
        Receta r = new Receta();
        r.setPaciente(paciente);
        r.setFecha(LocalDate.now(ZONA).minusDays(1));
        return r;
    }

    private static PlanLinea lineaPlan(Paciente paciente, String procedimiento, Integer diente,
                                       String superficie, PrioridadLinea prioridad,
                                       EstadoLineaPlan estado, BigDecimal subtotal) {
        PlanTratamiento plan = new PlanTratamiento();
        plan.setPaciente(paciente);
        plan.setTitulo("Tratamiento general");
        plan.setFecha(LocalDate.now(ZONA).minusDays(10));
        plan.setEstado(EstadoPlanTratamiento.PARCIALMENTE_ACEPTADO);

        PlanLinea linea = new PlanLinea();
        linea.setPlan(plan);
        linea.setProcedimiento(procedimiento);
        linea.setDiente(diente);
        linea.setSuperficie(superficie);
        linea.setPrioridad(prioridad);
        linea.setEstado(estado);
        linea.setSubtotal(subtotal);
        return linea;
    }
}

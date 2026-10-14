package org.dentalcrm.web.historia;

import org.dentalcrm.domain.archivo.ArchivoClinico;
import org.dentalcrm.domain.archivo.ArchivoClinicoRepository;
import org.dentalcrm.domain.archivo.CategoriaArchivo;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.consentimiento.Consentimiento;
import org.dentalcrm.domain.consentimiento.ConsentimientoRepository;
import org.dentalcrm.domain.consentimiento.EstadoConsentimiento;
import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.EstadoLineaPlan;
import org.dentalcrm.domain.plan.EstadoPlanTratamiento;
import org.dentalcrm.domain.plan.PlanLinea;
import org.dentalcrm.domain.plan.PlanTratamiento;
import org.dentalcrm.domain.plan.PlanTratamientoRepository;
import org.dentalcrm.domain.receta.EstadoReceta;
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaLinea;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.historia.dto.TimelineEventoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimelineServiceTest {

    private TimelineService servicio;

    private PacienteRepository pacienteRepo;
    private HistoriaClinicaRepository historiaRepo;
    private EvolucionRepository evolucionRepo;
    private DiagnosticoClinicoRepository diagnosticoRepo;
    private ExamenClinicoRepository examenRepo;
    private OdontogramaRepository odontogramaRepo;
    private ExamenPeriodontalRepository periodonciaRepo;
    private CitaRepository citaRepo;
    private PlanTratamientoRepository planRepo;
    private RecetaRepository recetaRepo;
    private ArchivoClinicoRepository archivoRepo;
    private ConsentimientoRepository consentimientoRepo;

    @BeforeEach
    void setUp() {
        pacienteRepo = mock(PacienteRepository.class);
        historiaRepo = mock(HistoriaClinicaRepository.class);
        evolucionRepo = mock(EvolucionRepository.class);
        diagnosticoRepo = mock(DiagnosticoClinicoRepository.class);
        examenRepo = mock(ExamenClinicoRepository.class);
        odontogramaRepo = mock(OdontogramaRepository.class);
        periodonciaRepo = mock(ExamenPeriodontalRepository.class);
        citaRepo = mock(CitaRepository.class);
        planRepo = mock(PlanTratamientoRepository.class);
        recetaRepo = mock(RecetaRepository.class);
        archivoRepo = mock(ArchivoClinicoRepository.class);
        consentimientoRepo = mock(ConsentimientoRepository.class);

        servicio = new TimelineService(pacienteRepo, historiaRepo, evolucionRepo, diagnosticoRepo,
                examenRepo, odontogramaRepo, periodonciaRepo, citaRepo, planRepo, recetaRepo,
                archivoRepo, consentimientoRepo);
    }

    // ------------------------------ Base --------------------------------

    @Test
    void pacienteInexistenteResponde404() {
        when(pacienteRepo.findById(9L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.timeline(9L));

        assertEquals("NO_ENCONTRADO", e.getCode());
        assertEquals("Paciente no encontrado", e.getMessage());
    }

    @Test
    void sinHistoriaTodaviaMuestraEventosDelPaciente() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());
        when(recetaRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(receta(11L)));

        List<TimelineEventoResponse> eventos = servicio.timeline(1L);

        assertEquals(1, eventos.size());
        assertEquals("RECETA", eventos.get(0).tipo());
        assertTrue(eventos.get(0).titulo().contains("Caries"));
    }

    @Test
    void sinHistoriaNiEventosDevuelveVacia() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());

        assertTrue(servicio.timeline(1L).isEmpty());
    }

    // ------------------------------ Fuentes -----------------------------

    @Test
    void timelineAgregaTodasLasFuentes() {
        Paciente paciente = paciente();
        HistoriaClinica historia = historia(paciente);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of(evolucion(1L)));
        when(diagnosticoRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of(diagnostico(2L)));
        when(examenRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of(examen(3L)));
        when(odontogramaRepo.findByHistoriaIdOrderByVersionDesc(5L)).thenReturn(List.of(odontograma(4L)));
        when(periodonciaRepo.findByHistoriaIdOrderByVersionDesc(5L)).thenReturn(List.of(periodoncia(6L)));
        when(citaRepo.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(1L)).thenReturn(List.of(citaAtendida(7L)));
        when(planRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(plan(8L)));
        when(recetaRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(receta(9L)));
        when(archivoRepo.findByPacienteIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(archivo(10L)));
        when(consentimientoRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(consentimiento(12L)));

        Set<String> tipos = servicio.timeline(1L).stream()
                .map(TimelineEventoResponse::tipo)
                .collect(Collectors.toSet());

        assertEquals(Set.of("EVOLUCION", "DIAGNOSTICO", "EXAMEN", "ODONTOGRAMA", "PERIODONCIA",
                "CITA", "PLAN", "PROCEDIMIENTO", "RECETA", "ARCHIVO", "CONSENTIMIENTO"), tipos);
    }

    @Test
    void timelineOrdenaPorFechaDescendente() {
        Paciente paciente = paciente();
        HistoriaClinica historia = historia(paciente);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));

        DiagnosticoClinico diagnostico = diagnostico(2L);
        diagnostico.setFecha(LocalDate.now());
        when(diagnosticoRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of(diagnostico));
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of(evolucion(1L)));

        Receta receta = receta(9L);
        receta.setFecha(LocalDate.now().minusDays(5));
        when(recetaRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(receta));

        List<TimelineEventoResponse> eventos = servicio.timeline(1L);

        assertEquals(3, eventos.size());
        assertEquals("EVOLUCION", eventos.get(0).tipo());
        assertEquals("DIAGNOSTICO", eventos.get(1).tipo());
        assertEquals("RECETA", eventos.get(2).tipo());
        assertTrue(eventos.get(0).fecha().isAfter(eventos.get(1).fecha()));
        assertTrue(eventos.get(1).fecha().isAfter(eventos.get(2).fecha()));
    }

    @Test
    void procedimientoSóloSaleDeLineasEnEjecucionOCompletadas() {
        Paciente paciente = paciente();
        HistoriaClinica historia = historia(paciente);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));

        PlanTratamiento plan = new PlanTratamiento();
        plan.setId(8L);
        plan.setTitulo("Rehabilitación");
        plan.setFecha(LocalDate.now().minusDays(2));
        plan.setEstado(EstadoPlanTratamiento.EN_EJECUCION);
        plan.setTotal(new BigDecimal("120.00"));
        plan.setLineas(List.of(linea(1L, EstadoLineaPlan.COMPLETADO),
                linea(2L, EstadoLineaPlan.EN_EJECUCION),
                linea(3L, EstadoLineaPlan.PENDIENTE),
                linea(4L, EstadoLineaPlan.CANCELADO)));
        when(planRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(plan));

        List<TimelineEventoResponse> eventos = servicio.timeline(1L);

        assertEquals(3, eventos.size());
        assertEquals(Set.of("PLAN", "PROCEDIMIENTO"),
                eventos.stream().map(TimelineEventoResponse::tipo).collect(Collectors.toSet()));
        List<TimelineEventoResponse> procedimientos = eventos.stream()
                .filter(e -> e.tipo().equals("PROCEDIMIENTO")).toList();
        assertEquals(2, procedimientos.size());
        assertTrue(procedimientos.stream().anyMatch(e -> e.referenciaId() == 1L));
        assertTrue(procedimientos.stream().anyMatch(e -> e.detalle().contains("Rehabilitación")));
        assertFalse(procedimientos.stream().anyMatch(e -> e.referenciaId() == 3L));
    }

    @Test
    void citaNoAtendidaNoEntraEnLaLinea() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());
        Cita cita = citaAtendida(7L);
        cita.setEstado(EstadoCita.CONFIRMADA);
        when(citaRepo.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(1L)).thenReturn(List.of(cita));

        assertTrue(servicio.timeline(1L).isEmpty());
    }

    // ------------------------------ Límites -----------------------------

    @Test
    void cadaFuenteSeLimitaASusPrimerosEventos() {
        Paciente paciente = paciente();
        HistoriaClinica historia = historia(paciente);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));

        List<Evolucion> evoluciones = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            evoluciones.add(evolucion((long) i));
        }
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(evoluciones);

        assertEquals(25, servicio.timeline(1L).size());
    }

    @Test
    void elTotalNuncaSuperaCienEventos() {
        Paciente paciente = paciente();
        HistoriaClinica historia = historia(paciente);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(evoluciones(30));
        when(diagnosticoRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(diagnosticos(30));
        when(examenRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(examenes(30));
        when(odontogramaRepo.findByHistoriaIdOrderByVersionDesc(5L)).thenReturn(odontogramas(30));
        when(periodonciaRepo.findByHistoriaIdOrderByVersionDesc(5L)).thenReturn(periodontales(30));

        assertEquals(100, servicio.timeline(1L).size());
    }

    // ------------------------------ Datos -------------------------------

    private static Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombres("Lucía");
        paciente.setApellidos("Martínez");
        return paciente;
    }

    private static HistoriaClinica historia(Paciente paciente) {
        HistoriaClinica historia = new HistoriaClinica();
        historia.setId(5L);
        historia.setPaciente(paciente);
        historia.setEstado(EstadoHistoria.ABIERTA);
        return historia;
    }

    private static Evolucion evolucion(Long id) {
        Evolucion evolucion = new Evolucion();
        evolucion.setId(id);
        evolucion.setFecha(Instant.now());
        evolucion.setMotivo("Control postoperatorio");
        evolucion.setHallazgos("Sin sangrado a la sondaje");
        return evolucion;
    }

    private static DiagnosticoClinico diagnostico(Long id) {
        DiagnosticoClinico diagnostico = new DiagnosticoClinico();
        diagnostico.setId(id);
        diagnostico.setNombre("Gingivitis");
        diagnostico.setFecha(LocalDate.now());
        diagnostico.setSeveridad(SeveridadDiagnostico.LEVE);
        diagnostico.setEstado(EstadoDiagnostico.CONFIRMADO);
        return diagnostico;
    }

    private static ExamenClinico examen(Long id) {
        ExamenClinico examen = new ExamenClinico();
        examen.setId(id);
        examen.setFecha(LocalDate.now());
        examen.setObservaciones("Oclusión funcional");
        return examen;
    }

    private static Odontograma odontograma(Long id) {
        Odontograma odontograma = new Odontograma();
        odontograma.setId(id);
        odontograma.setFecha(LocalDate.now().minusDays(1));
        odontograma.setVersion(3);
        odontograma.setDenticion(Denticion.PERMANENTE);
        return odontograma;
    }

    private static ExamenPeriodontal periodoncia(Long id) {
        ExamenPeriodontal examen = new ExamenPeriodontal();
        examen.setId(id);
        examen.setFecha(LocalDate.now().minusDays(3));
        examen.setVersion(2);
        return examen;
    }

    private static Cita citaAtendida(Long id) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setPaciente(paciente());
        cita.setFecha(LocalDate.now().minusDays(4));
        cita.setEstado(EstadoCita.ATENDIDA);
        return cita;
    }

    private static PlanTratamiento plan(Long id) {
        PlanTratamiento plan = new PlanTratamiento();
        plan.setId(id);
        plan.setTitulo("Estética dental");
        plan.setFecha(LocalDate.now().minusDays(6));
        plan.setEstado(EstadoPlanTratamiento.ACEPTADO);
        plan.setTotal(new BigDecimal("350.00"));
        plan.setLineas(List.of(linea(1L, EstadoLineaPlan.COMPLETADO)));
        return plan;
    }

    private static PlanLinea linea(Long id, EstadoLineaPlan estado) {
        PlanLinea linea = new PlanLinea();
        linea.setId(id);
        linea.setProcedimiento("Empaste de composite");
        linea.setDiente(16);
        linea.setSuperficie("OCLUSAL");
        linea.setEstado(estado);
        return linea;
    }

    private static Receta receta(Long id) {
        Receta receta = new Receta();
        receta.setId(id);
        receta.setFecha(LocalDate.now().minusDays(7));
        receta.setDiagnostico("Caries oclusal pieza 16");
        receta.setEstado(EstadoReceta.VIGENTE);
        RecetaLinea linea = new RecetaLinea();
        linea.setId(1L);
        linea.setMedicamento("Ibuprofeno 400mg");
        receta.setLineas(List.of(linea));
        return receta;
    }

    private static ArchivoClinico archivo(Long id) {
        ArchivoClinico archivo = new ArchivoClinico();
        archivo.setId(id);
        archivo.setNombre("rx-molar.png");
        archivo.setCategoria(CategoriaArchivo.RADIOGRAFIA);
        archivo.setTamano(2048L);
        archivo.setFecha(LocalDate.now().minusDays(2));
        archivo.setCreatedAt(Instant.now());
        return archivo;
    }

    private static Consentimiento consentimiento(Long id) {
        Consentimiento consentimiento = new Consentimiento();
        consentimiento.setId(id);
        consentimiento.setTitulo("Consentimiento de ortodoncia");
        consentimiento.setFecha(LocalDate.now().minusDays(8));
        consentimiento.setEstado(EstadoConsentimiento.FIRMADO);
        consentimiento.setFirmante("Lucía Martínez");
        return consentimiento;
    }

    private static List<Evolucion> evoluciones(int total) {
        List<Evolucion> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            lista.add(evolucion((long) i));
        }
        return lista;
    }

    private static List<DiagnosticoClinico> diagnosticos(int total) {
        List<DiagnosticoClinico> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            lista.add(diagnostico((long) i));
        }
        return lista;
    }

    private static List<ExamenClinico> examenes(int total) {
        List<ExamenClinico> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            lista.add(examen((long) i));
        }
        return lista;
    }

    private static List<Odontograma> odontogramas(int total) {
        List<Odontograma> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Odontograma odontograma = odontograma((long) i);
            odontograma.setVersion(i);
            lista.add(odontograma);
        }
        return lista;
    }

    private static List<ExamenPeriodontal> periodontales(int total) {
        List<ExamenPeriodontal> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            ExamenPeriodontal examen = periodoncia((long) i);
            examen.setVersion(i);
            lista.add(examen);
        }
        return lista;
    }
}

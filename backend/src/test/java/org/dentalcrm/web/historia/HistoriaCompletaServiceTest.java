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
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaLinea;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse.Seccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistoriaCompletaServiceTest {

    private HistoriaCompletaService servicio;

    private PacienteRepository pacienteRepo;
    private HistoriaClinicaRepository historiaRepo;
    private AntecedenteMedicoRepository antecedenteRepo;
    private ExamenClinicoRepository examenRepo;
    private DiagnosticoClinicoRepository diagnosticoRepo;
    private EvolucionRepository evolucionRepo;
    private EvolucionAdendaRepository adendaRepo;
    private OdontogramaRepository odontogramaRepo;
    private OdontogramaDienteRepository dienteRepo;
    private ExamenPeriodontalRepository periodonciaRepo;
    private PlanTratamientoRepository planRepo;
    private RecetaRepository recetaRepo;
    private ConsentimientoRepository consentimientoRepo;
    private ArchivoClinicoRepository archivoRepo;
    private CitaRepository citaRepo;

    @BeforeEach
    void setUp() {
        pacienteRepo = mock(PacienteRepository.class);
        historiaRepo = mock(HistoriaClinicaRepository.class);
        antecedenteRepo = mock(AntecedenteMedicoRepository.class);
        examenRepo = mock(ExamenClinicoRepository.class);
        diagnosticoRepo = mock(DiagnosticoClinicoRepository.class);
        evolucionRepo = mock(EvolucionRepository.class);
        adendaRepo = mock(EvolucionAdendaRepository.class);
        odontogramaRepo = mock(OdontogramaRepository.class);
        dienteRepo = mock(OdontogramaDienteRepository.class);
        periodonciaRepo = mock(ExamenPeriodontalRepository.class);
        planRepo = mock(PlanTratamientoRepository.class);
        recetaRepo = mock(RecetaRepository.class);
        consentimientoRepo = mock(ConsentimientoRepository.class);
        archivoRepo = mock(ArchivoClinicoRepository.class);
        citaRepo = mock(CitaRepository.class);

        servicio = new HistoriaCompletaService(pacienteRepo, historiaRepo, antecedenteRepo,
                examenRepo, diagnosticoRepo, evolucionRepo, adendaRepo, odontogramaRepo,
                dienteRepo, periodonciaRepo, planRepo, recetaRepo, consentimientoRepo,
                archivoRepo, citaRepo);
    }

    // ------------------------------ Base ---------------------------------

    @Test
    void pacienteInexistenteResponde404() {
        when(pacienteRepo.findById(99L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.construir(99L));

        assertEquals("NO_ENCONTRADO", e.getCode());
        assertEquals("Paciente no encontrado", e.getMessage());
    }

    @Test
    void sinHistoriaClinicaLaCabeceraLoIndica() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));

        HistoriaCompletaResponse r = servicio.construir(1L);

        assertEquals("Sin historia clínica", r.paciente().historia());
        assertEquals("Historia clínica: sin crear", r.resumen().get(0));
        assertEquals("—", r.paciente().odontologo());
        assertEquals(11, r.secciones().size());
        assertTrue(r.secciones().stream().allMatch(s -> s.filas().isEmpty()));
    }

    @Test
    void seccionesIncluyenRegistrosDeCadaFuente() {
        HistoriaClinica historia = historia();
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));
        when(antecedenteRepo.findByHistoriaIdOrderByTipoAscNombreAsc(1L))
                .thenReturn(List.of(antecedente(TipoAntecedente.ALERGIA, "Penicilina")));
        when(examenRepo.findByHistoriaIdOrderByFechaDesc(1L))
                .thenReturn(List.of(examen()));
        when(diagnosticoRepo.findByHistoriaIdOrderByFechaDesc(1L))
                .thenReturn(List.of(diagnostico()));
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(1L))
                .thenReturn(List.of(evolucion()));
        when(planRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(plan()));
        when(recetaRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(receta()));
        when(consentimientoRepo.findByPacienteIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(consentimiento()));
        when(archivoRepo.findByPacienteIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(archivo()));
        when(citaRepo.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(1L))
                .thenReturn(List.of(cita(EstadoCita.ATENDIDA), cita(EstadoCita.CANCELADA)));

        HistoriaCompletaResponse r = servicio.construir(1L);

        assertEquals("Antecedentes", seccion(r, "Antecedentes").titulo());
        assertEquals(1, seccion(r, "Antecedentes").filas().size());
        assertEquals(1, seccion(r, "Exámenes clínicos").filas().size());
        assertEquals(1, seccion(r, "Diagnósticos").filas().size());
        assertEquals(1, seccion(r, "Evoluciones").filas().size());
        assertEquals(1, seccion(r, "Planes de tratamiento").filas().size());
        assertEquals(1, seccion(r, "Recetas").filas().size());
        assertEquals(1, seccion(r, "Consentimientos").filas().size());
        assertEquals(1, seccion(r, "Imágenes y archivos").filas().size());
        assertEquals("Citas atendidas (últimas 10)", seccion(r, "Citas atendidas (últimas 10)").titulo());
        assertEquals(1, seccion(r, "Citas atendidas (últimas 10)").filas().size());
        assertEquals("Historia clínica: ABIERTA", r.resumen().get(0));
    }

    @Test
    void evolucionesIncluyenLasAdendas() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia()));
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(1L)).thenReturn(List.of(evolucion()));
        when(adendaRepo.findByEvolucionIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(adenda("Ampliación con radiografía")));

        Seccion seccion = seccion(servicio.construir(1L), "Evoluciones");

        assertEquals(2, seccion.filas().size());
        assertTrue(seccion.filas().get(1).celdas().get(1).contains("Adenda"));
        assertTrue(seccion.filas().get(1).celdas().get(2).contains("Ampliación con radiografía"));
    }

    @Test
    void odontogramaVigenteEsElDeMayorVersion() {
        Odontograma vigente = new Odontograma();
        vigente.setId(3L);
        vigente.setVersion(3);
        vigente.setFecha(LocalDate.of(2026, 3, 1));

        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia()));
        when(odontogramaRepo.findFirstByHistoriaIdOrderByVersionDesc(1L))
                .thenReturn(Optional.of(vigente));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(3L)).thenReturn(List.of(diente(21)));

        Seccion seccion = seccion(servicio.construir(1L), "Odontograma vigente (v3 · 01/03/2026)");

        assertEquals(1, seccion.filas().size());
        assertEquals("21", seccion.filas().get(0).celdas().get(0));
    }

    @Test
    void recetasMuestranMedicamentosDeCadaLinea() {
        Receta receta = receta();
        RecetaLinea linea = new RecetaLinea();
        linea.setMedicamento("Amoxicilina");
        linea.setConcentracion("500 mg");
        linea.setDosis("1 cada 8 horas");
        receta.setLineas(List.of(linea));

        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia()));
        when(recetaRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(receta));

        Seccion seccion = seccion(servicio.construir(1L), "Recetas");

        String medicamentos = seccion.filas().get(0).celdas().get(2);
        assertTrue(medicamentos.contains("Amoxicilina"));
        assertTrue(medicamentos.contains("500 mg"));
        assertTrue(medicamentos.contains("1 cada 8 horas"));
    }

    @Test
    void consentimientosFirmadosMuestranFirmante() {
        Consentimiento firmado = consentimiento();
        firmado.setEstado(EstadoConsentimiento.FIRMADO);
        firmado.setFirmante("Pedro Pérez");
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia()));
        when(consentimientoRepo.findByPacienteIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(firmado));

        Seccion seccion = seccion(servicio.construir(1L), "Consentimientos");

        assertEquals("Pedro Pérez", seccion.filas().get(0).celdas().get(3));
    }

    // ------------------------------ Base ---------------------------------

    private static Seccion seccion(HistoriaCompletaResponse r, String titulo) {
        return r.secciones().stream()
                .filter(s -> s.titulo().equals(titulo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta la sección: " + titulo));
    }

    private static Paciente paciente() {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setNombres("Ana");
        p.setApellidos("Pérez");
        p.setCedula("1234567890");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        return p;
    }

    private static HistoriaClinica historia() {
        HistoriaClinica h = new HistoriaClinica();
        h.setId(1L);
        h.setPaciente(paciente());
        h.setEstado(EstadoHistoria.ABIERTA);
        return h;
    }

    private static AntecedenteMedico antecedente(TipoAntecedente tipo, String nombre) {
        AntecedenteMedico a = new AntecedenteMedico();
        a.setTipo(tipo);
        a.setNombre(nombre);
        a.setEstado(EstadoAntecedente.ACTIVO);
        a.setFechaDesde(LocalDate.of(2020, 1, 1));
        return a;
    }

    private static ExamenClinico examen() {
        ExamenClinico x = new ExamenClinico();
        x.setFecha(LocalDate.of(2026, 1, 15));
        x.setGeneral("Sin hallazgos");
        return x;
    }

    private static DiagnosticoClinico diagnostico() {
        DiagnosticoClinico d = new DiagnosticoClinico();
        d.setNombre("Caries");
        d.setRegion("21");
        d.setSeveridad(SeveridadDiagnostico.LEVE);
        d.setEstado(EstadoDiagnostico.CONFIRMADO);
        d.setFecha(LocalDate.of(2026, 1, 15));
        return d;
    }

    private static Evolucion evolucion() {
        Evolucion e = new Evolucion();
        e.setId(7L);
        e.setFecha(Instant.parse("2026-01-15T10:00:00Z"));
        e.setMotivo("Dolor en molar inferior");
        e.setEstado(EstadoEvolucion.CERRADA);
        return e;
    }

    private static EvolucionAdenda adenda(String texto) {
        EvolucionAdenda a = new EvolucionAdenda();
        a.setTexto(texto);
        a.setCreatedAt(Instant.parse("2026-01-16T10:00:00Z"));
        return a;
    }

    private static OdontogramaDiente diente(int numero) {
        OdontogramaDiente d = new OdontogramaDiente();
        d.setDiente(numero);
        d.setEstado(EstadoOdontograma.EXISTENTE);
        d.setCondicion("Sano");
        return d;
    }

    private static PlanTratamiento plan() {
        PlanTratamiento plan = new PlanTratamiento();
        plan.setTitulo("Rehabilitación");
        plan.setFecha(LocalDate.of(2026, 2, 1));
        plan.setEstado(EstadoPlanTratamiento.ACEPTADO);
        plan.setTotal(new BigDecimal("150.00"));
        PlanLinea linea = new PlanLinea();
        linea.setProcedimiento("Empaste");
        linea.setEstado(EstadoLineaPlan.PENDIENTE);
        plan.setLineas(List.of(linea));
        return plan;
    }

    private static Receta receta() {
        Receta r = new Receta();
        r.setFecha(LocalDate.of(2026, 2, 5));
        r.setDiagnostico("Caries");
        r.setLineas(new java.util.ArrayList<>());
        return r;
    }

    private static Consentimiento consentimiento() {
        Consentimiento c = new Consentimiento();
        c.setFecha(LocalDate.of(2026, 2, 10));
        c.setTitulo("Consentimiento informado de tratamiento");
        c.setEstado(EstadoConsentimiento.BORRADOR);
        return c;
    }

    private static ArchivoClinico archivo() {
        ArchivoClinico a = new ArchivoClinico();
        a.setFecha(LocalDate.of(2026, 3, 1));
        a.setCategoria(CategoriaArchivo.RADIOGRAFIA);
        a.setNombre("periapical-21.jpg");
        a.setTamano(2048L);
        return a;
    }

    private static Cita cita(EstadoCita estado) {
        Cita c = new Cita();
        c.setFecha(LocalDate.of(2026, 3, 2));
        c.setEstado(estado);
        Servicio servicio = new Servicio();
        servicio.setNombre("Limpieza");
        c.setServicio(servicio);
        return c;
    }
}

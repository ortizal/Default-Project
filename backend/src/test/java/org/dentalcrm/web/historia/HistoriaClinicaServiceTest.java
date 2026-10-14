package org.dentalcrm.web.historia;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.historia.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HistoriaClinicaServiceTest {

    private HistoriaClinicaService servicio;

    private HistoriaClinicaRepository historiaRepo;
    private AntecedenteMedicoRepository antecedenteRepo;
    private ExamenClinicoRepository examenRepo;
    private DiagnosticoClinicoRepository diagnosticoRepo;
    private EvolucionRepository evolucionRepo;
    private EvolucionAdendaRepository adendaRepo;
    private PacienteRepository pacienteRepo;
    private OdontologoRepository odontologoRepo;
    private CitaRepository citaRepo;
    private CurrentUserService usuarioActual;

    @BeforeEach
    void setUp() {
        historiaRepo = mock(HistoriaClinicaRepository.class);
        antecedenteRepo = mock(AntecedenteMedicoRepository.class);
        examenRepo = mock(ExamenClinicoRepository.class);
        diagnosticoRepo = mock(DiagnosticoClinicoRepository.class);
        evolucionRepo = mock(EvolucionRepository.class);
        adendaRepo = mock(EvolucionAdendaRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        odontologoRepo = mock(OdontologoRepository.class);
        citaRepo = mock(CitaRepository.class);
        usuarioActual = mock(CurrentUserService.class);

        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(77L));

        servicio = new HistoriaClinicaService(historiaRepo, antecedenteRepo, examenRepo,
                diagnosticoRepo, evolucionRepo, adendaRepo, pacienteRepo, odontologoRepo,
                citaRepo, mock(AuditService.class), usuarioActual);
    }

    // ------------------------------ Resumen -----------------------------

    @Test
    void resumenSinHistoriaDevuelveVaciosSinCrearNada() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente(1L)));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());

        ResumenResponse r = servicio.resumen(1L);

        assertNull(r.historiaId());
        assertEquals(EstadoHistoria.ABIERTA, r.estadoHistoria());
        assertEquals(0, r.totalAntecedentes());
        assertEquals(0, r.totalEvoluciones());
        assertNull(r.ultimaEvolucion());
        verify(historiaRepo, never()).save(any(HistoriaClinica.class));
    }

    @Test
    void resumenTraeAlerergiasMedicacionYAlertas() {
        Paciente paciente = paciente(1L);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));

        HistoriaClinica historia = historia(5L, paciente);
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));

        AntecedenteMedico alergia = antecedente(historia, TipoAntecedente.ALERGIA, "Penicilina");
        AntecedenteMedico medicacion = antecedente(historia, TipoAntecedente.MEDICACION, "Ibuprofeno 400mg");
        when(antecedenteRepo.findByHistoriaIdOrderByTipoAscNombreAsc(5L)).thenReturn(List.of(alergia, medicacion));
        when(diagnosticoRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of());
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of());
        when(citaRepo.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(1L)).thenReturn(List.of());
        when(citaRepo.proximasDelPaciente(eq(1L), any(LocalDate.class))).thenReturn(List.of());

        ResumenResponse r = servicio.resumen(1L);

        assertEquals(5L, r.historiaId());
        assertEquals(List.of("Penicilina"), r.alergias());
        assertEquals(List.of("Ibuprofeno 400mg"), r.medicacion());
        assertEquals(2, r.totalAntecedentes());
        assertTrue(r.alertas().stream().anyMatch(a -> a.startsWith("Alergias:")));
        assertFalse(r.paciente().menorEdad());
    }

    @Test
    void resumenMarcaPacienteMenorDeEdad() {
        Paciente paciente = paciente(1L);
        paciente.setFechaNacimiento(LocalDate.now().minusYears(10));
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());

        ResumenResponse r = servicio.resumen(1L);

        assertTrue(r.paciente().menorEdad());
        assertEquals(10, r.paciente().edad());
        assertTrue(r.alertas().contains("Paciente menor de edad"));
    }

    // ----------------------------- Antecedentes -------------------------

    @Test
    void crearAntecedenteCreaLaHistoriaSiNoExiste() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente(1L)));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());
        when(historiaRepo.save(any(HistoriaClinica.class))).thenAnswer(inv -> {
            HistoriaClinica h = inv.getArgument(0);
            h.setId(9L);
            return h;
        });
        when(antecedenteRepo.save(any(AntecedenteMedico.class))).thenAnswer(inv -> {
            AntecedenteMedico a = inv.getArgument(0);
            a.setId(3L);
            return a;
        });

        AntecedenteResponse r = servicio.crearAntecedente(1L,
                new AntecedenteRequest(TipoAntecedente.ALERGIA, "Látex", null, null, null, null));

        assertEquals(3L, r.id());
        assertEquals(EstadoAntecedente.ACTIVO, r.estado());
        verify(historiaRepo).save(any(HistoriaClinica.class));
        verify(antecedenteRepo).save(any(AntecedenteMedico.class));
    }

    @Test
    void antecedenteRechazaPacienteInexistente() {
        when(pacienteRepo.findById(99L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.crearAntecedente(99L,
                        new AntecedenteRequest(TipoAntecedente.ENFERMEDAD, "Asma", null, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        verifyNoInteractions(antecedenteRepo);
    }

    @Test
    void eliminarAntecedenteSeEliminaYSeAudita() {
        AntecedenteMedico existente = antecedente(historia(5L, paciente(1L)), TipoAntecedente.HABITO, "Fumador");
        existente.setId(12L);
        when(antecedenteRepo.findById(12L)).thenReturn(Optional.of(existente));

        servicio.eliminarAntecedente(12L);

        verify(antecedenteRepo).delete(existente);
    }

    // ------------------------------ Diagnósticos ------------------------

    @Test
    void diagnosticoInexistenteDevuelve404() {
        when(diagnosticoRepo.findById(404L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.actualizarDiagnostico(404L, new DiagnosticoRequest("Caries", null, null, null, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void diagnosticoConservaHistorialAlCambiarEstado() {
        HistoriaClinica historia = historia(5L, paciente(1L));
        DiagnosticoClinico d = new DiagnosticoClinico();
        d.setId(8L);
        d.setHistoria(historia);
        d.setNombre("Pulpitis");
        d.setSeveridad(SeveridadDiagnostico.SEVERA);
        d.setEstado(EstadoDiagnostico.SOSPECHA);
        d.setFecha(LocalDate.now());
        when(diagnosticoRepo.findById(8L)).thenReturn(Optional.of(d));
        when(diagnosticoRepo.save(any(DiagnosticoClinico.class))).thenAnswer(inv -> inv.getArgument(0));

        DiagnosticoResponse r = servicio.actualizarDiagnostico(8L, new DiagnosticoRequest(
                "Pulpitis", "FDI 26", SeveridadDiagnostico.SEVERA, EstadoDiagnostico.RESUELTO,
                d.getFecha(), null, "Tratamiento realizado"));

        assertEquals(EstadoDiagnostico.RESUELTO, r.estado());
        assertEquals("Pulpitis", r.nombre());
    }

    // ------------------------------ Evoluciones -------------------------

    @Test
    void evolucionCerradaNoAdmiteEdicion() {
        Evolucion evolucion = evolucionCerrada();
        when(evolucionRepo.findById(4L)).thenReturn(Optional.of(evolucion));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.actualizarEvolucion(4L, new EvolucionRequest(null, null, null, "Motivo",
                        null, null, null, null, null, null, null, null, null, null, null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(evolucionRepo, never()).save(any(Evolucion.class));
    }

    @Test
    void cerrarEvolucionLaVuelveInmutable() {
        Evolucion evolucion = evolucion(4L, EstadoEvolucion.ABIERTA);
        when(evolucionRepo.findById(4L)).thenReturn(Optional.of(evolucion));
        when(evolucionRepo.save(any(Evolucion.class))).thenAnswer(inv -> inv.getArgument(0));

        EvolucionResponse r = servicio.cerrarEvolucion(4L);

        assertEquals(EstadoEvolucion.CERRADA, r.estado());
        assertNotNull(r.cerradaEn());
        assertEquals(77L, r.cerradaPor());

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.cerrarEvolucion(4L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void adendaSobreEvolucionAbiertaSeRechaza() {
        when(evolucionRepo.findById(4L)).thenReturn(Optional.of(evolucion(4L, EstadoEvolucion.ABIERTA)));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.crearAdenda(4L, new AdendaRequest("Corrección")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verifyNoInteractions(adendaRepo);
    }

    @Test
    void adendaSeRegistraEnEvolucionCerrada() {
        Evolucion evolucion = evolucion(4L, EstadoEvolucion.CERRADA);
        when(evolucionRepo.findById(4L)).thenReturn(Optional.of(evolucion));
        when(adendaRepo.save(any(EvolucionAdenda.class))).thenAnswer(inv -> {
            EvolucionAdenda a = inv.getArgument(0);
            a.setId(15L);
            return a;
        });

        AdendaResponse r = servicio.crearAdenda(4L, new AdendaRequest("  Corregido el diente 36  "));

        assertEquals(15L, r.id());
        assertEquals("Corregido el diente 36", r.texto());
        assertEquals(77L, r.autorId());
    }

    @Test
    void listarEvolucionesIncluyeAdendasDeLasCerradas() {
        Paciente paciente = paciente(1L);
        HistoriaClinica historia = historia(5L, paciente);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));

        Evolucion cerrada = evolucion(4L, EstadoEvolucion.CERRADA);
        cerrada.setHistoria(historia);
        when(evolucionRepo.findByHistoriaIdOrderByFechaDesc(5L)).thenReturn(List.of(cerrada));

        EvolucionAdenda adenda = new EvolucionAdenda();
        adenda.setId(15L);
        adenda.setEvolucion(cerrada);
        adenda.setTexto("Corrección");
        when(adendaRepo.findByEvolucionIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(adenda));

        List<EvolucionResponse> evoluciones = servicio.listarEvoluciones(1L);

        assertEquals(1, evoluciones.size());
        assertEquals(1, evoluciones.get(0).adendas().size());
        assertEquals("Corrección", evoluciones.get(0).adendas().get(0).texto());
    }

    // ------------------------------ Citas -------------------------------

    @Test
    void evolucionNoAdmiteCitaDeOtroPaciente() {
        Paciente paciente = paciente(1L);
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());
        when(historiaRepo.save(any(HistoriaClinica.class))).thenAnswer(inv -> {
            HistoriaClinica h = inv.getArgument(0);
            h.setId(9L);
            return h;
        });

        Cita cita = new Cita();
        cita.setId(3L);
        cita.setPaciente(paciente(2L));
        when(citaRepo.findById(3L)).thenReturn(Optional.of(cita));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.crearEvolucion(1L, new EvolucionRequest(null, null, 3L, "Motivo",
                        null, null, null, null, null, null, null, null, null, null, null)));

        assertEquals("CITA_DE_OTRO_PACIENTE", e.getCode());
    }

    // ------------------------------ Helpers -----------------------------

    private static Paciente paciente(Long id) {
        Paciente p = new Paciente();
        p.setId(id);
        p.setNombres("Ana");
        p.setApellidos("Torres");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        p.setEstado("ACTIVO");
        return p;
    }

    private static HistoriaClinica historia(Long id, Paciente paciente) {
        HistoriaClinica h = new HistoriaClinica();
        h.setId(id);
        h.setPaciente(paciente);
        h.setEstado(EstadoHistoria.ABIERTA);
        return h;
    }

    private static AntecedenteMedico antecedente(HistoriaClinica historia, TipoAntecedente tipo, String nombre) {
        AntecedenteMedico a = new AntecedenteMedico();
        a.setId(1L);
        a.setHistoria(historia);
        a.setTipo(tipo);
        a.setNombre(nombre);
        a.setEstado(EstadoAntecedente.ACTIVO);
        return a;
    }

    private static Evolucion evolucion(Long id, EstadoEvolucion estado) {
        Evolucion e = new Evolucion();
        e.setId(id);
        e.setHistoria(historia(5L, paciente(1L)));
        e.setMotivo("Control");
        e.setEstado(estado);
        e.setFecha(java.time.Instant.now());
        if (estado == EstadoEvolucion.CERRADA) {
            e.setCerradaEn(java.time.Instant.now());
            e.setCerradaPor(77L);
        }
        return e;
    }

    private static Evolucion evolucionCerrada() {
        return evolucion(4L, EstadoEvolucion.CERRADA);
    }
}

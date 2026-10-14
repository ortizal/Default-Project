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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Historia clínica odontológica — Fase B: antecedentes estructurados,
 * exámenes, diagnósticos y evoluciones (con adendas para notas cerradas).
 */
@Service
public class HistoriaClinicaService {

    private static final String MODULO = "HISTORIA_CLINICA";

    private final HistoriaClinicaRepository historiaRepository;
    private final AntecedenteMedicoRepository antecedenteRepository;
    private final ExamenClinicoRepository examenRepository;
    private final DiagnosticoClinicoRepository diagnosticoRepository;
    private final EvolucionRepository evolucionRepository;
    private final EvolucionAdendaRepository adendaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final CitaRepository citaRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public HistoriaClinicaService(HistoriaClinicaRepository historiaRepository,
                                  AntecedenteMedicoRepository antecedenteRepository,
                                  ExamenClinicoRepository examenRepository,
                                  DiagnosticoClinicoRepository diagnosticoRepository,
                                  EvolucionRepository evolucionRepository,
                                  EvolucionAdendaRepository adendaRepository,
                                  PacienteRepository pacienteRepository,
                                  OdontologoRepository odontologoRepository,
                                  CitaRepository citaRepository,
                                  AuditService auditService,
                                  CurrentUserService currentUserService) {
        this.historiaRepository = historiaRepository;
        this.antecedenteRepository = antecedenteRepository;
        this.examenRepository = examenRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.evolucionRepository = evolucionRepository;
        this.adendaRepository = adendaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.citaRepository = citaRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------
    // Resumen clínico
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResumenResponse resumen(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        HistoriaClinica historia = historiaDe(paciente);

        List<AntecedenteMedico> antecedentes = historia == null
                ? List.of()
                : antecedenteRepository.findByHistoriaIdOrderByTipoAscNombreAsc(historia.getId());
        List<DiagnosticoClinico> diagnosticos = historia == null
                ? List.of()
                : diagnosticoRepository.findByHistoriaIdOrderByFechaDesc(historia.getId());
        List<Evolucion> evoluciones = historia == null
                ? List.of()
                : evolucionRepository.findByHistoriaIdOrderByFechaDesc(historia.getId());

        List<String> alergias = nombresPorTipo(antecedentes, TipoAntecedente.ALERGIA, null);
        List<String> medicacion = nombresPorTipo(antecedentes, TipoAntecedente.MEDICACION, EstadoAntecedente.ACTIVO);

        List<DiagnosticoResponse> diagnosticosActivos = diagnosticos.stream()
                .filter(d -> d.getEstado() == EstadoDiagnostico.SOSPECHA || d.getEstado() == EstadoDiagnostico.CONFIRMADO)
                .map(DiagnosticoResponse::from)
                .toList();

        List<String> alertas = new ArrayList<>();
        boolean menorEdad = esMenor(paciente.getFechaNacimiento());
        if (menorEdad) {
            alertas.add("Paciente menor de edad");
        }
        if (!alergias.isEmpty()) {
            alertas.add("Alergias: " + String.join(", ", alergias));
        }
        if (historia != null && historia.getEstado() == EstadoHistoria.CERRADA) {
            alertas.add("Historia clínica cerrada");
        }
        diagnosticos.stream()
                .filter(d -> d.getSeveridad() == SeveridadDiagnostico.SEVERA
                        && (d.getEstado() == EstadoDiagnostico.SOSPECHA || d.getEstado() == EstadoDiagnostico.CONFIRMADO))
                .forEach(d -> alertas.add("Diagnóstico severo: " + d.getNombre()));

        return new ResumenResponse(
                historia == null ? null : historia.getId(),
                historia == null ? EstadoHistoria.ABIERTA : historia.getEstado(),
                pacienteResumen(paciente),
                historia == null || historia.getOdontologo() == null
                        ? null
                        : nombreCompleto(historia.getOdontologo()),
                alergias,
                medicacion,
                diagnosticosActivos,
                antecedentes.size(),
                evoluciones.size(),
                evoluciones.isEmpty() ? null : EvolucionResponse.from(evoluciones.get(0)),
                ultimaCita(pacienteId),
                proximaCita(pacienteId),
                alertas);
    }

    // ------------------------------------------------------------------
    // Antecedentes
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AntecedenteResponse> listarAntecedentes(Long pacienteId) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            return List.of();
        }
        return antecedenteRepository.findByHistoriaIdOrderByTipoAscNombreAsc(historia.getId()).stream()
                .map(AntecedenteResponse::from)
                .toList();
    }

    @Transactional
    public AntecedenteResponse crearAntecedente(Long pacienteId, AntecedenteRequest request) {
        HistoriaClinica historia = historiaOcrear(pacienteId);
        AntecedenteMedico antecedente = new AntecedenteMedico();
        antecedente.setHistoria(historia);
        aplicar(antecedente, request, true);
        antecedente.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));
        AntecedenteMedico guardado = antecedenteRepository.save(antecedente);
        auditService.registrar("CREAR_ANTECEDENTE", MODULO, "ANTECEDENTE",
                guardado.getId(), null, AntecedenteResponse.from(guardado));
        return AntecedenteResponse.from(guardado);
    }

    @Transactional
    public AntecedenteResponse actualizarAntecedente(Long id, AntecedenteRequest request) {
        AntecedenteMedico antecedente = antecedenteRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Antecedente"));
        AntecedenteResponse anterior = AntecedenteResponse.from(antecedente);
        aplicar(antecedente, request, false);
        AntecedenteMedico guardado = antecedenteRepository.save(antecedente);
        auditService.registrar("MODIFICAR_ANTECEDENTE", MODULO, "ANTECEDENTE",
                id, anterior, AntecedenteResponse.from(guardado));
        return AntecedenteResponse.from(guardado);
    }

    @Transactional
    public void eliminarAntecedente(Long id) {
        AntecedenteMedico antecedente = antecedenteRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Antecedente"));
        AntecedenteResponse anterior = AntecedenteResponse.from(antecedente);
        antecedenteRepository.delete(antecedente);
        auditService.registrar("ELIMINAR_ANTECEDENTE", MODULO, "ANTECEDENTE", id, anterior, null);
    }

    // ------------------------------------------------------------------
    // Exámenes
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ExamenResponse> listarExamenes(Long pacienteId) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            return List.of();
        }
        return examenRepository.findByHistoriaIdOrderByFechaDesc(historia.getId()).stream()
                .map(ExamenResponse::from)
                .toList();
    }

    @Transactional
    public ExamenResponse crearExamen(Long pacienteId, ExamenRequest request) {
        HistoriaClinica historia = historiaOcrear(pacienteId);
        ExamenClinico examen = new ExamenClinico();
        examen.setHistoria(historia);
        aplicar(examen, request);
        examen.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));
        ExamenClinico guardado = examenRepository.save(examen);
        auditService.registrar("CREAR_EXAMEN", MODULO, "EXAMEN_CLINICO",
                guardado.getId(), null, ExamenResponse.from(guardado));
        return ExamenResponse.from(guardado);
    }

    @Transactional
    public ExamenResponse actualizarExamen(Long id, ExamenRequest request) {
        ExamenClinico examen = examenRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Examen"));
        ExamenResponse anterior = ExamenResponse.from(examen);
        aplicar(examen, request);
        ExamenClinico guardado = examenRepository.save(examen);
        auditService.registrar("MODIFICAR_EXAMEN", MODULO, "EXAMEN_CLINICO",
                id, anterior, ExamenResponse.from(guardado));
        return ExamenResponse.from(guardado);
    }

    // ------------------------------------------------------------------
    // Diagnósticos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DiagnosticoResponse> listarDiagnosticos(Long pacienteId) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            return List.of();
        }
        return diagnosticoRepository.findByHistoriaIdOrderByFechaDesc(historia.getId()).stream()
                .map(DiagnosticoResponse::from)
                .toList();
    }

    @Transactional
    public DiagnosticoResponse crearDiagnostico(Long pacienteId, DiagnosticoRequest request) {
        HistoriaClinica historia = historiaOcrear(pacienteId);
        DiagnosticoClinico diagnostico = new DiagnosticoClinico();
        diagnostico.setHistoria(historia);
        aplicar(diagnostico, request, true);
        diagnostico.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));
        DiagnosticoClinico guardado = diagnosticoRepository.save(diagnostico);
        auditService.registrar("CREAR_DIAGNOSTICO", MODULO, "DIAGNOSTICO",
                guardado.getId(), null, DiagnosticoResponse.from(guardado));
        return DiagnosticoResponse.from(guardado);
    }

    @Transactional
    public DiagnosticoResponse actualizarDiagnostico(Long id, DiagnosticoRequest request) {
        DiagnosticoClinico diagnostico = diagnosticoRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Diagnóstico"));
        DiagnosticoResponse anterior = DiagnosticoResponse.from(diagnostico);
        aplicar(diagnostico, request, false);
        DiagnosticoClinico guardado = diagnosticoRepository.save(diagnostico);
        auditService.registrar("MODIFICAR_DIAGNOSTICO", MODULO, "DIAGNOSTICO",
                id, anterior, DiagnosticoResponse.from(guardado));
        return DiagnosticoResponse.from(guardado);
    }

    // ------------------------------------------------------------------
    // Evoluciones y adendas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EvolucionResponse> listarEvoluciones(Long pacienteId) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            return List.of();
        }
        return evolucionRepository.findByHistoriaIdOrderByFechaDesc(historia.getId()).stream()
                .map(e -> EvolucionResponse.from(e, adendasDe(e)))
                .toList();
    }

    @Transactional
    public EvolucionResponse crearEvolucion(Long pacienteId, EvolucionRequest request) {
        HistoriaClinica historia = historiaOcrear(pacienteId);
        Evolucion evolucion = new Evolucion();
        evolucion.setHistoria(historia);
        aplicar(evolucion, request, pacienteId, true);
        evolucion.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));
        Evolucion guardada = evolucionRepository.save(evolucion);
        auditService.registrar("CREAR_EVOLUCION", MODULO, "EVOLUCION",
                guardada.getId(), null, EvolucionResponse.from(guardada));
        return EvolucionResponse.from(guardada);
    }

    @Transactional
    public EvolucionResponse actualizarEvolucion(Long id, EvolucionRequest request) {
        Evolucion evolucion = evolucionOArrojar(id);
        if (evolucion.getEstado() == EstadoEvolucion.CERRADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "EVOLUCION_CERRADA",
                    "La evolución está cerrada: registre una adenda en lugar de editarla");
        }
        EvolucionResponse anterior = EvolucionResponse.from(evolucion);
        Long pacienteId = evolucion.getHistoria().getPaciente().getId();
        aplicar(evolucion, request, pacienteId, false);
        Evolucion guardada = evolucionRepository.save(evolucion);
        auditService.registrar("MODIFICAR_EVOLUCION", MODULO, "EVOLUCION",
                id, anterior, EvolucionResponse.from(guardada));
        return EvolucionResponse.from(guardada, adendasDe(guardada));
    }

    @Transactional
    public EvolucionResponse cerrarEvolucion(Long id) {
        Evolucion evolucion = evolucionOArrojar(id);
        if (evolucion.getEstado() == EstadoEvolucion.CERRADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "EVOLUCION_YA_CERRADA",
                    "La evolución ya estaba cerrada");
        }
        EvolucionResponse anterior = EvolucionResponse.from(evolucion);
        evolucion.setEstado(EstadoEvolucion.CERRADA);
        evolucion.setCerradaEn(Instant.now());
        currentUserService.idUsuarioActual().ifPresent(evolucion::setCerradaPor);
        Evolucion guardada = evolucionRepository.save(evolucion);
        auditService.registrar("CERRAR_EVOLUCION", MODULO, "EVOLUCION",
                id, anterior, EvolucionResponse.from(guardada));
        return EvolucionResponse.from(guardada, adendasDe(guardada));
    }

    @Transactional
    public AdendaResponse crearAdenda(Long evolucionId, AdendaRequest request) {
        Evolucion evolucion = evolucionOArrojar(evolucionId);
        if (evolucion.getEstado() != EstadoEvolucion.CERRADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "EVOLUCION_ABIERTA",
                    "Sólo se admiten adendas sobre una evolución cerrada");
        }
        EvolucionAdenda adenda = new EvolucionAdenda();
        adenda.setEvolucion(evolucion);
        adenda.setTexto(request.texto().trim());
        currentUserService.idUsuarioActual().ifPresent(adenda::setAutorId);
        EvolucionAdenda guardada = adendaRepository.save(adenda);
        auditService.registrar("CREAR_ADENDA", MODULO, "EVOLUCION",
                evolucionId, null, AdendaResponse.from(guardada));
        return AdendaResponse.from(guardada);
    }

    // ------------------------------------------------------------------
    // Aplicaciones / utilidades
    // ------------------------------------------------------------------

    private void aplicar(AntecedenteMedico antecedente, AntecedenteRequest request, boolean creacion) {
        antecedente.setTipo(request.tipo());
        antecedente.setNombre(request.nombre().trim());
        antecedente.setDetalle(request.detalle());
        antecedente.setFechaDesde(request.fechaDesde());
        antecedente.setObservaciones(request.observaciones());
        if (request.estado() != null) {
            antecedente.setEstado(request.estado());
        } else if (creacion && antecedente.getEstado() == null) {
            antecedente.setEstado(EstadoAntecedente.ACTIVO);
        }
    }

    private void aplicar(ExamenClinico examen, ExamenRequest request) {
        examen.setFecha(request.fecha());
        examen.setOdontologo(odontologoODefault(request.odontologoId()));
        examen.setGeneral(request.general());
        examen.setExtraoralAtm(request.extraoralAtm());
        examen.setExtraoralSimetria(request.extraoralSimetria());
        examen.setExtraoralGanglios(request.extraoralGanglios());
        examen.setExtraoralApertura(request.extraoralApertura());
        examen.setIntraoralMucosas(request.intraoralMucosas());
        examen.setIntraoralLengua(request.intraoralLengua());
        examen.setIntraoralPaladar(request.intraoralPaladar());
        examen.setIntraoralEncias(request.intraoralEncias());
        examen.setIntraoralHigiene(request.intraoralHigiene());
        examen.setIntraoralOclusion(request.intraoralOclusion());
        examen.setObservaciones(request.observaciones());
    }

    private void aplicar(DiagnosticoClinico diagnostico, DiagnosticoRequest request, boolean creacion) {
        diagnostico.setNombre(request.nombre().trim());
        diagnostico.setRegion(request.region());
        diagnostico.setOdontologo(odontologoODefault(request.odontologoId()));
        diagnostico.setObservaciones(request.observaciones());
        if (request.severidad() != null) {
            diagnostico.setSeveridad(request.severidad());
        } else if (creacion && diagnostico.getSeveridad() == null) {
            diagnostico.setSeveridad(SeveridadDiagnostico.MODERADA);
        }
        if (request.estado() != null) {
            diagnostico.setEstado(request.estado());
        } else if (creacion && diagnostico.getEstado() == null) {
            diagnostico.setEstado(EstadoDiagnostico.CONFIRMADO);
        }
        if (request.fecha() != null) {
            diagnostico.setFecha(request.fecha());
        } else if (creacion && diagnostico.getFecha() == null) {
            diagnostico.setFecha(LocalDate.now());
        }
    }

    private void aplicar(Evolucion evolucion, EvolucionRequest request, Long pacienteId, boolean creacion) {
        if (request.fecha() != null) {
            evolucion.setFecha(request.fecha());
        } else if (creacion && evolucion.getFecha() == null) {
            evolucion.setFecha(Instant.now());
        }
        evolucion.setOdontologo(odontologoODefault(request.odontologoId()));
        evolucion.setCita(citaODefault(request.citaId(), pacienteId));
        evolucion.setMotivo(request.motivo().trim());
        evolucion.setHallazgos(request.hallazgos());
        evolucion.setDiagnostico(request.diagnostico());
        evolucion.setProcedimiento(request.procedimiento());
        evolucion.setAnestesia(request.anestesia());
        evolucion.setMateriales(request.materiales());
        evolucion.setIndicaciones(request.indicaciones());
        evolucion.setConducta(request.conducta());
        evolucion.setSoapSubjetivo(request.soapSubjetivo());
        evolucion.setSoapObjetivo(request.soapObjetivo());
        evolucion.setSoapAnalisis(request.soapAnalisis());
        evolucion.setSoapPlan(request.soapPlan());
    }

    private Paciente pacienteOArrojar(Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> noEncontrado("Paciente"));
    }

    private HistoriaClinica historiaDe(Paciente paciente) {
        return historiaRepository.findByPacienteId(paciente.getId()).orElse(null);
    }

    private HistoriaClinica historiaOArrojarLectura(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        return historiaDe(paciente);
    }

    private HistoriaClinica historiaOcrear(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        HistoriaClinica existente = historiaDe(paciente);
        if (existente != null) {
            return existente;
        }
        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setEstado(EstadoHistoria.ABIERTA);
        HistoriaClinica creada = historiaRepository.save(historia);
        auditService.registrar("CREAR_HISTORIA", MODULO, "HISTORIA_CLINICA",
                creada.getId(), null, creada.getId());
        return creada;
    }

    private Evolucion evolucionOArrojar(Long id) {
        return evolucionRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Evolución"));
    }

    private List<AdendaResponse> adendasDe(Evolucion evolucion) {
        if (evolucion.getEstado() != EstadoEvolucion.CERRADA) {
            return List.of();
        }
        return adendaRepository.findByEvolucionIdOrderByCreatedAtDesc(evolucion.getId()).stream()
                .map(AdendaResponse::from)
                .toList();
    }

    private Odontologo odontologoODefault(Long odontologoId) {
        if (odontologoId == null) {
            return null;
        }
        return odontologoRepository.findById(odontologoId)
                .orElseThrow(() -> noEncontrado("Odontólogo"));
    }

    private Cita citaODefault(Long citaId, Long pacienteId) {
        if (citaId == null) {
            return null;
        }
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> noEncontrado("Cita"));
        if (cita.getPaciente() == null || !Objects.equals(cita.getPaciente().getId(), pacienteId)) {
            throw new BusinessException("CITA_DE_OTRO_PACIENTE", "La cita no pertenece a este paciente");
        }
        return cita;
    }

    private List<String> nombresPorTipo(List<AntecedenteMedico> antecedentes, TipoAntecedente tipo,
                                        EstadoAntecedente estado) {
        return antecedentes.stream()
                .filter(a -> a.getTipo() == tipo)
                .filter(a -> estado == null || a.getEstado() == estado)
                .map(a -> a.getDetalle() == null || a.getDetalle().isBlank()
                        ? a.getNombre()
                        : a.getNombre() + " (" + a.getDetalle() + ")")
                .toList();
    }

    private ResumenResponse.PacienteResumen pacienteResumen(Paciente paciente) {
        LocalDate nacimiento = paciente.getFechaNacimiento();
        int edad = nacimiento == null ? 0 : Period.between(nacimiento, LocalDate.now()).getYears();
        return new ResumenResponse.PacienteResumen(
                paciente.getId(), paciente.getNombres(), paciente.getApellidos(),
                nacimiento, edad, paciente.getTelefono(), paciente.getEmail(), edad < 18 && nacimiento != null);
    }

    private boolean esMenor(LocalDate fechaNacimiento) {
        return fechaNacimiento != null
                && Period.between(fechaNacimiento, LocalDate.now()).getYears() < 18;
    }

    private ResumenResponse.CitaResumen ultimaCita(Long pacienteId) {
        return citaRepository.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId).stream()
                .filter(c -> c.getEstado() == EstadoCita.ATENDIDA)
                .findFirst()
                .map(this::citaResumen)
                .orElse(null);
    }

    private ResumenResponse.CitaResumen proximaCita(Long pacienteId) {
        return citaRepository.proximasDelPaciente(pacienteId, LocalDate.now()).stream()
                .findFirst()
                .map(this::citaResumen)
                .orElse(null);
    }

    private ResumenResponse.CitaResumen citaResumen(Cita c) {
        return new ResumenResponse.CitaResumen(
                c.getId(), c.getFecha(), c.getHoraInicio(),
                c.getDoctor() == null ? null : nombreCompleto(c.getDoctor()),
                c.getServicio() == null ? null : c.getServicio().getNombre(),
                c.getEstado() == null ? null : c.getEstado().name());
    }

    private String nombreCompleto(Odontologo o) {
        return (o.getNombres() + " " + o.getApellidos()).trim();
    }

    private BusinessException noEncontrado(String entidad) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", entidad + " no encontrado");
    }
}

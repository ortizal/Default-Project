package org.dentalcrm.web.historia;

import org.dentalcrm.domain.archivo.ArchivoClinico;
import org.dentalcrm.domain.archivo.ArchivoClinicoRepository;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.consentimiento.Consentimiento;
import org.dentalcrm.domain.consentimiento.ConsentimientoRepository;
import org.dentalcrm.domain.consentimiento.EstadoConsentimiento;
import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.PlanTratamiento;
import org.dentalcrm.domain.plan.PlanTratamientoRepository;
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaLinea;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse.Fila;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse.Seccion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Datos de la historia clínica completa de un paciente (plan §4.17): arma
 * las secciones genéricas que después renderiza {@link HistoriaCompletaPdfService}.
 *
 * <p>Secciones: antecedentes, exámenes, diagnósticos, evoluciones,
 * odontograma vigente, periodoncia, planes, recetas, consentimientos,
 * archivos y citas atendidas.
 */
@Service
public class HistoriaCompletaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final AntecedenteMedicoRepository antecedenteRepository;
    private final ExamenClinicoRepository examenRepository;
    private final DiagnosticoClinicoRepository diagnosticoRepository;
    private final EvolucionRepository evolucionRepository;
    private final EvolucionAdendaRepository adendaRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final OdontogramaDienteRepository odontogramaDienteRepository;
    private final ExamenPeriodontalRepository periodonciaRepository;
    private final PlanTratamientoRepository planRepository;
    private final RecetaRepository recetaRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final ArchivoClinicoRepository archivoRepository;
    private final CitaRepository citaRepository;

    public HistoriaCompletaService(PacienteRepository pacienteRepository,
                                   HistoriaClinicaRepository historiaRepository,
                                   AntecedenteMedicoRepository antecedenteRepository,
                                   ExamenClinicoRepository examenRepository,
                                   DiagnosticoClinicoRepository diagnosticoRepository,
                                   EvolucionRepository evolucionRepository,
                                   EvolucionAdendaRepository adendaRepository,
                                   OdontogramaRepository odontogramaRepository,
                                   OdontogramaDienteRepository odontogramaDienteRepository,
                                   ExamenPeriodontalRepository periodonciaRepository,
                                   PlanTratamientoRepository planRepository,
                                   RecetaRepository recetaRepository,
                                   ConsentimientoRepository consentimientoRepository,
                                   ArchivoClinicoRepository archivoRepository,
                                   CitaRepository citaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.historiaRepository = historiaRepository;
        this.antecedenteRepository = antecedenteRepository;
        this.examenRepository = examenRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.evolucionRepository = evolucionRepository;
        this.adendaRepository = adendaRepository;
        this.odontogramaRepository = odontogramaRepository;
        this.odontogramaDienteRepository = odontogramaDienteRepository;
        this.periodonciaRepository = periodonciaRepository;
        this.planRepository = planRepository;
        this.recetaRepository = recetaRepository;
        this.consentimientoRepository = consentimientoRepository;
        this.archivoRepository = archivoRepository;
        this.citaRepository = citaRepository;
    }

    @Transactional(readOnly = true)
    public HistoriaCompletaResponse construir(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "NO_ENCONTRADO", "Paciente no encontrado"));
        HistoriaClinica historia = historiaRepository.findByPacienteId(paciente.getId()).orElse(null);
        Long historiaId = historia == null ? null : historia.getId();

        List<AntecedenteMedico> antecedentes = historiaId == null ? List.of()
                : antecedenteRepository.findByHistoriaIdOrderByTipoAscNombreAsc(historiaId);
        List<ExamenClinico> examenes = historiaId == null ? List.of()
                : examenRepository.findByHistoriaIdOrderByFechaDesc(historiaId);
        List<DiagnosticoClinico> diagnosticos = historiaId == null ? List.of()
                : diagnosticoRepository.findByHistoriaIdOrderByFechaDesc(historiaId);
        List<Evolucion> evoluciones = historiaId == null ? List.of()
                : evolucionRepository.findByHistoriaIdOrderByFechaDesc(historiaId);

        List<Seccion> secciones = new ArrayList<>();
        secciones.add(seccionAntecedentes(antecedentes));
        secciones.add(seccionExamenes(examenes));
        secciones.add(seccionDiagnosticos(diagnosticos));
        secciones.add(seccionEvoluciones(evoluciones));
        secciones.add(seccionOdontograma(historiaId));
        secciones.add(seccionPeriodoncia(historiaId));
        secciones.add(seccionPlanes(paciente.getId()));
        secciones.add(seccionRecetas(paciente.getId()));
        secciones.add(seccionConsentimientos(paciente.getId()));
        secciones.add(seccionArchivos(paciente.getId()));
        secciones.add(seccionCitas(paciente.getId()));

        return new HistoriaCompletaResponse(pacienteInfo(paciente, historia),
                resumen(historia, antecedentes, diagnosticos, evoluciones, examenes),
                secciones);
    }

    // ------------------------------------------------------------------
    // Cabecera y resumen
    // ------------------------------------------------------------------

    private HistoriaCompletaResponse.PacienteInfo pacienteInfo(Paciente paciente,
                                                              HistoriaClinica historia) {
        String edad = paciente.getFechaNacimiento() == null ? "—"
                : Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears() + " años";
        return new HistoriaCompletaResponse.PacienteInfo(
                nombre(paciente),
                vacio(paciente.getCedula()),
                edad,
                vacio(paciente.getTelefono()),
                vacio(paciente.getEmail()),
                historia == null || historia.getEstado() == null ? "Sin historia clínica"
                        : historia.getEstado().name(),
                historia == null || historia.getOdontologo() == null ? "—"
                        : nombre(historia.getOdontologo()),
                LocalDate.now().format(FECHA));
    }

    private List<String> resumen(HistoriaClinica historia,
                                 List<AntecedenteMedico> antecedentes,
                                 List<DiagnosticoClinico> diagnosticos,
                                 List<Evolucion> evoluciones,
                                 List<ExamenClinico> examenes) {
        String alergias = enPunto(antecedentes.stream()
                .filter(a -> a.getTipo() == TipoAntecedente.ALERGIA)
                .map(AntecedenteMedico::getNombre).toList());
        String medicacion = enPunto(antecedentes.stream()
                .filter(a -> a.getTipo() == TipoAntecedente.MEDICACION
                        && a.getEstado() == EstadoAntecedente.ACTIVO)
                .map(AntecedenteMedico::getNombre).toList());
        long diagnosticosActivos = diagnosticos.stream()
                .filter(d -> d.getEstado() == EstadoDiagnostico.SOSPECHA
                        || d.getEstado() == EstadoDiagnostico.CONFIRMADO).count();

        List<String> lineas = new ArrayList<>();
        lineas.add("Historia clínica: " + (historia == null || historia.getEstado() == null
                ? "sin crear" : historia.getEstado().name()));
        lineas.add("Odontólogo responsable: " + (historia == null || historia.getOdontologo() == null
                ? "—" : nombre(historia.getOdontologo())));
        lineas.add("Alergias: " + alergias);
        lineas.add("Medicación activa: " + medicacion);
        lineas.add("Diagnósticos activos: " + diagnosticosActivos);
        lineas.add("Registros: " + evoluciones.size() + " evoluciones · "
                + examenes.size() + " exámenes · " + diagnosticos.size() + " diagnósticos · "
                + antecedentes.size() + " antecedentes");
        if (!evoluciones.isEmpty()) {
            lineas.add("Última evolución: " + fecha(evoluciones.get(0).getFecha()) + " — "
                    + renglon(evoluciones.get(0).getMotivo(), 80));
        }
        return lineas;
    }

    // ------------------------------------------------------------------
    // Secciones
    // ------------------------------------------------------------------

    private Seccion seccionAntecedentes(List<AntecedenteMedico> items) {
        List<Fila> filas = items.stream()
                .map(a -> fila(a.getTipo() == null ? "—" : a.getTipo().name(),
                        vacio(a.getNombre()), vacio(a.getDetalle()),
                        a.getEstado() == null ? "—" : a.getEstado().name(),
                        a.getFechaDesde() == null ? "—" : a.getFechaDesde().format(FECHA)))
                .toList();
        return new Seccion("Antecedentes",
                List.of("Tipo", "Nombre", "Detalle", "Estado", "Desde"), filas);
    }

    private Seccion seccionExamenes(List<ExamenClinico> items) {
        List<Fila> filas = items.stream()
                .map(x -> fila(x.getFecha() == null ? "—" : x.getFecha().format(FECHA),
                        renglon(x.getGeneral(), 80),
                        renglon(x.getObservaciones(), 80)))
                .toList();
        return new Seccion("Exámenes clínicos",
                List.of("Fecha", "General", "Observaciones"), filas);
    }

    private Seccion seccionDiagnosticos(List<DiagnosticoClinico> items) {
        List<Fila> filas = items.stream()
                .map(d -> fila(d.getFecha() == null ? "—" : d.getFecha().format(FECHA),
                        vacio(d.getNombre()), vacio(d.getRegion()),
                        d.getSeveridad() == null ? "—" : d.getSeveridad().name(),
                        d.getEstado() == null ? "—" : d.getEstado().name()))
                .toList();
        return new Seccion("Diagnósticos",
                List.of("Fecha", "Nombre", "Región", "Severidad", "Estado"), filas);
    }

    private Seccion seccionEvoluciones(List<Evolucion> items) {
        List<Fila> filas = new ArrayList<>();
        for (Evolucion e : items) {
            filas.add(fila(fecha(e.getFecha()), vacio(e.getMotivo()),
                    renglon(e.getHallazgos(), 90), renglon(e.getProcedimiento(), 60),
                    e.getEstado() == null ? "—" : e.getEstado().name()));
            for (EvolucionAdenda adenda : adendaRepository.findByEvolucionIdOrderByCreatedAtDesc(e.getId())) {
                filas.add(fila("—", "↳ Adenda " + fechaHora(adenda.getCreatedAt()),
                        renglon(adenda.getTexto(), 120), "—", "—"));
            }
        }
        return new Seccion("Evoluciones",
                List.of("Fecha", "Motivo", "Hallazgos", "Procedimiento", "Estado"), filas);
    }

    private Seccion seccionOdontograma(Long historiaId) {
        List<Fila> filas = new ArrayList<>();
        String titulo = "Odontograma (sin registros)";
        if (historiaId != null) {
            Odontograma odontograma = odontogramaRepository
                    .findFirstByHistoriaIdOrderByVersionDesc(historiaId).orElse(null);
            if (odontograma != null) {
                titulo = "Odontograma vigente (v" + odontograma.getVersion() + " · "
                        + fecha(odontograma.getFecha()) + ")";
                for (OdontogramaDiente d : odontogramaDienteRepository
                        .findByOdontogramaIdOrderByDienteAsc(odontograma.getId())) {
                    filas.add(fila(String.valueOf(d.getDiente()),
                            d.getSuperficie() == null ? "—" : d.getSuperficie().name(),
                            d.getEstado() == null ? "—" : d.getEstado().name(),
                            vacio(d.getCondicion())));
                }
            }
        }
        return new Seccion(titulo, List.of("Pieza", "Superficie", "Estado", "Condición"), filas);
    }

    private Seccion seccionPeriodoncia(Long historiaId) {
        List<Fila> filas = new ArrayList<>();
        if (historiaId != null) {
            for (ExamenPeriodontal p : periodonciaRepository.findByHistoriaIdOrderByVersionDesc(historiaId)) {
                filas.add(fila("v" + p.getVersion(),
                        p.getFecha() == null ? "—" : p.getFecha().format(FECHA),
                        renglon(p.getObservaciones(), 100)));
            }
        }
        return new Seccion("Periodoncia", List.of("Versión", "Fecha", "Observaciones"), filas);
    }

    private Seccion seccionPlanes(Long pacienteId) {
        List<Fila> filas = new ArrayList<>();
        for (PlanTratamiento p : planRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId)) {
            filas.add(fila(p.getFecha() == null ? "—" : p.getFecha().format(FECHA),
                    vacio(p.getTitulo()),
                    p.getEstado() == null ? "—" : p.getEstado().name(),
                    String.valueOf(p.getLineas().size()),
                    p.getTotal() == null ? "0" : p.getTotal().toPlainString()));
        }
        return new Seccion("Planes de tratamiento",
                List.of("Fecha", "Título", "Estado", "Líneas", "Total (USD)"), filas);
    }

    private Seccion seccionRecetas(Long pacienteId) {
        List<Fila> filas = new ArrayList<>();
        for (Receta r : recetaRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId)) {
            List<String> medicamentos = new ArrayList<>();
            for (RecetaLinea l : r.getLineas()) {
                medicamentos.add(l.getMedicamento()
                        + (l.getConcentracion() == null || l.getConcentracion().isBlank()
                        ? "" : " " + l.getConcentracion())
                        + (l.getDosis() == null || l.getDosis().isBlank() ? "" : " · " + l.getDosis()));
            }
            filas.add(fila(r.getFecha() == null ? "—" : r.getFecha().format(FECHA),
                    renglon(r.getDiagnostico(), 70),
                    medicamentos.isEmpty() ? "—" : renglon(String.join("; ", medicamentos), 160),
                    r.getEstado() == null ? "—" : r.getEstado().name()));
        }
        return new Seccion("Recetas",
                List.of("Fecha", "Diagnóstico", "Medicamentos", "Estado"), filas);
    }

    private Seccion seccionConsentimientos(Long pacienteId) {
        List<Fila> filas = new ArrayList<>();
        for (Consentimiento c : consentimientoRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId)) {
            filas.add(fila(c.getFecha() == null ? "—" : c.getFecha().format(FECHA),
                    vacio(c.getTitulo()),
                    c.getEstado() == null ? "—" : c.getEstado().name(),
                    c.getEstado() == EstadoConsentimiento.FIRMADO ? vacio(c.getFirmante()) : "—"));
        }
        return new Seccion("Consentimientos",
                List.of("Fecha", "Título", "Estado", "Firmante"), filas);
    }

    private Seccion seccionArchivos(Long pacienteId) {
        List<Fila> filas = new ArrayList<>();
        for (ArchivoClinico a : archivoRepository.findByPacienteIdOrderByCreatedAtDescIdDesc(pacienteId)) {
            filas.add(fila(a.getFecha() == null ? fecha(a.getCreatedAt())
                            : a.getFecha().format(FECHA),
                    a.getCategoria() == null ? "—" : a.getCategoria().name(),
                    vacio(a.getNombre()), formatoTamano(a.getTamano())));
        }
        return new Seccion("Imágenes y archivos",
                List.of("Fecha", "Categoría", "Archivo", "Tamaño"), filas);
    }

    private Seccion seccionCitas(Long pacienteId) {
        List<Fila> filas = new ArrayList<>();
        for (Cita c : citaRepository.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId)) {
            if (c.getEstado() != EstadoCita.ATENDIDA) {
                continue;
            }
            filas.add(fila(c.getFecha() == null ? "—" : c.getFecha().format(FECHA),
                    c.getHoraInicio() == null ? "—" : c.getHoraInicio().toString(),
                    c.getServicio() == null ? "—" : vacio(c.getServicio().getNombre()),
                    c.getDoctor() == null ? "—" : nombre(c.getDoctor())));
        }
        return new Seccion("Citas atendidas (últimas 10)",
                List.of("Fecha", "Hora", "Servicio", "Profesional"), filas);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static Fila fila(String... celdas) {
        return new Fila(List.of(celdas));
    }

    private static String nombre(Paciente paciente) {
        return texto(paciente.getNombres()) + " " + texto(paciente.getApellidos());
    }

    private static String nombre(Odontologo odontologo) {
        return texto(odontologo.getNombres()) + " " + texto(odontologo.getApellidos());
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String vacio(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor.trim();
    }

    private static String enPunto(List<String> valores) {
        List<String> limpios = valores.stream().map(String::trim).filter(v -> !v.isBlank()).toList();
        return limpios.isEmpty() ? "ninguna registrada" : String.join(", ", limpios);
    }

    private static String fecha(LocalDate valor) {
        return valor == null ? "—" : valor.format(FECHA);
    }

    private static String fecha(Instant valor) {
        return valor == null ? "—" : FECHA.format(valor.atZone(ZoneOffset.UTC));
    }

    private static String fechaHora(Instant valor) {
        return valor == null ? "—" : FECHA_HORA.format(valor.atZone(ZoneOffset.UTC));
    }

    private static String renglon(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return "—";
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.length() <= max) {
            return limpio;
        }
        return limpio.substring(0, max - 1) + "…";
    }

    private static String formatoTamano(Long bytes) {
        long b = bytes == null ? 0L : bytes;
        if (b >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024.0));
        }
        if (b >= 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", b / 1024.0);
        }
        return b + " B";
    }
}

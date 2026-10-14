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
import org.dentalcrm.domain.plan.EstadoLineaPlan;
import org.dentalcrm.domain.plan.PlanLinea;
import org.dentalcrm.domain.plan.PlanTratamiento;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.PlanTratamientoRepository;
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.historia.dto.TimelineEventoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Línea temporal unificada del paciente (plan §4.16): consultas,
 * odontogramas, diagnósticos, planes, procedimientos, evoluciones, recetas,
 * archivos, consentimientos y controles periodontales en una sola lista.
 *
 * <p>Cada fuente se limita a {@value #LIMITE_POR_FUENTE} eventos antes de
 * ordenar, y el total nunca supera {@value #LIMITE_TIMELINE}. Los eventos de
 * paciente se muestran aunque la historia clínica todavía no exista.
 */
@Service
public class TimelineService {

    private static final int LIMITE_POR_FUENTE = 25;
    private static final int LIMITE_TIMELINE = 100;

    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final EvolucionRepository evolucionRepository;
    private final DiagnosticoClinicoRepository diagnosticoRepository;
    private final ExamenClinicoRepository examenRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final ExamenPeriodontalRepository examenPeriodontalRepository;
    private final CitaRepository citaRepository;
    private final PlanTratamientoRepository planRepository;
    private final RecetaRepository recetaRepository;
    private final ArchivoClinicoRepository archivoRepository;
    private final ConsentimientoRepository consentimientoRepository;

    public TimelineService(PacienteRepository pacienteRepository,
                           HistoriaClinicaRepository historiaRepository,
                           EvolucionRepository evolucionRepository,
                           DiagnosticoClinicoRepository diagnosticoRepository,
                           ExamenClinicoRepository examenRepository,
                           OdontogramaRepository odontogramaRepository,
                           ExamenPeriodontalRepository examenPeriodontalRepository,
                           CitaRepository citaRepository,
                           PlanTratamientoRepository planRepository,
                           RecetaRepository recetaRepository,
                           ArchivoClinicoRepository archivoRepository,
                           ConsentimientoRepository consentimientoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.historiaRepository = historiaRepository;
        this.evolucionRepository = evolucionRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.examenRepository = examenRepository;
        this.odontogramaRepository = odontogramaRepository;
        this.examenPeriodontalRepository = examenPeriodontalRepository;
        this.citaRepository = citaRepository;
        this.planRepository = planRepository;
        this.recetaRepository = recetaRepository;
        this.archivoRepository = archivoRepository;
        this.consentimientoRepository = consentimientoRepository;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventoResponse> timeline(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "NO_ENCONTRADO", "Paciente no encontrado"));
        Long historiaId = historiaRepository.findByPacienteId(paciente.getId())
                .map(HistoriaClinica::getId).orElse(null);

        List<TimelineEventoResponse> eventos = new ArrayList<>();
        if (historiaId != null) {
            eventos.addAll(eventosDeHistoria(historiaId));
        }
        eventos.addAll(eventosDePaciente(paciente.getId()));

        eventos.sort(Comparator.comparing(TimelineEventoResponse::fecha,
                        Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                .thenComparing(TimelineEventoResponse::tipo)
                .thenComparing(TimelineEventoResponse::referenciaId,
                        Comparator.nullsLast(Comparator.<Long>reverseOrder())));
        return eventos.size() > LIMITE_TIMELINE ? List.copyOf(eventos.subList(0, LIMITE_TIMELINE)) : eventos;
    }

    // ------------------------------------------------------------------
    // Fuentes
    // ------------------------------------------------------------------

    private List<TimelineEventoResponse> eventosDeHistoria(Long historiaId) {
        List<TimelineEventoResponse> eventos = new ArrayList<>();

        for (Evolucion e : limitar(evolucionRepository.findByHistoriaIdOrderByFechaDesc(historiaId))) {
            eventos.add(new TimelineEventoResponse("EVOLUCION", e.getFecha(), e.getMotivo(),
                    resumen(e.getHallazgos(), 160), e.getId()));
        }
        for (DiagnosticoClinico d : limitar(diagnosticoRepository.findByHistoriaIdOrderByFechaDesc(historiaId))) {
            String detalle = d.getRegion() == null || d.getRegion().isBlank()
                    ? d.getSeveridad().name() + " · " + d.getEstado().name()
                    : d.getRegion() + " · " + d.getSeveridad().name() + " · " + d.getEstado().name();
            eventos.add(new TimelineEventoResponse("DIAGNOSTICO", inicioDelDia(d.getFecha()),
                    d.getNombre(), detalle, d.getId()));
        }
        for (ExamenClinico x : limitar(examenRepository.findByHistoriaIdOrderByFechaDesc(historiaId))) {
            eventos.add(new TimelineEventoResponse("EXAMEN", inicioDelDia(x.getFecha()),
                    "Examen clínico", resumen(x.getObservaciones(), 160), x.getId()));
        }
        for (Odontograma o : limitar(odontogramaRepository.findByHistoriaIdOrderByVersionDesc(historiaId))) {
            String detalle = resumen(o.getObservaciones(), 160);
            if (detalle == null) {
                detalle = "Dentición " + (o.getDenticion() == null ? "" : o.getDenticion().name());
            }
            eventos.add(new TimelineEventoResponse("ODONTOGRAMA", inicioDelDia(o.getFecha()),
                    "Odontograma (v" + o.getVersion() + ")", detalle, o.getId()));
        }
        for (ExamenPeriodontal p : limitar(examenPeriodontalRepository.findByHistoriaIdOrderByVersionDesc(historiaId))) {
            eventos.add(new TimelineEventoResponse("PERIODONCIA", inicioDelDia(p.getFecha()),
                    "Control periodontal (v" + p.getVersion() + ")",
                    resumen(p.getObservaciones(), 160), p.getId()));
        }
        return eventos;
    }

    private List<TimelineEventoResponse> eventosDePaciente(Long pacienteId) {
        List<TimelineEventoResponse> eventos = new ArrayList<>();

        for (Cita c : citaRepository.findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId)) {
            if (c.getEstado() != EstadoCita.ATENDIDA) {
                continue;
            }
            eventos.add(new TimelineEventoResponse("CITA", inicioDelDia(c.getFecha()),
                    "Consulta atendida", detalleCita(c), c.getId()));
        }
        for (PlanTratamiento plan : limitar(planRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId))) {
            String detalle = legible(plan.getEstado()) + " · " + plan.getLineas().size()
                    + (plan.getLineas().size() == 1 ? " línea · " : " líneas · ")
                    + plan.getTotal().toPlainString();
            eventos.add(new TimelineEventoResponse("PLAN", inicioDelDia(plan.getFecha()),
                    plan.getTitulo(), detalle, plan.getId()));
            for (PlanLinea linea : plan.getLineas()) {
                if (linea.getEstado() != EstadoLineaPlan.EN_EJECUCION
                        && linea.getEstado() != EstadoLineaPlan.COMPLETADO) {
                    continue;
                }
                eventos.add(new TimelineEventoResponse("PROCEDIMIENTO", inicioDelDia(plan.getFecha()),
                        linea.getProcedimiento(), detalleLinea(linea, plan), linea.getId()));
            }
        }
        for (Receta r : limitar(recetaRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId))) {
            String detalle = legible(r.getEstado()) + " · " + r.getLineas().size()
                    + (r.getLineas().size() == 1 ? " medicamento" : " medicamentos");
            String titulo = resumen(r.getDiagnostico(), 150);
            eventos.add(new TimelineEventoResponse("RECETA", inicioDelDia(r.getFecha()),
                    titulo == null ? "Receta de medicación" : titulo, detalle, r.getId()));
        }
        for (ArchivoClinico a : limitar(archivoRepository.findByPacienteIdOrderByCreatedAtDescIdDesc(pacienteId))) {
            Instant fecha = a.getFecha() == null ? a.getCreatedAt() : inicioDelDia(a.getFecha());
            eventos.add(new TimelineEventoResponse("ARCHIVO", fecha, a.getNombre(),
                    legible(a.getCategoria()) + " · " + formatoTamano(a.getTamano()), a.getId()));
        }
        for (Consentimiento c : limitar(consentimientoRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId))) {
            String detalle = legible(c.getEstado());
            if (c.getEstado() == EstadoConsentimiento.FIRMADO
                    && c.getFirmante() != null && !c.getFirmante().isBlank()) {
                detalle += " · " + c.getFirmante();
            }
            eventos.add(new TimelineEventoResponse("CONSENTIMIENTO", inicioDelDia(c.getFecha()),
                    c.getTitulo(), detalle, c.getId()));
        }
        return eventos;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private String detalleCita(Cita c) {
        StringBuilder sb = new StringBuilder();
        if (c.getServicio() != null) {
            sb.append(c.getServicio().getNombre());
        }
        if (c.getDoctor() != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(c.getDoctor().getNombres().trim()).append(' ')
                    .append(c.getDoctor().getApellidos().trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String detalleLinea(PlanLinea linea, PlanTratamiento plan) {
        StringBuilder sb = new StringBuilder();
        if (linea.getDiente() != null) {
            sb.append("Pieza ").append(linea.getDiente());
            if (linea.getSuperficie() != null && !linea.getSuperficie().isBlank()) {
                sb.append(" · ").append(linea.getSuperficie());
            }
            sb.append(" · ");
        }
        sb.append(legible(linea.getEstado())).append(" · ").append(plan.getTitulo());
        return sb.toString();
    }

    private <T> List<T> limitar(List<T> fuente) {
        if (fuente == null || fuente.isEmpty()) {
            return List.of();
        }
        return fuente.size() > LIMITE_POR_FUENTE ? List.copyOf(fuente.subList(0, LIMITE_POR_FUENTE)) : fuente;
    }

    private String legible(Enum<?> valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private String formatoTamano(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        if (bytes >= 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private String resumen(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.length() <= max ? limpio : limpio.substring(0, max - 1) + "…";
    }
}

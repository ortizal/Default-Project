package org.dentalcrm.web.historia;

import org.dentalcrm.domain.archivo.ArchivoClinicoRepository;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.consentimiento.ConsentimientoRepository;
import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.EstadoLineaPlan;
import org.dentalcrm.domain.plan.EstadoPlanTratamiento;
import org.dentalcrm.domain.plan.PlanLineaRepository;
import org.dentalcrm.domain.plan.PlanTratamientoRepository;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.historia.dto.ReporteClinicoResponse;
import org.dentalcrm.web.historia.dto.ReporteClinicoResponse.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reportes clínicos del consultorio (plan §4.17): totales del período,
 * diagnósticos frecuentes, tratamientos pendientes, continuidad de la
 * atención y producción clínica.
 *
 * <p>Permisos: exige {@code REPORTES_CLINICOS} (lo decide el controlador);
 * ninguna consulta escribe datos.
 */
@Service
public class ReporteClinicoService {

    private static final int LIMITE_DEFECTO = 10;
    private static final int LIMITE_MAXIMO = 50;
    private static final int DIAS_SIN_ACTIVIDAD = 90;

    private final PacienteRepository pacienteRepository;
    private final EvolucionRepository evolucionRepository;
    private final ExamenClinicoRepository examenRepository;
    private final DiagnosticoClinicoRepository diagnosticoRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final ExamenPeriodontalRepository periodonciaRepository;
    private final RecetaRepository recetaRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final ArchivoClinicoRepository archivoRepository;
    private final PlanTratamientoRepository planRepository;
    private final PlanLineaRepository planLineaRepository;
    private final CitaRepository citaRepository;
        private final ZoneId zona;

    public ReporteClinicoService(PacienteRepository pacienteRepository,
                                 EvolucionRepository evolucionRepository,
                                 ExamenClinicoRepository examenRepository,
                                 DiagnosticoClinicoRepository diagnosticoRepository,
                                 OdontogramaRepository odontogramaRepository,
                                 ExamenPeriodontalRepository periodonciaRepository,
                                 RecetaRepository recetaRepository,
                                 ConsentimientoRepository consentimientoRepository,
                                 ArchivoClinicoRepository archivoRepository,
                                 PlanTratamientoRepository planRepository,
                                 PlanLineaRepository planLineaRepository,
                                 CitaRepository citaRepository,
                                 @Value("${app.timezone}") String zona) {
        this.pacienteRepository = pacienteRepository;
        this.evolucionRepository = evolucionRepository;
        this.examenRepository = examenRepository;
        this.diagnosticoRepository = diagnosticoRepository;
        this.odontogramaRepository = odontogramaRepository;
        this.periodonciaRepository = periodonciaRepository;
        this.recetaRepository = recetaRepository;
        this.consentimientoRepository = consentimientoRepository;
        this.archivoRepository = archivoRepository;
        this.planRepository = planRepository;
        this.planLineaRepository = planLineaRepository;
        this.citaRepository = citaRepository;
        this.zona = ZoneId.of(zona);
    }

    @Transactional(readOnly = true)
    public ReporteClinicoResponse reporte(LocalDate desde, LocalDate hasta, int limite) {
        LocalDate fin = hasta == null ? LocalDate.now(zona) : hasta;
        LocalDate inicio = desde == null ? fin.minusYears(1) : desde;
        if (inicio.isAfter(fin)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RANGO_INVALIDO",
                    "La fecha de inicio no puede ser posterior a la fecha final");
        }
        int tope = limite <= 0 ? LIMITE_DEFECTO : Math.min(limite, LIMITE_MAXIMO);

        Instant desdeInstant = inicio.atStartOfDay(zona).toInstant();
        Instant hastaInstant = fin.plusDays(1).atStartOfDay(zona).toInstant();

        Set<Long> activos = pacientesActivos(inicio, fin, desdeInstant, hastaInstant);

        Totales totales = new Totales(
                (int) evolucionRepository.countByFechaBetween(desdeInstant, hastaInstant),
                (int) examenRepository.countByFechaBetween(inicio, fin),
                (int) diagnosticoRepository.countByFechaBetween(inicio, fin),
                (int) odontogramaRepository.countByFechaBetween(inicio, fin),
                (int) periodonciaRepository.countByFechaBetween(inicio, fin),
                (int) recetaRepository.countByFechaBetween(inicio, fin),
                (int) consentimientoRepository.countByFechaBetween(inicio, fin),
                (int) archivoRepository.countByCreatedAtBetween(desdeInstant, hastaInstant),
                (int) planRepository.countByFechaBetween(inicio, fin),
                (int) citaRepository.countByEstadoAndFechaBetween(EstadoCita.ATENDIDA, inicio, fin),
                activos.size());

        List<FrecuenciaDiagnostico> frecuentes = diagnosticoRepository
                .masFrecuentes(inicio, fin, PageRequest.of(0, tope))
                .stream()
                .map(f -> new FrecuenciaDiagnostico(f.getNombre(),
                        Math.toIntExact(f.getTotal()), Math.toIntExact(f.getPacientes())))
                .toList();

        List<TratamientoPendiente> pendientes = planLineaRepository
                .buscarPendientes(List.of(EstadoLineaPlan.PENDIENTE, EstadoLineaPlan.EN_EJECUCION),
                        EstadoPlanTratamiento.CANCELADO, PageRequest.of(0, tope))
                .stream()
                .map(l -> new TratamientoPendiente(
                        l.getPlan().getPaciente().getId(),
                        nombre(l.getPlan().getPaciente()),
                        l.getPlan().getId(),
                        l.getPlan().getTitulo(),
                        l.getId(),
                        l.getProcedimiento(),
                        l.getDiente(),
                        l.getSuperficie(),
                        l.getPrioridad() == null ? null : l.getPrioridad().name(),
                        l.getEstado() == null ? null : l.getEstado().name(),
                        l.getSubtotal()))
                .toList();

        Continuidad continuidad = continuidad(inicio, fin, hastaInstant, activos, tope);

        List<ProduccionClinica> produccion = planLineaRepository
                .produccion(inicio, fin, PageRequest.of(0, tope))
                .stream()
                .map(p -> new ProduccionClinica(p.getProcedimiento(),
                        Math.toIntExact(p.getCantidad()), p.getTotal()))
                .toList();

        return new ReporteClinicoResponse(inicio, fin, totales, frecuentes, pendientes,
                continuidad, produccion);
    }

    // ------------------------------------------------------------------
    // Continuidad
    // ------------------------------------------------------------------

    private Continuidad continuidad(LocalDate inicio, LocalDate fin, Instant hastaInstant,
                                   Set<Long> activosPeriodo, int tope) {
        int sinProximaCita = Math.toIntExact(pacienteRepository.countSinProximaCita(
                LocalDate.now(zona), List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, EstadoCita.ATENDIDA)));

        Instant desde90 = fin.minusDays(DIAS_SIN_ACTIVIDAD).atStartOfDay(zona).toInstant();
        Set<Long> recientes = desde90.isBefore(hastaInstant)
                ? pacientesActivos(fin.minusDays(DIAS_SIN_ACTIVIDAD), fin, desde90, hastaInstant)
                : Set.of();

        List<SinActividad> sinActividad = pacienteRepository.findAll().stream()
                .filter(p -> !recientes.contains(p.getId()))
                .sorted((a, b) -> String.valueOf(nombre(a)).compareToIgnoreCase(String.valueOf(nombre(b))))
                .limit(tope)
                .map(p -> new SinActividad(p.getId(), nombre(p)))
                .toList();

        return new Continuidad(
                (int) citaRepository.countByEstadoAndFechaBetween(EstadoCita.ATENDIDA, inicio, fin),
                (int) citaRepository.countByEstadoAndFechaBetween(EstadoCita.CONFIRMADA, inicio, fin),
                (int) citaRepository.countByEstadoAndFechaBetween(EstadoCita.CANCELADA, inicio, fin),
                (int) citaRepository.countByEstadoAndFechaBetween(EstadoCita.NO_ASISTIO, inicio, fin),
                activosPeriodo.size(),
                sinProximaCita,
                sinActividad);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Pacientes con actividad clínica (evolución, examen, odontograma o receta) en el rango. */
    private Set<Long> pacientesActivos(LocalDate inicio, LocalDate fin,
                                       Instant desdeInstant, Instant hastaInstant) {
        Set<Long> activos = new HashSet<>();
        for (Evolucion e : evolucionRepository.findByFechaBetween(desdeInstant, hastaInstant)) {
            if (e.getHistoria() != null && e.getHistoria().getPaciente() != null) {
                activos.add(e.getHistoria().getPaciente().getId());
            }
        }
        for (ExamenClinico x : examenRepository.findByFechaBetween(inicio, fin)) {
            if (x.getHistoria() != null && x.getHistoria().getPaciente() != null) {
                activos.add(x.getHistoria().getPaciente().getId());
            }
        }
        for (Odontograma o : odontogramaRepository.findByFechaBetween(inicio, fin)) {
            if (o.getHistoria() != null && o.getHistoria().getPaciente() != null) {
                activos.add(o.getHistoria().getPaciente().getId());
            }
        }
        for (org.dentalcrm.domain.receta.Receta r : recetaRepository.findByFechaBetween(inicio, fin)) {
            if (r.getPaciente() != null) {
                activos.add(r.getPaciente().getId());
            }
        }
        return activos;
    }

    private static String nombre(Paciente paciente) {
        return ((paciente.getNombres() == null ? "" : paciente.getNombres().trim()) + " "
                + (paciente.getApellidos() == null ? "" : paciente.getApellidos().trim())).trim();
    }
}

package org.dentalcrm.web.agente.tool;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Operaciones sobre citas con idempotencia y revalidación (§19):
 * un webhook duplicado no crea dos citas y un slot mostrado no es un slot reservado.
 */
public class AppointmentTool {

    public static final String OBSERVACIONES_AGENTE = "Agendada por el agente de IA (WhatsApp)";

    /** Resultado de la creación; {@code alternativas} sólo se llena si el slot se ocupó. */
    public record ResultadoCreacion(CitaResponse cita, boolean duplicada,
                                    List<LocalTime> alternativas, String error) {
        static ResultadoCreacion ok(CitaResponse c) {
            return new ResultadoCreacion(c, false, List.of(), null);
        }

        static ResultadoCreacion repetida(CitaResponse c) {
            return new ResultadoCreacion(c, true, List.of(), null);
        }

        static ResultadoCreacion ocupado(List<LocalTime> alternativas) {
            return new ResultadoCreacion(null, false, alternativas, null);
        }

        static ResultadoCreacion fallo(String error) {
            return new ResultadoCreacion(null, false, List.of(), error);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AppointmentTool.class);

    private final CitaRepository citaRepository;
    private final CitaService citaService;
    private final AvailabilityTool disponibilidad;

    public AppointmentTool(CitaRepository citaRepository, CitaService citaService,
                           AvailabilityTool disponibilidad) {
        this.citaRepository = citaRepository;
        this.citaService = citaService;
        this.disponibilidad = disponibilidad;
    }

    public List<Cita> proximas(Paciente paciente) {
        if (paciente == null) {
            return List.of();
        }
        return citaRepository.proximasDelPaciente(paciente.getId(), LocalDate.now()).stream()
                .filter(c -> c.getEstado().esActiva() || c.getEstado() == EstadoCita.PENDIENTE)
                .toList();
    }

    public Optional<Cita> proxima(Paciente paciente) {
        return proximas(paciente).stream().findFirst();
    }

    public Cita porId(Long id) {
        if (id == null) {
            return null;
        }
        return citaRepository.findById(id).orElse(null);
    }

    public CitaResponse confirmar(Cita cita) {
        return citaService.confirmar(cita.getId(), "WHATSAPP");
    }

    public CitaResponse cancelar(Cita cita, String motivo) {
        return citaService.cancelar(cita.getId(), new CancelarRequest(motivo));
    }

    public CitaResponse actualizar(Cita cita, LocalDate fecha, LocalTime hora) {
        return citaService.actualizar(cita.getId(), new CitaRequest(
                cita.getPaciente().getId(), cita.getDoctor().getId(), cita.getServicio().getId(),
                fecha, hora, cita.getObservaciones()));
    }

    /**
     * Crea la cita revalidando el slot y evitando duplicados:
     *
     * <ol>
     *   <li>si ya existe esa misma cita activa se devuelve tal cual (idempotencia);</li>
     *   <li>si el slot dejó de estar libre se devuelven las alternativas reales;</li>
     *   <li>recién entonces se pide la creación al backend.</li>
     * </ol>
     */
    public ResultadoCreacion crear(Paciente paciente, Long doctorId, Long servicioId,
                                   LocalDate fecha, LocalTime hora) {
        if (paciente == null || doctorId == null || servicioId == null || fecha == null || hora == null) {
            return ResultadoCreacion.fallo("Faltan datos de la cita.");
        }
        Cita existente = proximas(paciente).stream()
                .filter(c -> c.getFecha().equals(fecha)
                        && c.getHoraInicio().equals(hora)
                        && c.getDoctor().getId().equals(doctorId)
                        && c.getServicio().getId().equals(servicioId))
                .findFirst()
                .orElse(null);
        if (existente != null) {
            log.info("Agente: idempotencia, la cita {} ya existía para el paciente {}",
                    existente.getId(), paciente.getId());
            return ResultadoCreacion.repetida(CitaResponse.from(existente));
        }
        List<SlotResponse> slots = disponibilidad.slots(doctorId, fecha, servicioId);
        if (!disponibilidad.contiene(slots, hora)) {
            List<LocalTime> alternativas = slots.stream().map(SlotResponse::horaInicio).limit(6).toList();
            return ResultadoCreacion.ocupado(alternativas);
        }
        try {
            CitaResponse creada = citaService.crear(new CitaRequest(
                    paciente.getId(), doctorId, servicioId, fecha, hora, OBSERVACIONES_AGENTE));
            log.info("Agente: cita {} creada para el paciente {}", creada.id(), paciente.getId());
            return ResultadoCreacion.ok(creada);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo crear la cita del paciente {}: {}", paciente.getId(), ex.getMessage());
            return ResultadoCreacion.fallo(ex.getMessage());
        }
    }
}

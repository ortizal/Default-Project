package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.bloqueo.BloqueoAgenda;
import org.dentalcrm.domain.bloqueo.BloqueoAgendaRepository;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.horario.ExcepcionHorario;
import org.dentalcrm.domain.horario.ExcepcionHorarioRepository;
import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AgendaService {

    private static final int INTERVALO_DEFECTO_MINUTOS = 30;

    private final HorarioOdontologoRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final BloqueoAgendaRepository bloqueoRepository;
    private final ServicioRepository servicioRepository;
    private final ExcepcionHorarioRepository excepcionRepository;
    private final ZoneId zonaCita;

    public AgendaService(HorarioOdontologoRepository horarioRepository,
                         CitaRepository citaRepository,
                         BloqueoAgendaRepository bloqueoRepository,
                         ServicioRepository servicioRepository,
                         ExcepcionHorarioRepository excepcionRepository,
                         @Value("${app.timezone}") String timezone) {
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.servicioRepository = servicioRepository;
        this.excepcionRepository = excepcionRepository;
        this.zonaCita = ZoneId.of(timezone);
    }

    /**
     * Excepción de atención del día, si la hay: {@link ExcepcionHorario#TIPO_CERRADO}
     * o {@link ExcepcionHorario#TIPO_HORARIO_ESPECIAL}. Vacío cuando el odontólogo
     * sigue su horario semanal de siempre.
     */
    @Transactional(readOnly = true)
    public Optional<ExcepcionHorario> excepcion(Long odontologoId, LocalDate fecha) {
        return excepcionRepository.findByOdontologoIdAndFecha(odontologoId, fecha);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> disponibilidad(Long odontologoId, LocalDate fecha, Long servicioId, Integer duracionPersonalizada) {
        int duracion = duracionPersonalizada != null ? duracionPersonalizada : duracionDeServicio(servicioId);
        List<Cita> citas = citaRepository.findByFechaAndDoctorIdOrderByHoraInicioAsc(fecha, odontologoId);
        List<BloqueoAgenda> bloqueos = bloqueoRepository.findByOdontologoIdAndFecha(odontologoId, fecha);
        LocalTime ahora = LocalTime.now(zonaCita);
        boolean esHoy = fecha.equals(LocalDate.now(zonaCita));

        Optional<ExcepcionHorario> excepcion = excepcionRepository.findByOdontologoIdAndFecha(odontologoId, fecha);
        if (excepcion.isPresent()) {
            ExcepcionHorario ex = excepcion.get();
            if (ex.esCerrado()) {
                return List.of();
            }
            return slotsDeVentana(ex.getHoraInicio(), ex.getHoraFin(), null, duracion,
                    citas, bloqueos, ahora, esHoy);
        }

        List<HorarioOdontologo> horarios = horarioRepository.findByOdontologoIdAndDiaSemanaAndEstado(
                odontologoId, fecha.getDayOfWeek().getValue(), "ACTIVO");
        if (horarios.isEmpty()) {
            return List.of();
        }

        List<SlotResponse> slots = new ArrayList<>();
        for (HorarioOdontologo horario : horarios) {
            slots.addAll(slotsDeVentana(horario.getHoraInicio(), horario.getHoraFin(), horario.getIntervaloMinutos(),
                    duracion, citas, bloqueos, ahora, esHoy));
        }
        return slots;
    }

    /**
     * Rellena los cupos que caben entre {@code ventanaInicio} y {@code ventanaFin},
     * descartando los que choquen con una cita activa o un bloqueo. Es la misma
     * cuenta para un horario semanal y para un día con horario especial.
     */
    private List<SlotResponse> slotsDeVentana(LocalTime ventanaInicio, LocalTime ventanaFin, Integer intervalo,
                                              int duracion, List<Cita> citas, List<BloqueoAgenda> bloqueos,
                                              LocalTime ahora, boolean esHoy) {
        List<SlotResponse> slots = new ArrayList<>();
        int paso = intervalo == null ? INTERVALO_DEFECTO_MINUTOS : Math.max(intervalo, 5);
        LocalTime inicio = ventanaInicio;
        while (!inicio.plusMinutes(duracion).isAfter(ventanaFin)) {
            LocalTime fin = inicio.plusMinutes(duracion);
            if (!esHoy || inicio.isAfter(ahora)) {
                if (estaLibre(inicio, fin, citas, bloqueos)) {
                    slots.add(new SlotResponse(inicio, fin));
                }
            }
            inicio = inicio.plusMinutes(paso);
        }
        return slots;
    }

    @Transactional(readOnly = true)
    public boolean estaLibre(Long doctorId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        Optional<ExcepcionHorario> excepcion = excepcionRepository.findByOdontologoIdAndFecha(doctorId, fecha);
        if (excepcion.isPresent() && !excepcion.get().admite(horaInicio, horaFin)) {
            return false;
        }
        List<Cita> citas = citaRepository.findByFechaAndDoctorIdOrderByHoraInicioAsc(fecha, doctorId);
        List<BloqueoAgenda> bloqueos = bloqueoRepository.findByOdontologoIdAndFecha(doctorId, fecha);
        return estaLibre(horaInicio, horaFin, citas, bloqueos);
    }

    private boolean estaLibre(LocalTime horaInicio, LocalTime horaFin,
                              List<Cita> citas, List<BloqueoAgenda> bloqueos) {
        for (Cita cita : citas) {
            if (cita.getEstado().esActiva()
                    && cita.getHoraInicio().isBefore(horaFin)
                    && cita.getHoraFin().isAfter(horaInicio)) {
                return false;
            }
        }
        for (BloqueoAgenda b : bloqueos) {
            if (bloquea(b, horaInicio, horaFin)) {
                return false;
            }
        }
        return true;
    }

    static boolean bloquea(BloqueoAgenda b, LocalTime horaInicio, LocalTime horaFin) {
        if (b.getHoraInicio() == null && b.getHoraFin() == null) {
            return true;
        }
        LocalTime bInicio = b.getHoraInicio() == null ? LocalTime.MIN : b.getHoraInicio();
        LocalTime bFin = b.getHoraFin() == null ? LocalTime.MAX : b.getHoraFin();
        return bInicio.isBefore(horaFin) && bFin.isAfter(horaInicio);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> citasDelDia(Long odontologoId, LocalDate fecha) {
        return citaRepository.findByFechaAndDoctorIdOrderByHoraInicioAsc(fecha, odontologoId).stream()
                .map(CitaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Cita> citasActivasDelDia(Long odontologoId, LocalDate fecha) {
        return citaRepository.findByFechaAndDoctorIdOrderByHoraInicioAsc(fecha, odontologoId).stream()
                .filter(c -> c.getEstado() == org.dentalcrm.domain.cita.EstadoCita.PENDIENTE
                        || c.getEstado() == org.dentalcrm.domain.cita.EstadoCita.CONFIRMADA)
                .toList();
    }

    /**
     * Tramos del día que no se pueden ofrecer: citas activas, bloqueos y, si el
     * día está cerrado, la propia excepción. Alimenta la columna "Ocupado" del
     * PDF de disponibilidad.
     */
    @Transactional(readOnly = true)
    public List<Ocupado> ocupados(Long odontologoId, LocalDate fecha) {
        List<Ocupado> tramos = new ArrayList<>();
        Optional<ExcepcionHorario> excepcion = excepcionRepository.findByOdontologoIdAndFecha(odontologoId, fecha);
        if (excepcion.isPresent() && excepcion.get().esCerrado()) {
            ExcepcionHorario ex = excepcion.get();
            String motivo = ex.getMotivo() == null || ex.getMotivo().isBlank() ? "Cerrado" : ex.getMotivo();
            tramos.add(new Ocupado(null, null, motivo));
        }
        for (Cita cita : citaRepository.findByFechaAndDoctorIdOrderByHoraInicioAsc(fecha, odontologoId)) {
            if (!cita.getEstado().esActiva()) {
                continue;
            }
            String motivo = cita.getServicio() == null ? "Cita" : cita.getServicio().getNombre();
            tramos.add(new Ocupado(cita.getHoraInicio(), cita.getHoraFin(), motivo));
        }
        for (BloqueoAgenda b : bloqueoRepository.findByOdontologoIdAndFecha(odontologoId, fecha)) {
            tramos.add(new Ocupado(b.getHoraInicio(), b.getHoraFin(),
                    b.getMotivo() == null || b.getMotivo().isBlank() ? "Bloqueo" : b.getMotivo()));
        }
        tramos.sort(Comparator.comparing(Ocupado::rango));
        return tramos;
    }

    /** Un tramo no disponible de la agenda de un odontólogo en una fecha. */
    public record Ocupado(LocalTime horaInicio, LocalTime horaFin, String motivo) {

        /** Rango legible: {@code 10:00–10:45}, {@code Todo el día} o {@code —}. */
        public String rango() {
            if (horaInicio == null && horaFin == null) {
                return "Todo el día";
            }
            return (horaInicio == null ? "—" : horaInicio.toString())
                    + "–" + (horaFin == null ? "—" : horaFin.toString());
        }

        public String descripcion() {
            return rango() + " " + motivo;
        }
    }

    private int duracionDeServicio(Long servicioId) {
        if (servicioId == null) {
            return 30;
        }
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Servicio no encontrado"));
        return servicio.getDuracionMinutos();
    }
}

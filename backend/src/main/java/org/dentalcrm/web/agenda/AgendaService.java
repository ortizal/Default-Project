package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.bloqueo.BloqueoAgenda;
import org.dentalcrm.domain.bloqueo.BloqueoAgendaRepository;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgendaService {

    private final HorarioOdontologoRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final BloqueoAgendaRepository bloqueoRepository;
    private final ServicioRepository servicioRepository;
    private final ZoneId zonaCita;

    public AgendaService(HorarioOdontologoRepository horarioRepository,
                         CitaRepository citaRepository,
                         BloqueoAgendaRepository bloqueoRepository,
                         ServicioRepository servicioRepository,
                         @Value("${app.timezone}") String timezone) {
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.servicioRepository = servicioRepository;
        this.zonaCita = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> disponibilidad(Long odontologoId, LocalDate fecha, Long servicioId, Integer duracionPersonalizada) {
        int duracion = duracionPersonalizada != null ? duracionPersonalizada : duracionDeServicio(servicioId);
        List<HorarioOdontologo> horarios = horarioRepository.findByOdontologoIdAndDiaSemanaAndEstado(
                odontologoId, fecha.getDayOfWeek().getValue(), "ACTIVO");
        if (horarios.isEmpty()) {
            return List.of();
        }

        List<Cita> citas = citaRepository.findByFechaAndDoctorIdOrderByHoraInicioAsc(fecha, odontologoId);
        List<BloqueoAgenda> bloqueos = bloqueoRepository.findByOdontologoIdAndFecha(odontologoId, fecha);

        LocalTime ahora = LocalTime.now(zonaCita);
        boolean esHoy = fecha.equals(LocalDate.now(zonaCita));

        List<SlotResponse> slots = new ArrayList<>();
        for (HorarioOdontologo horario : horarios) {
            LocalTime inicio = horario.getHoraInicio();
            while (!inicio.plusMinutes(duracion).isAfter(horario.getHoraFin())) {
                LocalTime fin = inicio.plusMinutes(duracion);
                if (!esHoy || inicio.isAfter(ahora)) {
                    if (estaLibre(inicio, fin, citas, bloqueos)) {
                        slots.add(new SlotResponse(inicio, fin));
                    }
                }
                inicio = inicio.plusMinutes(Math.max(horario.getIntervaloMinutos(), 5));
            }
        }
        return slots;
    }

    public boolean estaLibre(Long doctorId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
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

    private int duracionDeServicio(Long servicioId) {
        if (servicioId == null) {
            return 30;
        }
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Servicio no encontrado"));
        return servicio.getDuracionMinutos();
    }
}
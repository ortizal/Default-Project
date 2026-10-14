package org.dentalcrm.web.agente.tool;

import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.agenda.HorarioPdfService;
import org.dentalcrm.web.agenda.dto.SlotResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Disponibilidad real del backend (§8 y §16): nunca se muestran horas
 * teóricas, sólo cupos devueltos por {@link AgendaService}.
 */
public class AvailabilityTool {

    /** Cupo con el odontólogo al que pertenece (para "cualquier odontólogo"). */
    public record SlotConDoctor(LocalTime hora, Long doctorId, String doctorNombre) {
    }

    private final AgendaService agendaService;
    private final HorarioOdontologoRepository horarioRepository;
    private final HorarioPdfService horarioPdfService;
    private final DentistTool dentistaTool;

    public AvailabilityTool(AgendaService agendaService,
                            HorarioOdontologoRepository horarioRepository,
                            HorarioPdfService horarioPdfService,
                            DentistTool dentistaTool) {
        this.agendaService = agendaService;
        this.horarioRepository = horarioRepository;
        this.horarioPdfService = horarioPdfService;
        this.dentistaTool = dentistaTool;
    }

    public List<SlotResponse> slots(Long doctorId, LocalDate fecha, Long servicioId) {
        if (doctorId == null || fecha == null) {
            return List.of();
        }
        return agendaService.disponibilidad(doctorId, fecha, servicioId, null);
    }

    public List<LocalTime> horas(List<SlotResponse> slots) {
        return slots.stream().map(SlotResponse::horaInicio).toList();
    }

    public boolean contiene(List<SlotResponse> slots, LocalTime hora) {
        return slots.stream().map(SlotResponse::horaInicio).anyMatch(hora::equals);
    }

    /** Cupos de todos los odontólogos activos, ordenados por hora (§8). */
    public List<SlotConDoctor> slotsCualquierOdontologo(LocalDate fecha, Long servicioId) {
        List<SlotConDoctor> resultado = new ArrayList<>();
        for (Odontologo o : dentistaTool.activos()) {
            for (SlotResponse s : slots(o.getId(), fecha, servicioId)) {
                resultado.add(new SlotConDoctor(s.horaInicio(), o.getId(), dentistaTool.nombreCompleto(o)));
            }
        }
        resultado.sort(Comparator.comparing(SlotConDoctor::hora).thenComparing(SlotConDoctor::doctorNombre));
        return resultado;
    }

    public List<SlotResponse> filtrarFranja(List<SlotResponse> slots, String franja) {
        if (franja == null || "TODAS".equals(franja)) {
            return slots;
        }
        return slots.stream().filter(s -> enFranja(s.horaInicio(), franja)).toList();
    }

    public boolean enFranja(LocalTime hora, String franja) {
        if (franja == null || "TODAS".equals(franja)) {
            return true;
        }
        return switch (franja) {
            case "MANANA" -> hora.isBefore(LocalTime.of(12, 0));
            case "MEDIODIA" -> !hora.isBefore(LocalTime.of(12, 0)) && hora.isBefore(LocalTime.of(15, 0));
            case "TARDE" -> !hora.isBefore(LocalTime.of(15, 0));
            default -> true;
        };
    }

    public List<String> proximosCupos(Long doctorId, Long servicioId) {
        List<String> cupos = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (int offset = 1; offset <= 14 && cupos.size() < 8; offset++) {
            LocalDate f = hoy.plusDays(offset);
            List<SlotResponse> lista = slots(doctorId, f, servicioId);
            if (!lista.isEmpty()) {
                cupos.add(f.toString() + " a las " + lista.get(0).horaInicio());
            }
        }
        return cupos;
    }

    public List<HorarioPdfService.DiaDisponibilidad> disponibilidadPorDia(Long doctorId, Long servicioId) {
        List<HorarioPdfService.DiaDisponibilidad> dias = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (int offset = 1; offset <= 7; offset++) {
            LocalDate fecha = hoy.plusDays(offset);
            String libre = slots(doctorId, fecha, servicioId).stream()
                    .map(s -> s.horaInicio().toString())
                    .collect(java.util.stream.Collectors.joining(", "));
            String ocupado = agendaService.ocupados(doctorId, fecha).stream()
                    .map(AgendaService.Ocupado::descripcion)
                    .collect(java.util.stream.Collectors.joining("\n"));
            dias.add(new HorarioPdfService.DiaDisponibilidad(fecha.toString(),
                    libre.isEmpty() ? "—" : libre,
                    ocupado.isEmpty() ? "—" : ocupado));
        }
        return dias;
    }

    public List<HorarioOdontologo> horariosActivos(Long doctorId) {
        return horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(doctorId).stream()
                .filter(h -> "ACTIVO".equals(h.getEstado()))
                .toList();
    }

    public byte[] pdf(Odontologo doctor, List<HorarioOdontologo> horarios,
                      List<String> cupos, List<HorarioPdfService.DiaDisponibilidad> dias) {
        try {
            return horarioPdfService.generar(doctor, horarios, cupos, dias);
        } catch (Exception ex) {
            return null;
        }
    }

    public String nombreArchivo(Odontologo doctor) {
        return horarioPdfService.nombreArchivo(doctor);
    }
}

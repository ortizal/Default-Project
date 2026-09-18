package org.dentalcrm.web.horario.dto;

import org.dentalcrm.domain.horario.HorarioOdontologo;

import java.time.Instant;
import java.time.LocalTime;

public record HorarioResponse(
        Long id,
        Long odontologoId,
        String odontologoNombre,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer intervaloMinutos,
        String estado,
        Instant createdAt,
        Instant updatedAt
) {
    public static HorarioResponse from(HorarioOdontologo h) {
        return new HorarioResponse(
                h.getId(),
                h.getOdontologo().getId(),
                h.getOdontologo().getNombres() + " " + h.getOdontologo().getApellidos(),
                h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin(),
                h.getIntervaloMinutos(), h.getEstado(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
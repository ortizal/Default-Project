package org.dentalcrm.web.agenda.dto;

import org.dentalcrm.domain.bloqueo.BloqueoAgenda;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueoResponse(
        Long id,
        Long odontologoId,
        String odontologoNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo,
        Instant createdAt
) {
    public static BloqueoResponse from(BloqueoAgenda b) {
        return new BloqueoResponse(
                b.getId(), b.getOdontologo().getId(),
                b.getOdontologo().getNombres() + " " + b.getOdontologo().getApellidos(),
                b.getFecha(), b.getHoraInicio(), b.getHoraFin(), b.getMotivo(), b.getCreatedAt());
    }
}
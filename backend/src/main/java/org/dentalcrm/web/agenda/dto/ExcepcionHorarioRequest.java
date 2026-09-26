package org.dentalcrm.web.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExcepcionHorarioRequest(
        @NotNull(message = "El odontólogo es obligatorio") Long odontologoId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotBlank(message = "El tipo es obligatorio") String tipo,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo
) {
}

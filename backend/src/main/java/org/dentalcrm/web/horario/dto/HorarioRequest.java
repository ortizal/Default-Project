package org.dentalcrm.web.horario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

public record HorarioRequest(
        @NotNull(message = "El odontólogo es obligatorio") Long odontologoId,
        @NotNull(message = "El día de semana es obligatorio")
        @Min(value = 1, message = "Día debe estar entre 1 (lunes) y 7 (domingo)")
        @Max(value = 7, message = "Día debe estar entre 1 (lunes) y 7 (domingo)")
        Integer diaSemana,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
        @Min(value = 5, message = "El intervalo mínimo es 5 minutos") Integer intervaloMinutos,
        @Pattern(regexp = "^(ACTIVO|INACTIVO)$", message = "Estado debe ser ACTIVO o INACTIVO")
        String estado
) {}
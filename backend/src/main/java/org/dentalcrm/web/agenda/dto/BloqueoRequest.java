package org.dentalcrm.web.agenda.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueoRequest(
        @NotNull(message = "El odontólogo es obligatorio") Long odontologoId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo
) {

    public boolean esDiaCompleto() {
        return horaInicio == null && horaFin == null;
    }
}
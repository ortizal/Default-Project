package org.dentalcrm.web.cita.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaRequest(
        @NotNull(message = "El paciente es obligatorio") Long pacienteId,
        @NotNull(message = "El odontólogo es obligatorio") Long doctorId,
        @NotNull(message = "El servicio es obligatorio") Long servicioId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        String observaciones
) {}
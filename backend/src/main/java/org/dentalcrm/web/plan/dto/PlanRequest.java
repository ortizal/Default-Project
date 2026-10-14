package org.dentalcrm.web.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlanRequest(
        @NotBlank @Size(max = 150) String titulo,
        Long odontologoId,
        @Size(max = 4000) String observaciones,
        LocalDate fecha
) {
}

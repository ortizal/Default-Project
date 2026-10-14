package org.dentalcrm.web.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.plan.EstadoPlanTratamiento;

public record EstadoPlanRequest(
        @NotNull EstadoPlanTratamiento estado,
        @Size(max = 255) String comentario
) {
}

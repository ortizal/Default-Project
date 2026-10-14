package org.dentalcrm.web.plan.dto;

import jakarta.validation.constraints.Size;

public record PresupuestoRequest(
        @Size(max = 255) String observaciones
) {
}

package org.dentalcrm.web.plan.dto;

import jakarta.validation.constraints.NotNull;
import org.dentalcrm.domain.plan.EstadoLineaPlan;

public record EstadoLineaRequest(
        @NotNull EstadoLineaPlan estado
) {
}

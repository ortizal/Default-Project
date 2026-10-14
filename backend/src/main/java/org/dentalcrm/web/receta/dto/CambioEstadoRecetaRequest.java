package org.dentalcrm.web.receta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dentalcrm.domain.receta.EstadoReceta;

public record CambioEstadoRecetaRequest(
        @NotNull EstadoReceta estado,
        @NotBlank String motivo
) {
}

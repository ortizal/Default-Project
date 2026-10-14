package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.Size;

/** Anulación de un movimiento del libro (conserva la traza). */
public record AnularMovimientoRequest(
        @Size(max = 255) String motivo
) {
}

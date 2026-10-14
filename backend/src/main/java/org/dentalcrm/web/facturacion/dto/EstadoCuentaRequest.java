package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.NotNull;
import org.dentalcrm.domain.cuenta.EstadoCuenta;

/**
 * Fijar o reabrir el estado manual de la cuenta (plan §3.3):
 * CERRADA, ANULADA o reabrirla con ABIERTA.
 */
public record EstadoCuentaRequest(
        @NotNull EstadoCuenta estado,
        @jakarta.validation.constraints.Size(max = 255) String motivo
) {
}

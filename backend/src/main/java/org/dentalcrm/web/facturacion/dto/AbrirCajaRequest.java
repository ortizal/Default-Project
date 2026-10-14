package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Apertura de una sesión de caja (§3.4). El fondo inicial es efectivo. */
public record AbrirCajaRequest(
        @Size(max = 80) String nombre,
        BigDecimal montoApertura,
        @Size(max = 500) String observaciones
) {
}

package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Cierre de caja: efectivo contado en la gaveta. La diferencia se
 * calcula contra el esperado (apertura + ingresos − egresos en
 * efectivo).
 */
public record CerrarCajaRequest(
        @NotNull BigDecimal montoDeclarado,
        @jakarta.validation.constraints.Size(max = 500) String observaciones
) {
}

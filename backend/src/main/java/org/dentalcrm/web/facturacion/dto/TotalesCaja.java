package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corte de una sesión de caja (§3.4).
 *
 * <p>El fondo inicial es efectivo; {@code esperadoEfectivo} = apertura
 * + ingresos − egresos cobrados en efectivo, y
 * {@code diferencia = declarado − esperadoEfectivo}.
 */
public record TotalesCaja(
        BigDecimal apertura,
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal declarado,
        BigDecimal esperadoEfectivo,
        BigDecimal diferencia,
        List<FormaPagoTotal> porForma,
        int movimientos
) {
}

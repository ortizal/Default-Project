package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.domain.cuenta.TipoMovimientoCuenta;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de un movimiento en la cuenta del paciente (plan §3.3).
 *
 * <p>Los campos que se usan dependen del tipo:
 * <ul>
 *   <li>{@code CARGO}: cantidad × precioUnitario × (1 − descuentoPct/100)</li>
 *   <li>{@code DESCUENTO, ABONO, ANTICIPO, DEVOLUCION}: monto en {@code total}</li>
 * </ul>
 * Abonos, anticipos y devoluciones exigen {@code formaPago} (§3.4).
 */
public record NuevoMovimientoCuentaRequest(
        @NotNull TipoMovimientoCuenta tipo,
        @NotBlank @Size(max = 255) String concepto,
        Integer diente,
        @Size(max = 20) String superficie,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuentoPct,
        BigDecimal total,
        FormaPago formaPago,
        LocalDate fecha,
        @Size(max = 255) String observaciones
) {
}

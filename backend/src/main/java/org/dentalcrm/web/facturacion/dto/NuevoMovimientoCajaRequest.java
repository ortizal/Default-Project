package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.caja.TipoMovimientoCaja;
import org.dentalcrm.domain.cuenta.FormaPago;

import java.math.BigDecimal;

/**
 * Ingreso o egreso manual de caja (§3.4). Los cobros de pacientes se
 * generan solos desde la cuenta; este endpoint cubre movimientos fuera
 * de ella (insumos, alquiler, retiros de fondo, etc.).
 */
public record NuevoMovimientoCajaRequest(
        @NotNull TipoMovimientoCaja tipo,
        @NotBlank @Size(max = 255) String concepto,
        @NotNull FormaPago formaPago,
        @NotNull BigDecimal monto,
        Long pacienteId
) {
}

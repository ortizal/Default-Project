package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.util.List;

/** Saldos por cobrar con el detalle de cada cuenta (plan §3.13). */
public record ReporteSaldosResponse(
        BigDecimal totalPorCobrar,
        int cantidadCuentas,
        List<CuentaResumenResponse> cuentas
) {
}

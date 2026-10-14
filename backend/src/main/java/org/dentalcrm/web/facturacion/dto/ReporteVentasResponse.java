package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ventas por rango (plan §3.13): facturado y cobrado por
 * día, por forma de pago y por servicio.
 */
public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal facturado,
        BigDecimal cobrado,
        BigDecimal descuentos,
        BigDecimal devoluciones,
        int cargos,
        int cobros,
        List<FilaDiaria> porDia,
        List<FormaPagoTotal> porFormaPago,
        List<TotalPorServicio> porServicio
) {
    public record FilaDiaria(LocalDate fecha, BigDecimal facturado, BigDecimal cobrado,
                             int movimientos) {
    }
}

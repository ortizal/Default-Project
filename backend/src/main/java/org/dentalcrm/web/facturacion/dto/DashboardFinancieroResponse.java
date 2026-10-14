package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard financiero (plan §3.12), sin datos SRI: facturado/cobrado
 * del rango, de hoy y del mes, cuentas por cobrar, anticipos, ticket
 * promedio y desgloses por forma de pago, odontólogo y servicio.
 *
 * <p>Antes de existir comprobantes electrónicos, "facturado" son los
 * cargos de las cuentas y "cobrado" el dinero recibido (abonos +
 * anticipos − devoluciones).
 */
public record DashboardFinancieroResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal facturado,
        BigDecimal cobrado,
        BigDecimal descuentos,
        BigDecimal devoluciones,
        BigDecimal facturadoHoy,
        BigDecimal cobradoHoy,
        BigDecimal facturadoMes,
        BigDecimal cobradoMes,
        BigDecimal cuentasPorCobrar,
        int cuentasPendientes,
        BigDecimal anticipos,
        int cobros,
        BigDecimal ticketPromedio,
        List<FormaPagoTotal> porFormaPago,
        List<TotalPorOdontologo> porOdontologo,
        List<TotalPorServicio> porServicio
) {
}

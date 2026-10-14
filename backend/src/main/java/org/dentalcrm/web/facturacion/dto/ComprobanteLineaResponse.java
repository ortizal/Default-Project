package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.web.facturacion.sri.RideDatos;

import java.math.BigDecimal;

/**
 * Una línea del detalle del comprobante, leída del XML autorizado
 * (plan §3.9): es lo que la bandeja ofrece para elegir qué devuelve
 * una nota de crédito parcial.
 *
 * @param linea número de línea del comprobante, empezando en 1, que es
 *              el identificador que espera {@code EmitirNotaCreditoRequest}
 * @param total importe de la línea sin impuestos ({@code precioTotalSinImpuesto})
 * @param impuesto IVA desglosado de la línea; con él, {@code total + impuesto}
 *                 es lo que el paciente vio cobrado en la cuenta
 */
public record ComprobanteLineaResponse(
        Long linea,
        String codigo,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal total,
        BigDecimal impuesto
) {

    public static ComprobanteLineaResponse de(long indice, RideDatos.Linea l) {
        return new ComprobanteLineaResponse(indice, l.codigo(), l.descripcion(),
                l.cantidad(), l.precioUnitario(), l.descuento(), l.total(), l.impuesto());
    }
}

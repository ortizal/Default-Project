package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.domain.cuenta.CuentaDetalle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Movimiento del libro mayor de la cuenta. */
public record MovimientoCuentaResponse(
        Long id,
        Long cuentaId,
        String tipo,
        String origen,
        Long referenciaId,
        String concepto,
        Integer diente,
        String superficie,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuentoPct,
        BigDecimal total,
        String formaPago,
        LocalDate fecha,
        boolean anulado,
        String anuladoMotivo,
        String observaciones,
        Long creadoPor,
        Instant createdAt
) {
    public static MovimientoCuentaResponse from(CuentaDetalle d) {
        return new MovimientoCuentaResponse(d.getId(), d.getCuenta().getId(),
                d.getTipo().name(), d.getOrigen().name(), d.getReferenciaId(),
                d.getConcepto(), d.getDiente(), d.getSuperficie(), d.getCantidad(),
                d.getPrecioUnitario(), d.getDescuentoPct(), d.getTotal(),
                d.getFormaPago() == null ? null : d.getFormaPago().name(),
                d.getFecha(), d.isAnulado(), d.getAnuladoMotivo(),
                d.getObservaciones(), d.getCreatedBy(), d.getCreatedAt());
    }
}

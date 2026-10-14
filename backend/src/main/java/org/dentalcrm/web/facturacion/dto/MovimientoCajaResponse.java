package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.domain.caja.MovimientoCaja;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Movimiento de una sesión de caja. */
public record MovimientoCajaResponse(
        Long id,
        Long cajaId,
        String tipo,
        String concepto,
        String formaPago,
        BigDecimal monto,
        Long cuentaDetalleId,
        Long pacienteId,
        String paciente,
        LocalDate fecha,
        boolean anulado,
        String anuladoMotivo,
        Long creadoPor,
        Instant createdAt
) {
    public static MovimientoCajaResponse from(MovimientoCaja m, String paciente) {
        return new MovimientoCajaResponse(m.getId(), m.getCaja().getId(), m.getTipo().name(),
                m.getConcepto(), m.getFormaPago().name(), m.getMonto(), m.getCuentaDetalleId(),
                m.getPaciente() == null ? null : m.getPaciente().getId(), paciente,
                m.getFecha(), m.isAnulado(), m.getAnuladoMotivo(), m.getCreatedBy(),
                m.getCreatedAt());
    }
}

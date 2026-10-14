package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.domain.cuenta.CuentaDetalle;
import org.dentalcrm.domain.cuenta.CuentaPaciente;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Cuenta del paciente con sus totales denormalizados y el libro de
 * movimientos (§3.3).
 *
 * <p>{@code saldo = totalCargos − totalDescuentos − totalAbonos +
 * devoluciones}; aquí sólo se exponen los totales ya calculados.
 */
public record CuentaPacienteResponse(
        Long id,
        Long pacienteId,
        String paciente,
        String cedula,
        String estado,
        BigDecimal saldo,
        BigDecimal totalCargos,
        BigDecimal totalDescuentos,
        BigDecimal totalAbonos,
        int cantidadMovimientos,
        List<MovimientoCuentaResponse> movimientos,
        Instant createdAt,
        Instant updatedAt
) {
    public static CuentaPacienteResponse from(CuentaPaciente c, String paciente,
                                              String cedula, List<CuentaDetalle> movimientos) {
        return new CuentaPacienteResponse(c.getId(), c.getPaciente().getId(), paciente, cedula,
                c.getEstado().name(), c.getSaldo(), c.getTotalCargos(), c.getTotalDescuentos(),
                c.getTotalAbonos(),
                (int) movimientos.stream().filter(m -> !m.isAnulado()).count(),
                movimientos.stream().map(MovimientoCuentaResponse::from).toList(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}

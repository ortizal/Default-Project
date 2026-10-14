package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.domain.cuenta.CuentaPaciente;

import java.math.BigDecimal;
import java.time.Instant;

/** Fila de listado de cuentas (saldo por paciente). */
public record CuentaResumenResponse(
        Long id,
        Long pacienteId,
        String paciente,
        String cedula,
        String estado,
        BigDecimal saldo,
        BigDecimal totalCargos,
        BigDecimal totalAbonos,
        int cantidadMovimientos,
        Instant updatedAt
) {
    public static CuentaResumenResponse from(CuentaPaciente c, String paciente, String cedula,
                                             int cantidadMovimientos) {
        return new CuentaResumenResponse(c.getId(), c.getPaciente().getId(), paciente, cedula,
                c.getEstado().name(), c.getSaldo(), c.getTotalCargos(), c.getTotalAbonos(),
                cantidadMovimientos, c.getUpdatedAt());
    }
}

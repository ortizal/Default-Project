package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Sesión de caja con su corte y sus movimientos. */
public record CajaResponse(
        Long id,
        String nombre,
        String estado,
        LocalDate fecha,
        BigDecimal montoApertura,
        BigDecimal montoDeclarado,
        BigDecimal diferencia,
        String responsable,
        String abiertoPor,
        Instant abiertoEn,
        String cerradoPor,
        Instant cerradoEn,
        String observacionesApertura,
        String observacionesCierre,
        TotalesCaja totales,
        List<MovimientoCajaResponse> movimientos,
        Instant createdAt,
        Instant updatedAt
) {
}

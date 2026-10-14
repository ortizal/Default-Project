package org.dentalcrm.web.plan.dto;

import org.dentalcrm.domain.plan.Presupuesto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PresupuestoResumenResponse(
        Long id,
        int numero,
        String estado,
        LocalDate fecha,
        boolean vigente,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        String firmante,
        String firmaHash,
        Instant aceptadoEn,
        String observaciones
) {
    public static PresupuestoResumenResponse from(Presupuesto p) {
        return new PresupuestoResumenResponse(
                p.getId(), p.getNumero(), p.getEstado().name(), p.getFecha(),
                Boolean.TRUE.equals(p.getVigente()), p.getSubtotal(), p.getDescuento(),
                p.getTotal(), p.getFirmante(), p.getFirmaHash(), p.getAceptadoEn(),
                p.getObservaciones());
    }
}

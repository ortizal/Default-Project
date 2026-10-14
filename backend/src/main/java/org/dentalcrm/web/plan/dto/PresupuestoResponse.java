package org.dentalcrm.web.plan.dto;

import org.dentalcrm.domain.plan.Presupuesto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PresupuestoResponse(
        Long id,
        Long planId,
        int numero,
        String estado,
        LocalDate fecha,
        boolean vigente,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        String observaciones,
        String comentario,
        String firmante,
        String firmaHash,
        Instant aceptadoEn,
        List<LineaPresupuestoResponse> lineas,
        Instant createdAt
) {
    public static PresupuestoResponse from(Presupuesto p, List<LineaPresupuestoResponse> lineas) {
        return new PresupuestoResponse(
                p.getId(), p.getPlan().getId(), p.getNumero(), p.getEstado().name(), p.getFecha(),
                Boolean.TRUE.equals(p.getVigente()), p.getSubtotal(), p.getDescuento(),
                p.getTotal(), p.getObservaciones(), p.getComentario(), p.getFirmante(),
                p.getFirmaHash(), p.getAceptadoEn(), lineas, p.getCreatedAt());
    }
}

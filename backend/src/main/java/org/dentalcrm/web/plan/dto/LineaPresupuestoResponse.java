package org.dentalcrm.web.plan.dto;

import org.dentalcrm.domain.plan.PresupuestoLinea;

import java.math.BigDecimal;

public record LineaPresupuestoResponse(
        Long id,
        Long planLineaId,
        int numero,
        int fase,
        Integer diente,
        String superficie,
        String diagnostico,
        String procedimiento,
        String prioridad,
        int sesiones,
        BigDecimal precioUnitario,
        BigDecimal descuentoPct,
        BigDecimal subtotal
) {
    public static LineaPresupuestoResponse from(PresupuestoLinea l) {
        return new LineaPresupuestoResponse(
                l.getId(), l.getPlanLinea() == null ? null : l.getPlanLinea().getId(),
                l.getNumero(), l.getFase(), l.getDiente(),
                l.getSuperficie() == null ? null : l.getSuperficie(),
                l.getDiagnostico(), l.getProcedimiento(),
                l.getPrioridad() == null ? null : l.getPrioridad().name(),
                l.getSesiones(), l.getPrecioUnitario(), l.getDescuentoPct(), l.getSubtotal());
    }
}

package org.dentalcrm.web.plan.dto;

import org.dentalcrm.domain.plan.EstadoLineaPlan;
import org.dentalcrm.domain.plan.PlanLinea;

import java.math.BigDecimal;
import java.time.Instant;

public record LineaResponse(
        Long id,
        int numero,
        int fase,
        Integer diente,
        String superficie,
        String diagnostico,
        String procedimiento,
        Long servicioId,
        String servicio,
        String prioridad,
        int sesiones,
        BigDecimal precioUnitario,
        BigDecimal descuentoPct,
        BigDecimal subtotal,
        String estado,
        String notas,
        Instant createdAt,
        Instant updatedAt
) {
    public static LineaResponse from(PlanLinea l) {
        return from(l, l.getServicio() == null ? null : l.getServicio().getNombre());
    }

    public static LineaResponse from(PlanLinea l, String servicio) {
        EstadoLineaPlan estado = l.getEstado();
        return new LineaResponse(
                l.getId(), l.getNumero(), l.getFase(), l.getDiente(),
                l.getSuperficie(),
                l.getDiagnostico(), l.getProcedimiento(),
                l.getServicio() == null ? null : l.getServicio().getId(), servicio,
                l.getPrioridad().name(), l.getSesiones(),
                l.getPrecioUnitario(), l.getDescuentoPct(), l.getSubtotal(),
                estado == null ? null : estado.name(),
                l.getNotas(), l.getCreatedAt(), l.getUpdatedAt());
    }
}

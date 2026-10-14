package org.dentalcrm.web.plan.dto;

import org.dentalcrm.domain.plan.PlanTratamiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanResumenResponse(
        Long id,
        Long pacienteId,
        String titulo,
        String estado,
        LocalDate fecha,
        BigDecimal subtotal,
        BigDecimal total,
        int lineas,
        int presupuestos,
        String odontologo
) {
    public static PlanResumenResponse from(PlanTratamiento plan, int lineas, int presupuestos,
                                           String odontologo) {
        return new PlanResumenResponse(
                plan.getId(), plan.getPaciente().getId(), plan.getTitulo(),
                plan.getEstado().name(), plan.getFecha(), plan.getSubtotal(),
                plan.getTotal(), lineas, presupuestos, odontologo);
    }
}

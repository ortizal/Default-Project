package org.dentalcrm.web.plan.dto;

import org.dentalcrm.domain.plan.PlanTratamiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record PlanResponse(
        Long id,
        Long pacienteId,
        String paciente,
        Long odontologoId,
        String odontologo,
        String titulo,
        String estado,
        String observaciones,
        LocalDate fecha,
        BigDecimal subtotal,
        BigDecimal total,
        String comentario,
        List<LineaResponse> lineas,
        List<PresupuestoResumenResponse> presupuestos,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlanResponse from(PlanTratamiento plan, String paciente, String odontologo,
                                    List<LineaResponse> lineas,
                                    List<PresupuestoResumenResponse> presupuestos) {
        return new PlanResponse(
                plan.getId(), plan.getPaciente().getId(), paciente,
                plan.getOdontologo() == null ? null : plan.getOdontologo().getId(), odontologo,
                plan.getTitulo(), plan.getEstado().name(), plan.getObservaciones(),
                plan.getFecha(), plan.getSubtotal(), plan.getTotal(), plan.getComentario(),
                lineas, presupuestos, plan.getCreatedAt(), plan.getUpdatedAt());
    }
}

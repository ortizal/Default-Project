package org.dentalcrm.web.automatizacion.dto;

import org.dentalcrm.domain.automatizacion.Automatizacion;

import java.time.Instant;

public record AutomatizacionResponse(
        Long id,
        String nombre,
        String evento,
        Integer minutosAntes,
        Long plantillaId,
        String plantillaNombre,
        String condicion,
        Boolean activa,
        Instant createdAt,
        Instant updatedAt
) {
    public static AutomatizacionResponse from(Automatizacion a) {
        return new AutomatizacionResponse(
                a.getId(), a.getNombre(), a.getEvento().name(), a.getMinutosAntes(),
                a.getPlantilla().getId(), a.getPlantilla().getNombre(),
                a.getCondicion(), a.getActiva(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
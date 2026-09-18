package org.dentalcrm.web.automatizacion.dto;

import org.dentalcrm.domain.automatizacion.PlantillaMensaje;

import java.time.Instant;

public record PlantillaResponse(
        Long id,
        String nombre,
        String contenido,
        Boolean activa,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlantillaResponse from(PlantillaMensaje p) {
        return new PlantillaResponse(
                p.getId(), p.getNombre(), p.getContenido(), p.getActiva(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
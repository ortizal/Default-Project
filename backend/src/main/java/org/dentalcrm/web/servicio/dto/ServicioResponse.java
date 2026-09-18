package org.dentalcrm.web.servicio.dto;

import org.dentalcrm.domain.servicio.Servicio;

import java.math.BigDecimal;
import java.time.Instant;

public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        BigDecimal precio,
        String estado,
        Instant createdAt,
        Instant updatedAt
) {
    public static ServicioResponse from(Servicio s) {
        return new ServicioResponse(
                s.getId(), s.getNombre(), s.getDescripcion(), s.getDuracionMinutos(),
                s.getPrecio(), s.getEstado(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
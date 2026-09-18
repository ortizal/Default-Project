package org.dentalcrm.web.odontologo.dto;

import org.dentalcrm.domain.odontologo.Odontologo;

import java.time.Instant;

public record OdontologoResponse(
        Long id,
        String nombres,
        String apellidos,
        String especialidad,
        String telefono,
        String email,
        String etiquetas,
        String googleCalendarId,
        String estado,
        Instant createdAt,
        Instant updatedAt
) {
    public static OdontologoResponse from(Odontologo o) {
        return new OdontologoResponse(
                o.getId(), o.getNombres(), o.getApellidos(), o.getEspecialidad(),
                o.getTelefono(), o.getEmail(), o.getEtiquetas(), o.getGoogleCalendarId(),
                o.getEstado(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
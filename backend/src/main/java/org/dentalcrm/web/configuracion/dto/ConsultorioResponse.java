package org.dentalcrm.web.configuracion.dto;

import org.dentalcrm.domain.configuracion.Consultorio;

import java.time.Instant;

public record ConsultorioResponse(
        String nombre,
        String direccion,
        String telefono,
        String enlaceUbicacion,
        String horarioAtencion,
        Instant updatedAt
) {
    public static ConsultorioResponse from(Consultorio consultorio) {
        return new ConsultorioResponse(consultorio.getNombre(), consultorio.getDireccion(),
                consultorio.getTelefono(), consultorio.getEnlaceUbicacion(),
                consultorio.getHorarioAtencion(), consultorio.getUpdatedAt());
    }
}
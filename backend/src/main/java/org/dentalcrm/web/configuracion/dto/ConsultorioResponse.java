package org.dentalcrm.web.configuracion.dto;

import org.dentalcrm.domain.configuracion.Consultorio;

import java.time.Instant;

public record ConsultorioResponse(
        String nombre,
    String razonSocial,
    String ruc,
    String correoElectronico,
        String direccion,
        String telefono,
        String enlaceUbicacion,
        String horarioAtencion,
    String firmaDigitalNombre,
    boolean firmaDigitalCargada,
        Instant updatedAt
) {
    public static ConsultorioResponse from(Consultorio consultorio) {
    return new ConsultorioResponse(consultorio.getNombre(), consultorio.getRazonSocial(),
        consultorio.getRuc(), consultorio.getCorreoElectronico(), consultorio.getDireccion(),
        consultorio.getTelefono(), consultorio.getEnlaceUbicacion(),
        consultorio.getHorarioAtencion(), consultorio.getFirmaDigitalNombre(),
        consultorio.getFirmaDigital() != null && consultorio.getFirmaDigital().length > 0,
        consultorio.getUpdatedAt());
    }
}
package org.dentalcrm.web.automatizacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlantillaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100) String nombre,
        @NotBlank(message = "El contenido es obligatorio") String contenido,
        Boolean activa
) {}
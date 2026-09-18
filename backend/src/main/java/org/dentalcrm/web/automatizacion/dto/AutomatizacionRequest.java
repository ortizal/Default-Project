package org.dentalcrm.web.automatizacion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AutomatizacionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150) String nombre,
        @NotBlank(message = "El evento es obligatorio") String evento,
        @NotNull(message = "minutos_antes es obligatorio")
        @Min(value = 0, message = "minutos_antes debe ser mayor o igual a 0") Integer minutosAntes,
        @NotNull(message = "Debe indicar la plantilla") Long plantillaId,
        @Size(max = 255) String condicion,
        Boolean activa
) {}
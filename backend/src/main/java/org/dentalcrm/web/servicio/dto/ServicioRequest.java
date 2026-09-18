package org.dentalcrm.web.servicio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull(message = "La duración es obligatoria")
        @Min(value = 1, message = "La duración debe ser mayor que 0")
        Integer duracionMinutos,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal precio,
        @Pattern(regexp = "^(ACTIVO|INACTIVO)$", message = "Estado debe ser ACTIVO o INACTIVO")
        String estado
) {}
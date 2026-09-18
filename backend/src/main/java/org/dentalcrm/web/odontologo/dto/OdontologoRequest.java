package org.dentalcrm.web.odontologo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OdontologoRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150) String apellidos,
        @Size(max = 100) String especialidad,
        @Size(max = 30) String telefono,
        @Email(message = "Email inválido")
        @Size(max = 190) String email,
        @Size(max = 255) String etiquetas,
        @Size(max = 255) String googleCalendarId,
        @Pattern(regexp = "^(ACTIVO|INACTIVO)$", message = "Estado debe ser ACTIVO o INACTIVO")
        String estado
) {}
package org.dentalcrm.web.paciente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TutorRequest(
        @NotBlank(message = "El parentesco es obligatorio")
        @Pattern(regexp = "PADRE|MADRE", message = "Parentesco debe ser PADRE o MADRE")
        String parentesco,
        @NotBlank(message = "Los nombres del tutor son obligatorios")
        @Size(max = 150) String nombres,
        @Size(max = 150) String apellidos,
        @Size(max = 30) String telefono
) {}
package org.dentalcrm.web.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioUpdateRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        @Size(max = 190) String email,
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150) String apellidos,
        @Size(max = 30) String telefono,
        @Pattern(regexp = "^(ACTIVO|INACTIVO)$", message = "Estado debe ser ACTIVO o INACTIVO")
        String estado,
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String nuevaPassword,
        Set<@NotBlank String> roles
) {}
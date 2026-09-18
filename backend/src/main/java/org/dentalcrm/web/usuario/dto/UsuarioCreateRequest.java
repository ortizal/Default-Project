package org.dentalcrm.web.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioCreateRequest(
        @NotBlank(message = "El username es obligatorio")
        @Size(max = 100) String username,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        @Size(max = 190) String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String password,
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150) String apellidos,
        @Size(max = 30) String telefono,
        @NotEmpty(message = "Debe asignar al menos un rol")
        Set<@NotBlank String> roles
) {}
package org.dentalcrm.web.paciente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PacienteRequest(
        @Size(max = 20) String cedula,
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150) String apellidos,
        @Size(max = 30) String telefono,
        @Email(message = "Email inválido")
        @Size(max = 190) String email,
        LocalDate fechaNacimiento,
        @Size(max = 255) String direccion,
        String observaciones,
        @Pattern(regexp = "^(ACTIVO|INACTIVO)$", message = "Estado debe ser ACTIVO o INACTIVO")
        String estado,
        @Valid
        List<TutorRequest> tutores
) {}
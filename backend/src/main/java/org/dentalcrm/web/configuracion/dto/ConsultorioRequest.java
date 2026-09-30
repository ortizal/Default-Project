package org.dentalcrm.web.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConsultorioRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 200) String razonSocial,
        @Pattern(regexp = "^$|^\\d{13}$", message = "El RUC debe contener 13 dígitos") String ruc,
        @Email @Size(max = 190) String correoElectronico,
        @Size(max = 500) String direccion,
        @Size(max = 30) String telefono,
        @Size(max = 1000) String enlaceUbicacion,
        @Size(max = 500) String horarioAtencion
) {
}
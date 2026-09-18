package org.dentalcrm.web.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatsAppSesionRequest(
        @NotBlank(message = "El identificador de sesión es obligatorio")
        @Size(max = 100) String sesionId,
        @Size(max = 150) String nombre
) {}
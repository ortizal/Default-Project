package org.dentalcrm.web.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequest(
        @NotBlank(message = "El texto del mensaje es obligatorio")
        @Size(max = 2000) String texto
) {}
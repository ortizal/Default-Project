package org.dentalcrm.web.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoConversacionRequest(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "BOT|ATENCION_HUMANA|ATENDIDA|CERRADA", message = "Estado inválido")
        String estado
) {}
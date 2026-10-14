package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.NotBlank;

public record AdendaRequest(
        @NotBlank(message = "El texto de la adenda es obligatorio") String texto
) {}

package org.dentalcrm.web.agente.dto;

public record ConversacionInicioResponse(
        String conversacionId,
        String link,
        String mensaje
) {
}

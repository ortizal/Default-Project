package org.dentalcrm.web.whatsapp.dto;

import org.dentalcrm.domain.whatsapp.Conversacion;

import java.util.List;

public record ConversacionDetalleResponse(
        ConversacionResponse conversacion,
        List<MensajeResponse> mensajes
) {
    public static ConversacionDetalleResponse from(Conversacion c, String ultimoMensaje,
                                                   List<MensajeResponse> mensajes) {
        return new ConversacionDetalleResponse(
                ConversacionResponse.from(c, ultimoMensaje), mensajes);
    }
}
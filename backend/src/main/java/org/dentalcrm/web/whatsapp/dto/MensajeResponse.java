package org.dentalcrm.web.whatsapp.dto;

import org.dentalcrm.domain.whatsapp.Mensaje;

import java.time.Instant;

public record MensajeResponse(
        Long id,
        String direccion,
        String texto,
        String estado,
        String remitente,
        String error,
        Instant receivedAt
) {
    public static MensajeResponse from(Mensaje m) {
        return new MensajeResponse(
                m.getId(), m.getDireccion().name(), m.getTexto(),
                m.getEstado().name(), m.getRemitente(), m.getError(), m.getReceivedAt());
    }
}
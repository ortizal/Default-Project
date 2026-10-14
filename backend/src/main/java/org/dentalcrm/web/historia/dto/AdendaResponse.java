package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.EvolucionAdenda;

import java.time.Instant;

public record AdendaResponse(
        Long id,
        String texto,
        Long autorId,
        Instant createdAt
) {
    public static AdendaResponse from(EvolucionAdenda a) {
        return new AdendaResponse(a.getId(), a.getTexto(), a.getAutorId(), a.getCreatedAt());
    }
}

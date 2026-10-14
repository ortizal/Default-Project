package org.dentalcrm.web.agenda.publica.dto;

import org.dentalcrm.web.cita.dto.CitaResponse;

import java.util.List;

/** Resultado de la reserva web (§15): creada, repetida u ocupada con alternativas. */
public record AgendaReservarResponse(
        String estado,
        CitaResponse cita,
        List<AgendaSlotResponse> alternativas,
        String mensaje
) {
    public static final String CREADA = "CREADA";
    public static final String REPETIDA = "REPETIDA";
    public static final String OCUPADA = "OCUPADA";
    public static final String ERROR = "ERROR";
}

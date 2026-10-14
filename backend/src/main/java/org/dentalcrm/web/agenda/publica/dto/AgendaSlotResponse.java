package org.dentalcrm.web.agenda.publica.dto;

import java.time.LocalTime;

/**
 * Cupo libre mostrado por el calendario público (sólo lo que ya sabe el
 * paciente, §12). {@code slotToken} es el cupo firmado: al confirmar, el
 * navegador lo devuelve en lugar de mandar fecha y hora "a ojo" (§15).
 */
public record AgendaSlotResponse(String horaInicio, String horaFin, Long odontologoId,
                                 String odontologo, String slotToken) {

    public AgendaSlotResponse(String horaInicio, String horaFin, Long odontologoId, String odontologo) {
        this(horaInicio, horaFin, odontologoId, odontologo, null);
    }
}

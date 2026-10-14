package org.dentalcrm.web.agenda.publica.dto;

import java.time.LocalDate;

/**
 * Reserva desde el calendario público (§13, §15): token + cupo elegido.
 *
 * <p>El cupo se identifica con {@code slotToken} (firmado al listar /slots);
 * {@code fecha} y {@code horaInicio} quedan como respaldo de compatibilidad
 * y para auditoría visual en el cliente.
 */
public record AgendaReservarRequest(
        String token,
        LocalDate fecha,
        String horaInicio,
        Long odontologoId,
        Long servicioId,
        String idempotencyKey,
        String slotToken
) {

    public AgendaReservarRequest(String token, LocalDate fecha, String horaInicio,
                                 Long odontologoId, Long servicioId, String idempotencyKey) {
        this(token, fecha, horaInicio, odontologoId, servicioId, idempotencyKey, null);
    }
}

package org.dentalcrm.web.agenda.dto;

import org.dentalcrm.domain.horario.ExcepcionHorario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @param citasCanceladas citas activas canceladas al guardar la excepción;
 *                        solo tiene valor al crearla, {@code null} en el resto
 *                        de respuestas.
 */
public record ExcepcionHorarioResponse(
        Long id,
        Long odontologoId,
        String odontologoNombre,
        LocalDate fecha,
        String tipo,
        String tipoNombre,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo,
        Integer citasCanceladas,
        Instant createdAt
) {
    public static ExcepcionHorarioResponse from(ExcepcionHorario e, Integer citasCanceladas) {
        return new ExcepcionHorarioResponse(
                e.getId(),
                e.getOdontologo().getId(),
                e.getOdontologo().getNombres() + " " + e.getOdontologo().getApellidos(),
                e.getFecha(),
                e.getTipo(),
                ExcepcionHorario.TIPO_CERRADO.equals(e.getTipo()) ? "Cerrado" : "Horario especial",
                e.getHoraInicio(),
                e.getHoraFin(),
                e.getMotivo(),
                citasCanceladas,
                e.getCreatedAt());
    }

    public static ExcepcionHorarioResponse from(ExcepcionHorario e) {
        return from(e, null);
    }
}

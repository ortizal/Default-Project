package org.dentalcrm.web.agenda.publica.dto;

/** Contexto mínimo que ve el calendario público (§12): nunca datos de terceros. */
public record AgendaContextResponse(
        Long odontologoId,
        String odontologo,
        String especialidad,
        String paciente,
        Long servicioId,
        long expiraEnMinutos
) {}

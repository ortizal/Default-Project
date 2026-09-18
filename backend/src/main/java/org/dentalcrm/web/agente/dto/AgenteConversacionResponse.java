package org.dentalcrm.web.agente.dto;

import java.time.Instant;

public record AgenteConversacionResponse(
        Long id,
        String telefono,
        String nombreContacto,
        String estado,
        Long pacienteId,
        String pacienteNombre,
        String intencion,
        Boolean agenteActivo,
        String contextoAgente,
        String ultimoMensaje,
        Instant ultimoMensajeAt
) {
}
package org.dentalcrm.web.whatsapp.dto;

import org.dentalcrm.domain.whatsapp.Conversacion;

import java.time.Instant;

public record ConversacionResponse(
        Long id,
        String sesionId,
        String telefono,
        String nombreContacto,
        String estado,
        Long pacienteId,
        String pacienteNombres,
        String ultimoMensaje,
        Instant ultimoMensajeAt
) {
    public static ConversacionResponse from(Conversacion c, String ultimoMensaje) {
        String pacienteNombres = c.getPaciente() == null ? null
                : c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos();
        return new ConversacionResponse(
                c.getId(), c.getSesion() == null ? null : c.getSesion().getSesionId(), c.getTelefono(),
                c.getNombreContacto(), c.getEstado() == null ? null : c.getEstado().name(),
                c.getPaciente() == null ? null : c.getPaciente().getId(),
                pacienteNombres, ultimoMensaje, c.getUltimoMensajeAt());
    }
}
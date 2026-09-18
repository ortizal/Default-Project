package org.dentalcrm.web.automatizacion.dto;

import org.dentalcrm.domain.automatizacion.Notificacion;

import java.time.Instant;

public record NotificacionResponse(
        Long id,
        Long citaId,
        Long pacienteId,
        String telefono,
        String evento,
        String plantilla,
        String mensaje,
        Instant programadaAt,
        Instant enviadaAt,
        String estado,
        Integer intentos,
        String error,
        Instant createdAt
) {
    public static NotificacionResponse from(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getCita() == null ? null : n.getCita().getId(),
                n.getPaciente() == null ? null : n.getPaciente().getId(),
                n.getTelefono(),
                n.getAutomatizacion() == null ? null : n.getAutomatizacion().getEvento().name(),
                n.getPlantilla() == null ? null : n.getPlantilla().getNombre(),
                n.getMensajeGenerado(),
                n.getProgramadaAt(), n.getEnviadaAt(),
                n.getEstado().name(), n.getIntentos(), n.getError(),
                n.getCreatedAt());
    }
}
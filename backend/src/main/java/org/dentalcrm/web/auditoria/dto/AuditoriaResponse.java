package org.dentalcrm.web.auditoria.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String accion,
        String modulo,
        String entidad,
        Long entidadId,
        JsonNode datosAnteriores,
        JsonNode datosNuevos,
        String ip,
        Instant createdAt
) {
}

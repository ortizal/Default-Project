package org.dentalcrm.web.historia.dto;

import java.time.Instant;

/**
 * Evento unificado de la línea temporal del paciente (plan §4.16).
 * Los tipos de las fases siguientes (odontograma, planes, recetas…)
 * se sumarán a esta misma lista.
 */
public record TimelineEventoResponse(
        String tipo,
        Instant fecha,
        String titulo,
        String detalle,
        Long referenciaId
) {}

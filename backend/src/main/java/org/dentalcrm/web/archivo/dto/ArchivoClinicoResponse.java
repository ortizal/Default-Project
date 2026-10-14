package org.dentalcrm.web.archivo.dto;

import org.dentalcrm.domain.archivo.ArchivoClinico;

import java.time.Instant;
import java.time.LocalDate;

/** Metadatos de un archivo clínico (los bytes no viajan por este DTO). */
public record ArchivoClinicoResponse(
        Long id,
        Long pacienteId,
        String categoria,
        String nombre,
        String mime,
        Long tamano,
        String sha256,
        LocalDate fecha,
        String observaciones,
        Instant subidoEn
) {
    public static ArchivoClinicoResponse from(ArchivoClinico a) {
        return new ArchivoClinicoResponse(a.getId(), a.getPaciente().getId(),
                a.getCategoria().name(), a.getNombre(), a.getMime(), a.getTamano(),
                a.getSha256(), a.getFecha(), a.getObservaciones(), a.getCreatedAt());
    }
}

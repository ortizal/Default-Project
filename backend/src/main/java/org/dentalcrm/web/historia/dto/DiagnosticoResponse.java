package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.DiagnosticoClinico;
import org.dentalcrm.domain.historia.EstadoDiagnostico;
import org.dentalcrm.domain.historia.SeveridadDiagnostico;

import java.time.Instant;
import java.time.LocalDate;

public record DiagnosticoResponse(
        Long id,
        String nombre,
        String region,
        SeveridadDiagnostico severidad,
        EstadoDiagnostico estado,
        LocalDate fecha,
        Long odontologoId,
        String odontologo,
        String observaciones,
        Instant createdAt,
        Instant updatedAt
) {
    public static DiagnosticoResponse from(DiagnosticoClinico d) {
        return new DiagnosticoResponse(
                d.getId(), d.getNombre(), d.getRegion(), d.getSeveridad(), d.getEstado(),
                d.getFecha(),
                d.getOdontologo() == null ? null : d.getOdontologo().getId(),
                OdontologoNombres.de(d.getOdontologo()),
                d.getObservaciones(), d.getCreatedAt(), d.getUpdatedAt());
    }
}

package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.AntecedenteMedico;
import org.dentalcrm.domain.historia.EstadoAntecedente;
import org.dentalcrm.domain.historia.TipoAntecedente;

import java.time.Instant;
import java.time.LocalDate;

public record AntecedenteResponse(
        Long id,
        TipoAntecedente tipo,
        String nombre,
        String detalle,
        LocalDate fechaDesde,
        EstadoAntecedente estado,
        String observaciones,
        Instant createdAt,
        Instant updatedAt
) {
    public static AntecedenteResponse from(AntecedenteMedico a) {
        return new AntecedenteResponse(
                a.getId(), a.getTipo(), a.getNombre(), a.getDetalle(), a.getFechaDesde(),
                a.getEstado(), a.getObservaciones(), a.getCreatedAt(), a.getUpdatedAt());
    }
}

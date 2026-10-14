package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.ExamenClinico;

import java.time.Instant;
import java.time.LocalDate;

public record ExamenResponse(
        Long id,
        LocalDate fecha,
        Long odontologoId,
        String odontologo,
        String general,
        String extraoralAtm,
        String extraoralSimetria,
        String extraoralGanglios,
        String extraoralApertura,
        String intraoralMucosas,
        String intraoralLengua,
        String intraoralPaladar,
        String intraoralEncias,
        String intraoralHigiene,
        String intraoralOclusion,
        String observaciones,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamenResponse from(ExamenClinico e) {
        return new ExamenResponse(
                e.getId(), e.getFecha(),
                e.getOdontologo() == null ? null : e.getOdontologo().getId(),
                OdontologoNombres.de(e.getOdontologo()),
                e.getGeneral(), e.getExtraoralAtm(), e.getExtraoralSimetria(),
                e.getExtraoralGanglios(), e.getExtraoralApertura(),
                e.getIntraoralMucosas(), e.getIntraoralLengua(), e.getIntraoralPaladar(),
                e.getIntraoralEncias(), e.getIntraoralHigiene(), e.getIntraoralOclusion(),
                e.getObservaciones(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

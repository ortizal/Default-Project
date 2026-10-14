package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.ExamenPeriodontal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExamenPeriodontalResponse(
        Long id,
        Long historiaId,
        int version,
        LocalDate fecha,
        String odontologo,
        String observaciones,
        List<SitioPeriodontalResponse> sitios,
        List<DientePeriodontalResponse> dientes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamenPeriodontalResponse from(ExamenPeriodontal e,
                                                 List<SitioPeriodontalResponse> sitios,
                                                 List<DientePeriodontalResponse> dientes,
                                                 String odontologo) {
        return new ExamenPeriodontalResponse(e.getId(), e.getHistoria().getId(), e.getVersion(),
                e.getFecha(), odontologo, e.getObservaciones(), sitios, dientes,
                e.getCreatedAt(), e.getUpdatedAt());
    }
}

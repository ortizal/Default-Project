package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.ExamenPeriodontal;

import java.time.LocalDate;

public record ExamenPeriodontalResumenResponse(
        Long id,
        int version,
        LocalDate fecha,
        String odontologo,
        String observaciones,
        int sitios,
        int dientes
) {
    public static ExamenPeriodontalResumenResponse from(ExamenPeriodontal e, int sitios, int dientes,
                                                        String odontologo) {
        return new ExamenPeriodontalResumenResponse(e.getId(), e.getVersion(), e.getFecha(),
                odontologo, e.getObservaciones(), sitios, dientes);
    }
}

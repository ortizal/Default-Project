package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.Odontograma;

import java.time.LocalDate;

public record OdontogramaResumenResponse(
        Long id,
        int version,
        LocalDate fecha,
        String denticion,
        String observaciones,
        long registros
) {
    public static OdontogramaResumenResponse from(Odontograma o, long registros) {
        return new OdontogramaResumenResponse(
                o.getId(), o.getVersion(), o.getFecha(), o.getDenticion().name(),
                o.getObservaciones(), registros);
    }
}

package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.PeriodontogramaDiente;

public record DientePeriodontalResponse(
        Long id,
        int diente,
        Integer movilidad,
        Integer furcacion,
        String notas
) {
    public static DientePeriodontalResponse from(PeriodontogramaDiente d) {
        return new DientePeriodontalResponse(d.getId(), d.getDiente(), d.getMovilidad(),
                d.getFurcacion(), d.getNotas());
    }
}

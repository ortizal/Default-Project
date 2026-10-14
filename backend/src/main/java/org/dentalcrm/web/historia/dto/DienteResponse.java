package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.EstadoOdontograma;
import org.dentalcrm.domain.historia.OdontogramaDiente;
import org.dentalcrm.domain.historia.SuperficieDental;

public record DienteResponse(
        Long id,
        int diente,
        SuperficieDental superficie,
        String condicion,
        EstadoOdontograma estado,
        String notas
) {
    public static DienteResponse from(OdontogramaDiente d) {
        return new DienteResponse(d.getId(), d.getDiente(), d.getSuperficie(),
                d.getCondicion(), d.getEstado(), d.getNotas());
    }
}

package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.EstadoOdontograma;
import org.dentalcrm.domain.historia.SuperficieDental;

public record CambioResponse(
        int diente,
        SuperficieDental superficie,
        String condicion,
        EstadoOdontograma estadoAnterior,
        EstadoOdontograma estadoNuevo,
        String tipo
) {
}

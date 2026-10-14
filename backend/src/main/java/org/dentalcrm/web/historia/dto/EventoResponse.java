package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.OdontogramaEvento;
import org.dentalcrm.domain.historia.SuperficieDental;

import java.time.Instant;

public record EventoResponse(
        Long id,
        String accion,
        Integer diente,
        SuperficieDental superficie,
        String condicion,
        String estadoAnterior,
        String estadoNuevo,
        String detalle,
        Instant fecha
) {
    public static EventoResponse from(OdontogramaEvento e) {
        return new EventoResponse(e.getId(), e.getAccion().name(), e.getDiente(), e.getSuperficie(),
                e.getCondicion(), e.getEstadoAnterior(), e.getEstadoNuevo(), e.getDetalle(), e.getFecha());
    }
}

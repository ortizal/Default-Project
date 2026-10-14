package org.dentalcrm.web.consentimiento.dto;

import org.dentalcrm.domain.consentimiento.ConsentimientoPlantilla;

/** Texto base de un tipo de consentimiento (§4.13). */
public record PlantillaConsentimientoResponse(
        Long id,
        String codigo,
        String nombre,
        String categoria,
        String contenido,
        int version
) {
    public static PlantillaConsentimientoResponse from(ConsentimientoPlantilla p) {
        return new PlantillaConsentimientoResponse(p.getId(), p.getCodigo(), p.getNombre(),
                p.getCategoria(), p.getContenido(), p.getVersion());
    }
}

package org.dentalcrm.web.receta.dto;

import org.dentalcrm.domain.receta.RecetaLinea;

public record RecetaLineaResponse(
        Long id,
        int numero,
        String medicamento,
        String concentracion,
        String presentacion,
        String dosis,
        String via,
        String frecuencia,
        String duracion,
        String instrucciones
) {
    public static RecetaLineaResponse from(RecetaLinea l) {
        return new RecetaLineaResponse(l.getId(), l.getNumero(), l.getMedicamento(),
                l.getConcentracion(), l.getPresentacion(), l.getDosis(),
                l.getVia() == null ? null : l.getVia().name(),
                l.getFrecuencia(), l.getDuracion(), l.getInstrucciones());
    }
}

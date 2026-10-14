package org.dentalcrm.web.receta.dto;

import org.dentalcrm.domain.receta.Receta;

import java.time.LocalDate;

public record RecetaResumenResponse(
        Long id,
        LocalDate fecha,
        String estado,
        String odontologo,
        String diagnostico,
        String motivoAnulacion,
        int lineas
) {
    public static RecetaResumenResponse from(Receta r, String odontologo) {
        return new RecetaResumenResponse(r.getId(), r.getFecha(), r.getEstado().name(),
                odontologo, r.getDiagnostico(), r.getMotivoAnulacion(), r.getLineas().size());
    }
}

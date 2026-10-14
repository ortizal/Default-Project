package org.dentalcrm.web.consentimiento.dto;

import org.dentalcrm.domain.consentimiento.Consentimiento;

import java.time.LocalDate;
import java.time.Instant;

public record ConsentimientoResumenResponse(
        Long id,
        LocalDate fecha,
        String titulo,
        String categoria,
        String plantillaCodigo,
        int plantillaVersion,
        String estado,
        String odontologo,
        String tutorNombre,
        Long tutorId,
        String tutorParentesco,
        String tutorTelefono,
        String firmante,
        String firmaHash,
        Instant firmadoEn
) {
    public static ConsentimientoResumenResponse from(Consentimiento c, String odontologo) {
        return new ConsentimientoResumenResponse(c.getId(), c.getFecha(), c.getTitulo(),
                c.getCategoria(), c.getPlantillaCodigo(), c.getPlantillaVersion(),
                c.getEstado().name(), odontologo, c.getTutorNombre(), c.getTutorId(),
                c.getTutorParentesco(), c.getTutorTelefono(), c.getFirmante(),
                c.getFirmaHash(), c.getFirmadoEn());
    }
}

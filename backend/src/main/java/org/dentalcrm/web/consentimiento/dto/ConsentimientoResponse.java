package org.dentalcrm.web.consentimiento.dto;

import org.dentalcrm.domain.consentimiento.Consentimiento;

import java.time.Instant;
import java.time.LocalDate;

public record ConsentimientoResponse(
        Long id,
        Long pacienteId,
        String paciente,
        String plantillaCodigo,
        int plantillaVersion,
        String titulo,
        String categoria,
        String contenido,
        LocalDate fecha,
        String tutorNombre,
        Long tutorId,
        String tutorParentesco,
        String tutorTelefono,
        Long odontologoId,
        String odontologo,
        String estado,
        String firmante,
        String firmaImagen,
        String firmaHash,
        Instant firmadoEn,
        String observaciones,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConsentimientoResponse from(Consentimiento c, String paciente, String odontologo) {
        return new ConsentimientoResponse(c.getId(), c.getPaciente().getId(), paciente,
                c.getPlantillaCodigo(), c.getPlantillaVersion(), c.getTitulo(), c.getCategoria(),
                c.getContenido(), c.getFecha(), c.getTutorNombre(), c.getTutorId(),
                c.getTutorParentesco(), c.getTutorTelefono(),
                c.getOdontologo() == null ? null : c.getOdontologo().getId(), odontologo,
                c.getEstado().name(), c.getFirmante(), c.getFirmaImagen(), c.getFirmaHash(),
                c.getFirmadoEn(), c.getObservaciones(), c.getCreatedAt(), c.getUpdatedAt());
    }
}

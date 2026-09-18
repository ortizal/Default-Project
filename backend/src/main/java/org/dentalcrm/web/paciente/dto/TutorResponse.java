package org.dentalcrm.web.paciente.dto;

import org.dentalcrm.domain.paciente.PacienteTutor;

public record TutorResponse(
        Long id,
        String parentesco,
        String nombres,
        String apellidos,
        String telefono
) {
    public static TutorResponse from(PacienteTutor t) {
        return new TutorResponse(t.getId(), t.getParentesco(), t.getNombres(), t.getApellidos(), t.getTelefono());
    }
}
package org.dentalcrm.web.receta.dto;

import org.dentalcrm.domain.receta.Receta;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecetaResponse(
        Long id,
        Long pacienteId,
        String paciente,
        Long odontologoId,
        String odontologo,
        LocalDate fecha,
        String diagnostico,
        String observaciones,
        String estado,
        String motivoAnulacion,
        List<RecetaLineaResponse> lineas,
        Instant createdAt,
        Instant updatedAt
) {
    public static RecetaResponse from(Receta r, String paciente, String odontologo,
                                      List<RecetaLineaResponse> lineas) {
        return new RecetaResponse(r.getId(), r.getPaciente().getId(), paciente,
                r.getOdontologo() == null ? null : r.getOdontologo().getId(), odontologo,
                r.getFecha(), r.getDiagnostico(), r.getObservaciones(), r.getEstado().name(),
                r.getMotivoAnulacion(), lineas, r.getCreatedAt(), r.getUpdatedAt());
    }
}

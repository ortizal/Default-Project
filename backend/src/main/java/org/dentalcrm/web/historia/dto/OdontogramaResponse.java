package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.Odontograma;
import org.dentalcrm.domain.historia.OdontogramaDiente;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OdontogramaResponse(
        Long id,
        int version,
        LocalDate fecha,
        String denticion,
        String odontologo,
        String observaciones,
        List<DienteResponse> dientes,
        Instant createdAt,
        Instant updatedAt
) {
    public static OdontogramaResponse from(Odontograma o, List<OdontogramaDiente> dientes, String odontologo) {
        return new OdontogramaResponse(
                o.getId(), o.getVersion(), o.getFecha(), o.getDenticion().name(), odontologo,
                o.getObservaciones(),
                dientes.stream().map(DienteResponse::from).toList(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}

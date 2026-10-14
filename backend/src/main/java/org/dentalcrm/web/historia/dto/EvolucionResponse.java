package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.EstadoEvolucion;
import org.dentalcrm.domain.historia.Evolucion;

import java.time.Instant;
import java.util.List;

public record EvolucionResponse(
        Long id,
        Instant fecha,
        Long odontologoId,
        String odontologo,
        Long citaId,
        String motivo,
        String hallazgos,
        String diagnostico,
        String procedimiento,
        String anestesia,
        String materiales,
        String indicaciones,
        String conducta,
        String soapSubjetivo,
        String soapObjetivo,
        String soapAnalisis,
        String soapPlan,
        EstadoEvolucion estado,
        Instant cerradaEn,
        Long cerradaPor,
        List<AdendaResponse> adendas,
        Instant createdAt,
        Instant updatedAt
) {
    public static EvolucionResponse from(Evolucion e) {
        return from(e, List.of());
    }

    public static EvolucionResponse from(Evolucion e, List<AdendaResponse> adendas) {
        return new EvolucionResponse(
                e.getId(), e.getFecha(),
                e.getOdontologo() == null ? null : e.getOdontologo().getId(),
                OdontologoNombres.de(e.getOdontologo()),
                e.getCita() == null ? null : e.getCita().getId(),
                e.getMotivo(), e.getHallazgos(), e.getDiagnostico(), e.getProcedimiento(),
                e.getAnestesia(), e.getMateriales(), e.getIndicaciones(), e.getConducta(),
                e.getSoapSubjetivo(), e.getSoapObjetivo(), e.getSoapAnalisis(), e.getSoapPlan(),
                e.getEstado(), e.getCerradaEn(), e.getCerradaPor(),
                adendas, e.getCreatedAt(), e.getUpdatedAt());
    }
}

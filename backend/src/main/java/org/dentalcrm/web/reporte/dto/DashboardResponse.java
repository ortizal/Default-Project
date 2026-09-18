package org.dentalcrm.web.reporte.dto;

public record DashboardResponse(
        long citasDeHoy,
        long citasPendientesHoy,
        long citasConfirmadasHoy,
        long citasCanceladasHoy,
        long noAsistieronHoy,
        long pacientesNuevosMes,
        long conversacionesAbiertas,
        long mensajesEnviados,
        long mensajesConError,
        long notificacionesEnviadas,
        long notificacionesConError
) {
}
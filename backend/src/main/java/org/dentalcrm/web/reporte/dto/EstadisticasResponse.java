package org.dentalcrm.web.reporte.dto;

import java.util.List;

public record EstadisticasResponse(
        List<ReporteSerie> citasPorDia,
        List<ReporteSerie> confirmaciones,
        List<ReporteSerie> cancelaciones,
        List<ReporteSerie> noAsistencia,
        List<ReporteDetalle> citasPorOdontologo,
        List<ReporteDetalle> serviciosMasSolicitados,
        List<ReporteDetalle> porEstado,
        List<ReporteDetalle> confirmacionesPorFuente
) {
}
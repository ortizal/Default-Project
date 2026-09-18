package org.dentalcrm.web.reporte.dto;

import java.time.LocalDate;

public record ReporteSerie(
        LocalDate fecha,
        long total
) {
}
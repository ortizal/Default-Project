package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reporte de caja por rango con el corte de cada sesión (§3.13). */
public record ReporteCajaResponse(
        LocalDate desde,
        LocalDate hasta,
        int cantidadSesiones,
        BigDecimal aperturas,
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal diferenciaTotal,
        List<SesionResumen> sesiones
) {
    public record SesionResumen(Long id, String nombre, LocalDate fecha, String estado,
                                BigDecimal apertura, BigDecimal declarado, BigDecimal diferencia,
                                int movimientos) {
    }
}

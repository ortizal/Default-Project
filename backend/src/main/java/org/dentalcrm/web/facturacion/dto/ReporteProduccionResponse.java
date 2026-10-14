package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Producción por odontólogo en un rango (plan §3.13). */
public record ReporteProduccionResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal total,
        int cargos,
        List<FilaProduccion> porOdontologo
) {
    public record FilaProduccion(String odontologo, BigDecimal facturado, int cargos,
                                 BigDecimal descuentos) {
    }
}

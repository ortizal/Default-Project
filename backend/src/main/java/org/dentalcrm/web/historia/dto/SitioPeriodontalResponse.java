package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.PeriodontogramaSitio;

import java.math.BigDecimal;

public record SitioPeriodontalResponse(
        Long id,
        int diente,
        String sitio,
        BigDecimal sondajeMm,
        BigDecimal margenMm,
        BigDecimal insercionMm,
        boolean sangrado,
        boolean placa,
        boolean supuracion
) {
    public static SitioPeriodontalResponse from(PeriodontogramaSitio s) {
        return new SitioPeriodontalResponse(s.getId(), s.getDiente(), s.getSitio().name(),
                s.getSondajeMm(), s.getMargenMm(), s.getInsercionMm(),
                s.isSangrado(), s.isPlaca(), s.isSupuracion());
    }
}

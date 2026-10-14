package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.dentalcrm.domain.historia.SitioPeriodontal;

import java.math.BigDecimal;

/** Medición de un diente en un sitio (plan §4.11). */
public record SitioPeriodontalRequest(
        @NotNull Integer diente,
        @NotNull SitioPeriodontal sitio,
        @DecimalMin("0") @DecimalMax("20") BigDecimal sondajeMm,
        @DecimalMin("-5") @DecimalMax("10") BigDecimal margenMm,
        @DecimalMin("0") @DecimalMax("20") BigDecimal insercionMm,
        Boolean sangrado,
        Boolean placa,
        Boolean supuracion
) {
}

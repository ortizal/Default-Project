package org.dentalcrm.web.plan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.historia.SuperficieDental;
import org.dentalcrm.domain.plan.PrioridadLinea;

import java.math.BigDecimal;

public record LineaRequest(
        Integer numero,
        Integer fase,
        Integer diente,
        SuperficieDental superficie,
        @Size(max = 150) String diagnostico,
        @NotBlank @Size(max = 150) String procedimiento,
        Long servicioId,
        PrioridadLinea prioridad,
        @NotNull Integer sesiones,
        @NotNull @DecimalMin("0") BigDecimal precioUnitario,
        @DecimalMin("0") @DecimalMax("100") BigDecimal descuentoPct,
        @Size(max = 255) String notas
) {
}

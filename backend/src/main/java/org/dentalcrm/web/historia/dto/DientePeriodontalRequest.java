package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Movilidad (0–3), furcación (0–3) y notas de una pieza. */
public record DientePeriodontalRequest(
        @NotNull Integer diente,
        @Min(0) @Max(3) Integer movilidad,
        @Min(0) @Max(3) Integer furcacion,
        @Size(max = 255) String notas
) {
}

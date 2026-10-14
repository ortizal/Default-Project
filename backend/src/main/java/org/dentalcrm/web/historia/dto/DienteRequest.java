package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.historia.EstadoOdontograma;
import org.dentalcrm.domain.historia.SuperficieDental;

public record DienteRequest(
        @NotNull Integer diente,
        SuperficieDental superficie,
        @NotBlank @Size(max = 40) String condicion,
        EstadoOdontograma estado,
        @Size(max = 255) String notas
) {
}

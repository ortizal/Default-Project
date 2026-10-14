package org.dentalcrm.web.receta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.receta.ViaAdministracion;

/** Pauta de un medicamento: dosis, vía, frecuencia, duración e instrucciones (§4.12). */
public record RecetaLineaRequest(
        @NotBlank @Size(max = 150) String medicamento,
        @Size(max = 100) String concentracion,
        @Size(max = 100) String presentacion,
        @NotBlank @Size(max = 100) String dosis,
        ViaAdministracion via,
        @Size(max = 100) String frecuencia,
        @Size(max = 100) String duracion,
        @Size(max = 500) String instrucciones
) {
}

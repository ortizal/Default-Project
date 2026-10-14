package org.dentalcrm.web.consentimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editar el texto de una plantilla: sólo sube la versión y no toca los consentimientos ya emitidos. */
public record ActualizarPlantillaConsentimientoRequest(
        @Size(max = 150) String nombre,
        @NotBlank @Size(max = 40000) String contenido
) {
}

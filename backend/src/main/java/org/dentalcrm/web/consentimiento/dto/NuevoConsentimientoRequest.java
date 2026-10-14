package org.dentalcrm.web.consentimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Alta de un consentimiento en BORRADOR con el snapshot de una plantilla.
 * `tutorId` reutiliza el tutor ya registrado en el paciente (§4.14); si se
 * omite se conserva el `tutorNombre` tecleado.
 */
public record NuevoConsentimientoRequest(
        @NotBlank @Size(max = 40) String plantillaCodigo,
        LocalDate fecha,
        @Size(max = 150) String tutorNombre,
        Long tutorId,
        Long odontologoId,
        @Size(max = 500) String observaciones
) {
}

package org.dentalcrm.web.consentimiento.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Edición de un consentimiento mientras siga en BORRADOR. */
public record EditarConsentimientoRequest(
        LocalDate fecha,
        @Size(max = 150) String titulo,
        @Size(max = 40000) String contenido,
        @Size(max = 150) String tutorNombre,
        Long tutorId,
        Long odontologoId,
        @Size(max = 500) String observaciones
) {
}

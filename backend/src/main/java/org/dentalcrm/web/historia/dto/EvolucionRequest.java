package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EvolucionRequest(
        Instant fecha,
        Long odontologoId,
        Long citaId,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 255) String motivo,
        String hallazgos,
        String diagnostico,
        String procedimiento,
        @Size(max = 155) String anestesia,
        @Size(max = 255) String materiales,
        String indicaciones,
        @Size(max = 255) String conducta,
        String soapSubjetivo,
        String soapObjetivo,
        String soapAnalisis,
        String soapPlan
) {}

package org.dentalcrm.web.cita.dto;

import jakarta.validation.constraints.Size;

public record CancelarRequest(
        @Size(max = 255) String motivo
) {}
package org.dentalcrm.web.agente.dto;

import jakarta.validation.constraints.NotNull;

public record ActivarAgenteRequest(
        @NotNull(message = "activo es obligatorio") Boolean activo
) {
}
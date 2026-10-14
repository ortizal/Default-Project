package org.dentalcrm.web.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.plan.EstadoPresupuesto;

/**
 * Cambio de estado de un presupuesto: presentar, aceptar (con firmante)
 * o rechazar.
 */
public record AceptacionRequest(
        @NotNull EstadoPresupuesto estado,
        @Size(max = 120) String firmante,
        @Size(max = 255) String comentario
) {
}

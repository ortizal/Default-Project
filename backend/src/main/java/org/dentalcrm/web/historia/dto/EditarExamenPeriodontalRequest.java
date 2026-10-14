package org.dentalcrm.web.historia.dto;

import java.time.LocalDate;

public record EditarExamenPeriodontalRequest(
        LocalDate fecha,
        String observaciones
) {
}

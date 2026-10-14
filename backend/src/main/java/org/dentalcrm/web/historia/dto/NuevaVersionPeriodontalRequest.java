package org.dentalcrm.web.historia.dto;

import java.time.LocalDate;

public record NuevaVersionPeriodontalRequest(
        LocalDate fecha,
        String observaciones,
        Boolean copiarUltima
) {
}

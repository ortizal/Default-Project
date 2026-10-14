package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.Denticion;

import java.time.LocalDate;

public record NuevaVersionRequest(
        LocalDate fecha,
        Denticion denticion,
        String observaciones,
        Boolean copiarUltima
) {
}

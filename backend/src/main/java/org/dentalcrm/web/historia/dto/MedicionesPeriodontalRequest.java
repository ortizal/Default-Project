package org.dentalcrm.web.historia.dto;

import jakarta.validation.Valid;

import java.util.List;

/** Guardado masivo: sitios y piezas de un examen periodontal. */
public record MedicionesPeriodontalRequest(
        @Valid List<SitioPeriodontalRequest> sitios,
        @Valid List<DientePeriodontalRequest> dientes
) {
}

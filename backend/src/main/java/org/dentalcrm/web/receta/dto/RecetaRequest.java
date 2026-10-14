package org.dentalcrm.web.receta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Cabecera y líneas de una receta (alta o reemplazo completo de la pauta). */
public record RecetaRequest(
        LocalDate fecha,
        Long odontologoId,
        @Size(max = 255) String diagnostico,
        String observaciones,
        @Valid List<RecetaLineaRequest> lineas
) {
}

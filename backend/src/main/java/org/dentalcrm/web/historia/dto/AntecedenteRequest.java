package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.historia.EstadoAntecedente;
import org.dentalcrm.domain.historia.TipoAntecedente;

import java.time.LocalDate;

public record AntecedenteRequest(
        @NotNull(message = "El tipo es obligatorio") TipoAntecedente tipo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @Size(max = 255) String detalle,
        LocalDate fechaDesde,
        EstadoAntecedente estado,
        String observaciones
) {}

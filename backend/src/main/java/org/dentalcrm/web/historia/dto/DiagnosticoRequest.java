package org.dentalcrm.web.historia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.historia.EstadoDiagnostico;
import org.dentalcrm.domain.historia.SeveridadDiagnostico;

import java.time.LocalDate;

public record DiagnosticoRequest(
        @NotBlank(message = "El diagnóstico es obligatorio") @Size(max = 150) String nombre,
        @Size(max = 60) String region,
        SeveridadDiagnostico severidad,
        EstadoDiagnostico estado,
        LocalDate fecha,
        Long odontologoId,
        String observaciones
) {}

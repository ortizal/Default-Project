package org.dentalcrm.web.historia.dto;

import java.time.LocalDate;

public record ExamenRequest(
        LocalDate fecha,
        Long odontologoId,
        String general,
        String extraoralAtm,
        String extraoralSimetria,
        String extraoralGanglios,
        String extraoralApertura,
        String intraoralMucosas,
        String intraoralLengua,
        String intraoralPaladar,
        String intraoralEncias,
        String intraoralHigiene,
        String intraoralOclusion,
        String observaciones
) {}

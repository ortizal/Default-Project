package org.dentalcrm.web.agenda.dto;

import java.time.LocalTime;

public record SlotResponse(
        LocalTime horaInicio,
        LocalTime horaFin
) {}
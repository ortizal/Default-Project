package org.dentalcrm.web.google.dto;

import java.util.List;

public record GoogleStatusResponse(
        Boolean configurada,
        Boolean conectada,
        String email,
        List<GoogleCalendarioResponse> calendarios,
        GoogleCalendarioResponse calendarioSeleccionado,
        long citasSincronizadas,
        long citasPendientes,
        long citasConError
) {
}
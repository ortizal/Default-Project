package org.dentalcrm.web.google.dto;

import org.dentalcrm.domain.google.GoogleCalendario;

public record GoogleCalendarioResponse(
        Long id,
        String calendarId,
        String summary,
        String timeZone,
        Boolean seleccionado
) {
    public static GoogleCalendarioResponse from(GoogleCalendario c) {
        return new GoogleCalendarioResponse(
                c.getId(), c.getCalendarId(), c.getSummary(), c.getTimeZone(), c.getSeleccionado());
    }
}
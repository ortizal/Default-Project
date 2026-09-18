package org.dentalcrm.web.cita.dto;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.cita.SyncEstado;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CitaResponse(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        String pacienteTelefono,
        Long doctorId,
        String doctorNombre,
        Long servicioId,
        String servicioNombre,
        Integer duracionMinutos,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        EstadoCita estado,
        Boolean confirmada,
        Instant confirmadaAt,
        String confirmationSource,
        String googleEventId,
        String googleCalendarId,
        SyncEstado syncStatus,
        String syncError,
        Integer syncAttempts,
        Instant lastSyncAt,
        Instant canceladaAt,
        String canceladaMotivo,
        String observaciones,
        Long createdBy,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static CitaResponse from(Cita c) {
        return new CitaResponse(
                c.getId(),
                c.getPaciente().getId(),
                c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                c.getPaciente().getTelefono(),
                c.getDoctor().getId(),
                c.getDoctor().getNombres() + " " + c.getDoctor().getApellidos(),
                c.getServicio().getId(),
                c.getServicio().getNombre(),
                c.getServicio().getDuracionMinutos(),
                c.getFecha(), c.getHoraInicio(), c.getHoraFin(),
                c.getEstado(), c.getConfirmada(), c.getConfirmadaAt(), c.getConfirmationSource(),
                c.getGoogleEventId(), c.getGoogleCalendarId(),
                c.getSyncStatus(), c.getSyncError(), c.getSyncAttempts(), c.getLastSyncAt(),
                c.getCanceladaAt(), c.getCanceladaMotivo(),
                c.getObservaciones(), c.getCreatedBy(), c.getVersion(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
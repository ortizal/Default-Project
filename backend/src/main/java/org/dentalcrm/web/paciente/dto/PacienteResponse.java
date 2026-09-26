package org.dentalcrm.web.paciente.dto;

import org.dentalcrm.domain.paciente.Paciente;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PacienteResponse(
        Long id,
        String cedula,
        String nombres,
        String apellidos,
        String telefono,
        String email,
        LocalDate fechaNacimiento,
        String direccion,
        String ciudad,
        String observaciones,
        String estado,
        Instant createdAt,
        Instant updatedAt,
        List<TutorResponse> tutores
) {
    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(
                p.getId(), p.getCedula(), p.getNombres(), p.getApellidos(),
                p.getTelefono(), p.getEmail(), p.getFechaNacimiento(), p.getDireccion(), p.getCiudad(),
                p.getObservaciones(), p.getEstado(), p.getCreatedAt(), p.getUpdatedAt(),
                p.getTutores().stream().map(TutorResponse::from).toList());
    }
}
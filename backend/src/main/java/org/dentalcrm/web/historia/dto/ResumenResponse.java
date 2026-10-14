package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.historia.EstadoHistoria;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ResumenResponse(
        Long historiaId,
        EstadoHistoria estadoHistoria,
        PacienteResumen paciente,
        String odontologoAsignado,
        List<String> alergias,
        List<String> medicacion,
        List<DiagnosticoResponse> diagnosticosActivos,
        int totalAntecedentes,
        int totalEvoluciones,
        EvolucionResponse ultimaEvolucion,
        CitaResumen ultimaCita,
        CitaResumen proximaCita,
        List<String> alertas
) {

    public record PacienteResumen(
            Long id,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            int edad,
            String telefono,
            String email,
            boolean menorEdad
    ) {}

    public record CitaResumen(
            Long id,
            LocalDate fecha,
            LocalTime hora,
            String odontologo,
            String servicio,
            String estado
    ) {}
}

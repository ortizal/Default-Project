package org.dentalcrm.web.historia.dto;

import java.util.List;

/**
 * Datos de la historia clínica completa de un paciente (plan §4.17),
 * listos para renderizarse como PDF o como JSON.
 *
 * <p>El contenido va en secciones genéricas ({@code título + columnas +
 * filas}) para que el generador de PDF no tenga que conocer el modelo.
 */
public record HistoriaCompletaResponse(
        PacienteInfo paciente,
        List<String> resumen,
        List<Seccion> secciones) {

    public record PacienteInfo(
            String nombre,
            String cedula,
            String edad,
            String telefono,
            String correo,
            String historia,
            String odontologo,
            String fechaEmision) {
    }

    public record Seccion(String titulo, List<String> columnas, List<Fila> filas) {
    }

    public record Fila(List<String> celdas) {
    }
}

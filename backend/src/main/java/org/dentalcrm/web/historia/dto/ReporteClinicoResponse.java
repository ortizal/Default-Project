package org.dentalcrm.web.historia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reportes clínicos del consultorio (plan §4.17): totales del período,
 * diagnósticos frecuentes, tratamientos pendientes, continuidad de la
 * atención y producción clínica.
 *
 * <p>Los totales, la producción y los diagnósticos se calculan sobre el rango
 * {@code [desde, hasta]}; los tratamientos pendientes reflejan el estado
 * actual (no dependen del rango) y la continuidad usa además la ventana de
 * 90 días previa a {@code hasta}.
 */
public record ReporteClinicoResponse(
        LocalDate desde,
        LocalDate hasta,
        Totales totales,
        List<FrecuenciaDiagnostico> diagnosticosFrecuentes,
        List<TratamientoPendiente> tratamientosPendientes,
        Continuidad continuidad,
        List<ProduccionClinica> produccionClinica) {

    public record Totales(
            int evoluciones,
            int examenes,
            int diagnosticos,
            int odontogramas,
            int periodontos,
            int recetas,
            int consentimientos,
            int archivos,
            int planes,
            int citasAtendidas,
            int pacientesAtendidos) {
    }

    public record FrecuenciaDiagnostico(String nombre, int total, int pacientes) {
    }

    public record TratamientoPendiente(
            Long pacienteId,
            String paciente,
            Long planId,
            String plan,
            Long lineaId,
            String procedimiento,
            Integer diente,
            String superficie,
            String prioridad,
            String estado,
            BigDecimal precio) {
    }

    public record Continuidad(
            int citasAtendidas,
            int citasConfirmadas,
            int citasCanceladas,
            int citasNoAsistio,
            int pacientesActivos,
            int pacientesSinProximaCita,
            List<SinActividad> sinActividad90d) {
    }

    /** Paciente sin ninguna actividad clínica en los 90 días previos a `hasta`. */
    public record SinActividad(Long pacienteId, String paciente) {
    }

    public record ProduccionClinica(String procedimiento, int cantidad, BigDecimal total) {
    }
}

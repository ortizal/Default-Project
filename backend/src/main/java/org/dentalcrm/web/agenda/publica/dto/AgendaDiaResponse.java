package org.dentalcrm.web.agenda.publica.dto;

import java.time.LocalDate;

/** Estado de un día del calendario público (§12). */
public record AgendaDiaResponse(LocalDate fecha, String estado, int cupos) {

    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String POCA = "POCA";
    public static final String SIN_DISPONIBILIDAD = "SIN_DISPONIBILIDAD";
    public static final String PASADO = "PASADO";
    /** El odontólogo no atiende ese día (sin horario semanal activo o clínica cerrada). */
    public static final String NO_LABORABLE = "NO_LABORABLE";
}

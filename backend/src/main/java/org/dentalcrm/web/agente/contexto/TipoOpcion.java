package org.dentalcrm.web.agente.contexto;

/**
 * Qué representa cada opción de un menú contextual (§4 de las instrucciones).
 */
public enum TipoOpcion {
    /** Opción del menú principal: dispara una intención. */
    MENU,
    /** Servicio odontológico. */
    SERVICIO,
    /** Odontólogo; "cualquier odontólogo" llega con {@code id = null}. */
    ODONTOLOGO,
    /** Franja horaria: MANANA, MEDIODIA, TARDE, TODAS. */
    FRANJA,
    /** Hora concreta de un cupo real. */
    HORA,
    /** Paciente al que aplica la cita (o "otro paciente"). */
    PACIENTE,
    /** Acción sobre una cita: CONFIRMAR, CANCELAR, REPROGRAMAR. */
    ACCION,
    /** Confirmación o decisión genérica. */
    DECISION
}

package org.dentalcrm.domain.plan;

/**
 * Ciclo de vida de un plan de tratamiento (plan §4.8).
 */
public enum EstadoPlanTratamiento {
    BORRADOR,
    PRESENTADO,
    ACEPTADO,
    PARCIALMENTE_ACEPTADO,
    RECHAZADO,
    EN_EJECUCION,
    COMPLETADO,
    CANCELADO
}

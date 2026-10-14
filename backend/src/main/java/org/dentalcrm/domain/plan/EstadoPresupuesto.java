package org.dentalcrm.domain.plan;

/**
 * Estado de un presupuesto emitido: documento separado del plan clínico
 * (plan §4.9: Plan → Presupuesto → Aceptación).
 */
public enum EstadoPresupuesto {
    BORRADOR,
    PRESENTADO,
    ACEPTADO,
    RECHAZADO,
    ANULADO
}

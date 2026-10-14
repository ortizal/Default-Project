package org.dentalcrm.domain.caja;

/** Estado de una sesión de caja (plan §3.4). */
public enum EstadoCaja {
    /** Sesión abierta: admite ingresos y egresos. */
    ABIERTA,
    /** Sesión cerrada con monto declarado y diferencia calculada. */
    CERRADA
}

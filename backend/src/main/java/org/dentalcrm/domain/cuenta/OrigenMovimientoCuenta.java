package org.dentalcrm.domain.cuenta;

/**
 * Origen de un movimiento de la cuenta (plan §3.3 y §5 "Procedimiento →
 * cuenta"): manual (usuario), procedimiento (línea del plan completada)
 * o caja (cobro registrado en una sesión de caja).
 */
public enum OrigenMovimientoCuenta {
    MANUAL,
    PROCEDIMIENTO,
    CAJA
}

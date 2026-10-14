package org.dentalcrm.domain.cuenta;

/**
 * Estados de la cuenta del paciente (plan §3.3).
 *
 * <p>{@link #ABIERTA}, {@link #PARCIAL} y {@link #PAGADA} se DERIVAN de
 * los movimientos: sin cargos está en ABIERTA, con saldo y pagos
 * parciales en PARCIAL y con saldo cubierto en PAGADA. {@link #CERRADA}
 * y {@link #ANULADA} sólo se fijan a mano y bloquean los movimientos.
 */
public enum EstadoCuenta {
    ABIERTA,
    PARCIAL,
    PAGADA,
    CERRADA,
    ANULADA
}

package org.dentalcrm.domain.cuenta;

/**
 * Formas de pago admitidas (plan §3.4): efectivo, transferencia, tarjeta
 * y otros. Se guardan en cada abono/anticipo/devolución y alimentan el
 * corte por forma de pago.
 */
public enum FormaPago {
    EFECTIVO,
    TRANSFERENCIA,
    TARJETA,
    OTRO
}

package org.dentalcrm.domain.cuenta;

/**
 * Tipo de movimiento del libro mayor de la cuenta (plan §3.3).
 *
 * <p>Cada tipo aporta un signo fijo al saldo:
 *
 * <pre>
 *   saldo = Σ CARGO − Σ DESCUENTO − Σ ABONO − Σ ANTICIPO + Σ DEVOLUCION
 * </pre>
 */
public enum TipoMovimientoCuenta {

    /** Cargo clínico o manual: incrementa lo que el paciente debe. */
    CARGO(1),
    /** Descuento aplicado: reduce el saldo sin entregar dinero. */
    DESCUENTO(-1),
    /** Abono sobre un cargo existente (pago recibido). */
    ABONO(-1),
    /** Anticipo recibido antes de generar cargos. */
    ANTICIPO(-1),
    /** Devolución de dinero previamente abonado: recupera saldo. */
    DEVOLUCION(1);

    private final int signo;

    TipoMovimientoCuenta(int signo) {
        this.signo = signo;
    }

    public int signo() {
        return signo;
    }

    /** Los tipos que representan dinero recibido del paciente. */
    public boolean esDineroRecibido() {
        return this == ABONO || this == ANTICIPO;
    }

    /** Los tipos que aportan crédito (dinero a favor) al paciente. */
    public boolean esCredito() {
        return this == ABONO || this == ANTICIPO || this == DEVOLUCION;
    }
}

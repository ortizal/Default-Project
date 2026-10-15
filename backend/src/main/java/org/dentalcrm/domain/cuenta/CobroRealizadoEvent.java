package org.dentalcrm.domain.cuenta;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Publicado cuando el paciente abona o anticipa dinero en su cuenta
 * (plan §3.3): dispara la notificación post-cobro por WhatsApp y correo.
 *
 * @param movimientoId id del {@code cuenta_detalles} que originó el cobro
 * @param pacienteId   paciente que pagó
 * @param tipo         {@link TipoMovimientoCuenta}: ABONO o ANTICIPO
 * @param concepto     concepto capturado en el movimiento
 * @param monto        dinero recibido (siempre positivo)
 * @param formaPago    {@link FormaPago} del cobro
 * @param saldo        saldo de la cuenta después del cobro
 * @param fecha        fecha del movimiento
 */
public record CobroRealizadoEvent(Long movimientoId,
                                  Long pacienteId,
                                  TipoMovimientoCuenta tipo,
                                  String concepto,
                                  BigDecimal monto,
                                  FormaPago formaPago,
                                  BigDecimal saldo,
                                  LocalDate fecha) {
}

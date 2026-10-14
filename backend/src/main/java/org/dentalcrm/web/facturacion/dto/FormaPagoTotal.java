package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;

/** Neto de una forma de pago dentro de la sesión de caja. */
public record FormaPagoTotal(String formaPago, BigDecimal monto) {
}

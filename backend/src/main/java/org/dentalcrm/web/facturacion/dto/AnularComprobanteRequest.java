package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de anulación de un comprobante electrónico (plan §3.10): el
 * comprobante nunca se borra, sólo cambia a {@code ANULADO} y queda
 * registrado quién lo pidió y por qué.
 *
 * @param motivo razón de la anulación; queda en el comprobante y en el
 *               trámite de anulación
 */
public record AnularComprobanteRequest(
        @NotBlank @Size(min = 5, max = 500) String motivo
) {
}

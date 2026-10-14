package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Emisión de una nota de crédito electrónica (plan §3.9): devuelve una
 * factura autorizada total o parcialmente.
 *
 * @param comprobanteId factura que se modifica; debe estar autorizada y sin
 *                      anular
 * @param motivo        razón de la devolución, obligatoria: es el campo que
 *                      el SRI exige en {@code <motivo>} (1–300 caracteres)
 * @param lineas        devolución parcial: líneas (empezando en 1) y
 *                      cantidad devuelta de cada una. <b>Sin líneas</b> la
 *                      nota devuelve la factura entera
 * @param fechaEmision  fecha pedida (por defecto hoy)
 */
public record EmitirNotaCreditoRequest(
        @NotNull Long comprobanteId,
        @NotBlank @Size(min = 5, max = 300) String motivo,
        @Valid List<Devolucion> lineas,
        LocalDate fechaEmision
) {

    /**
     * @param linea    número de línea de la factura original (1 = primera)
     * @param cantidad unidades devueltas; no puede superar las facturadas
     */
    public record Devolucion(
            @NotNull Integer linea,
            @NotNull @DecimalMin("0.01") BigDecimal cantidad
    ) {
    }
}

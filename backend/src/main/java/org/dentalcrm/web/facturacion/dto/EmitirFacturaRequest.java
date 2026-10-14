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
 * Emisión de la factura electrónica de una cuenta (plan §3.5: cuenta →
 * validación → secuencial → clave → XML → XSD → firma → envío →
 * autorización).
 *
 * @param cuentaId   cuenta cuyos cargos se facturan; el paciente, el
 *                   consultorio y la configuración tributaria se leen solos
 * @param fechaEmision fecha pedida (por defecto hoy; el fin de semana se
 *                   traslada al lunes dentro de la clave de acceso)
 * @param formasPago formas de pago a mano; si no vienen se derivan de los
 *                   abonos, anticipos y devoluciones de la cuenta
 */
public record EmitirFacturaRequest(
        @NotNull Long cuentaId,
        LocalDate fechaEmision,
        @Valid List<Pago> formasPago
) {

    /**
     * @param codigo        código del catálogo vigente (01 efectivo · 16
     *                      débito · 19 crédito · 20 otros con sistema financiero)
     * @param plazo         días de crédito, sólo para pago a plazo
     * @param unidadTiempo  unidad del plazo (`DÍAS`, `MESES`…), opcional
     */
    public record Pago(
            @NotBlank @Size(max = 2) String codigo,
            @NotNull @DecimalMin("0.01") BigDecimal total,
            BigDecimal plazo,
            @Size(max = 10) String unidadTiempo
    ) {
    }
}

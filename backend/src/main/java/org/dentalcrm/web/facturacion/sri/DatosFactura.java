package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una factura comercial para generar el XML del SRI (plan §3.6).
 *
 * <p>El generador calcula por su cuenta los totales (subtotal, descuentos,
 * impuestos agrupados e importe total) a partir de las líneas, así que aquí
 * sólo entran los datos base y las decisiones fiscales de cada línea.
 *
 * @param claveAcceso  49 dígitos ya generados ({@link ClaveAccesoService}); debe coincidir con fecha y secuencial
 * @param fechaEmision fecha ya ajustada a día hábil por {@link ClaveAccesoService}
 * @param pagos        al menos una forma de pago; vacío no emite el bloque {@code <pagos>}
 * @param adicionales  hasta 15 campos ({@code <infoAdicional>})
 */
public record DatosFactura(
        String claveAcceso,
        LocalDate fechaEmision,
        Emisor emisor,
        Comprador comprador,
        List<Linea> lineas,
        List<Pago> pagos,
        List<CampoAdicional> adicionales
) {

    /** Datos fiscales del emisor; el RUC debe terminar en 001 (patrón del XSD). */
    public record Emisor(
            String razonSocial,
            String nombreComercial,
            String ruc,
            String dirMatriz,
            AmbienteSri ambiente,
            TipoEmisionSri tipoEmision,
            String codDoc,
            String estab,
            String ptoEmi,
            int secuencial,
            String dirEstablecimiento,
            boolean obligadoContabilidad,
            String agenteRetencion
    ) {
    }

    /** Receptor: 04 RUC · 05 cédula · 06 pasaporte · 07 venta al público · 08 exterior. */
    public record Comprador(
            String tipoIdentificacion,
            String razonSocial,
            String identificacion,
            String direccion
    ) {
    }

    /**
     * Línea de la factura. {@code tarifa} es el porcentaje (15 para el IVA
     * general, 0 para exento) y {@code codigoPorcentaje} el código SRI que
     * lo representa (Tabla 17: 0=0%, 2=12%, 3=14%, 4=15%, 5=5%).
     *
     * @param impuesto impuesto de la línea; cuando se omite ({@code null})
     *                 lo calcula el generador como {@code base × tarifa / 100}
     *                 redondeado. {@link FacturaBuilder} lo fija a mano porque
     *                 los precios ya llevan IVA incluido: así el comprobante
     *                 cuadra al céntimo con la cuenta del paciente.
     */
    public record Linea(
            String codigoPrincipal,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            int codigoImpuesto,
            int codigoPorcentaje,
            BigDecimal tarifa,
            BigDecimal impuesto
    ) {
        /** Línea cuyo impuesto lo calcula el generador (precio sin IVA). */
        public Linea(String codigoPrincipal, String descripcion, BigDecimal cantidad,
                     BigDecimal precioUnitario, BigDecimal descuento, int codigoImpuesto,
                     int codigoPorcentaje, BigDecimal tarifa) {
            this(codigoPrincipal, descripcion, cantidad, precioUnitario, descuento,
                    codigoImpuesto, codigoPorcentaje, tarifa, null);
        }
    }

    /**
     * Forma de pago con el código del catálogo vigente del SRI:
     * 01 efectivo · 16 tarjeta de débito · 19 tarjeta de crédito ·
     * 20 otros con sistema financiero (transferencias y saldo a crédito).
     */
    public record Pago(String formaPago, BigDecimal total, BigDecimal plazo, String unidadTiempo) {
    }

    /** Hasta 15 campos libres (correo del paciente, número de plan, etc.). */
    public record CampoAdicional(String nombre, String valor) {
    }
}

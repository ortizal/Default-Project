package org.dentalcrm.web.facturacion.sri;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una nota de crédito electrónica para generar su XML
 * (plan §3.6 {@code NotaCreditoBuilder}, §3.9).
 *
 * <p>La nota <b>referencia siempre</b> al documento original
 * ({@code codDocModificado}, {@code numDocModificado} y
 * {@code fechaEmisionDocSustento}) y arrastra sus líneas: o bien todas
 * (anulación total) o bien una devolución parcial de las que se
 * indiquen. Los importes salen del XML autorizado de la factura, así que
 * la nota cuadra al céntimo con lo que se emitió.
 *
 * <p>El total sin impuestos, la modificación y los impuestos totales los
 * calcula {@link SriXmlGenerator} a partir de las líneas, igual que en la
 * factura.
 *
 * @param claveAcceso             49 dígitos ya generados con {@code codDoc} = 04
 * @param fechaEmision            fecha de emisión de la nota (ya ajustada a día hábil)
 * @param fechaEmisionDocSustento fecha de emisión de la factura que se modifica
 * @param motivo                  1–300 caracteres, obligatorio (XSD)
 * @param adicionales             hasta 15 campos ({@code <infoAdicional>})
 */
public record DatosNotaCredito(
        String claveAcceso,
        LocalDate fechaEmision,
        DatosFactura.Emisor emisor,
        DatosFactura.Comprador comprador,
        String codDocModificado,
        String numDocModificado,
        LocalDate fechaEmisionDocSustento,
        List<DatosFactura.Linea> lineas,
        String motivo,
        List<DatosFactura.CampoAdicional> adicionales
) {
}

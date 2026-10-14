package org.dentalcrm.web.facturacion.sri;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contenido del RIDE reconstruido desde el XML del comprobante
 * (plan §3.11): el PDF se pinta de esta lectura para que siempre
 * coincida con lo que se envió y autorizó el SRI.
 */
public record RideDatos(
        String claveAcceso,
        String fechaEmision,
        String ambiente,
        String tipoEmision,
        String razonSocialEmisor,
        String nombreComercial,
        String rucEmisor,
        String direccionMatriz,
        String direccionEstablecimiento,
        String obligadoContabilidad,
        String tipoIdentificacionComprador,
        String razonSocialComprador,
        String identificacionComprador,
        String direccionComprador,
        List<Linea> lineas,
        BigDecimal totalSinImpuestos,
        BigDecimal totalDescuento,
        List<Impuesto> impuestos,
        BigDecimal importeTotal,
        List<Pago> pagos,
        List<Campo> adicionales
) {

    /** Una línea del detalle: el total es el precio con IVA ya incluido. */
    public record Linea(String codigo, String descripcion, BigDecimal cantidad,
                        BigDecimal precioUnitario, BigDecimal descuento, BigDecimal total,
                        BigDecimal tarifa, BigDecimal impuesto) {
    }

    /** Bloque de {@code totalConImpuestos}: tarifa, base y valor. */
    public record Impuesto(BigDecimal tarifa, BigDecimal baseImponible, BigDecimal valor) {
    }

    /** Forma de pago con su código del catálogo vigente (01, 16, 19, 20…). */
    public record Pago(String formaPago, BigDecimal total) {
    }

    /** Campo libre de {@code infoAdicional}, p. ej. el correo del paciente. */
    public record Campo(String nombre, String valor) {
    }
}

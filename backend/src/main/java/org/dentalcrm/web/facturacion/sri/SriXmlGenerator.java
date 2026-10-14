package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.List;
import java.util.Map;

/**
 * Generador del XML de factura comercial v1.1.0 del SRI (plan §3.6).
 *
 * <p>El orden de los elementos sigue exactamente el esquema
 * {@code sri/factura_V1.1.0.xsd}: cualquier desviación lo rechaza la
 * validación previa a la firma. Los totales se calculan aquí para que
 * siempre sean coherentes con las líneas: subtotal, descuentos, impuestos
 * agrupados por tarifa e importe total.
 */
public final class SriXmlGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private SriXmlGenerator() {
    }

    public static String generarFactura(DatosFactura datos) {
        if (datos.lineas() == null || datos.lineas().isEmpty()) {
            throw new BusinessException("SIN_DETALLE", "La factura necesita al menos una línea");
        }

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<factura id=\"comprobante\" version=\"1.1.0\">\n");

        infoTributaria(xml, datos.claveAcceso(), datos.emisor());
        List<Totales> totales = infoFactura(xml, datos);
        detalles(xml, datos);
        infoAdicional(xml, datos.adicionales());

        xml.append("</factura>");
        return xml.toString();
    }

    /**
     * Nota de crédito v1.1.0 (plan §3.9): mismo orden de elementos que el
     * esquema {@code sri/NotaCredito_V1.1.0.xsd}, con la referencia al
     * documento original y el motivo obligatorios.
     */
    public static String generarNotaCredito(DatosNotaCredito datos) {
        if (datos.lineas() == null || datos.lineas().isEmpty()) {
            throw new BusinessException("SIN_DETALLE", "La nota de crédito necesita al menos una línea");
        }
        String motivo = datos.motivo() == null ? "" : datos.motivo().trim();
        if (motivo.isEmpty()) {
            throw new BusinessException("MOTIVO_REQUERIDO", "La nota de crédito necesita un motivo");
        }

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<notaCredito id=\"comprobante\" version=\"1.1.0\">\n");

        infoTributaria(xml, datos.claveAcceso(), datos.emisor());
        infoNotaCredito(xml, datos, motivo);
        detallesNotaCredito(xml, datos);
        infoAdicional(xml, datos.adicionales());

        xml.append("</notaCredito>");
        return xml.toString();
    }

    /** Total por tarifa: base e impuesto ya redondeados a 2 decimales. */
    private record Totales(int codigo, int codigoPorcentaje, BigDecimal tarifa,
                           BigDecimal base, BigDecimal valor) {
    }

    private static void infoTributaria(StringBuilder xml, String claveAcceso, DatosFactura.Emisor e) {
        abrir(xml, "infoTributaria");
        tag(xml, "ambiente", ClaveAccesoService.codigoAmbiente(e.ambiente()));
        tag(xml, "tipoEmision", ClaveAccesoService.codigoEmision(e.tipoEmision()));
        tag(xml, "razonSocial", e.razonSocial());
        opcional(xml, "nombreComercial", e.nombreComercial());
        tag(xml, "ruc", e.ruc());
        tag(xml, "claveAcceso", claveAcceso);
        tag(xml, "codDoc", e.codDoc());
        tag(xml, "estab", e.estab());
        tag(xml, "ptoEmi", e.ptoEmi());
        tag(xml, "secuencial", String.format("%09d", e.secuencial()));
        tag(xml, "dirMatriz", e.dirMatriz());
        opcional(xml, "agenteRetencion", e.agenteRetencion());
        cerrar(xml, "infoTributaria");
    }

    private static List<Totales> infoFactura(StringBuilder xml, DatosFactura datos) {
        DatosFactura.Emisor e = datos.emisor();
        List<Totales> totales = calcularTotales(datos.lineas());
        BigDecimal totalSinImpuestos = dinero(cumsum(totales, Totales::base));
        BigDecimal totalImpuestos = dinero(cumsum(totales, Totales::valor));
        BigDecimal totalDescuento = dinero(datos.lineas().stream()
                .map(l -> dinero(l.descuento()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal importeTotal = dinero(totalSinImpuestos.add(totalImpuestos));

        abrir(xml, "infoFactura");
        tag(xml, "fechaEmision", datos.fechaEmision().format(FECHA));
        opcional(xml, "dirEstablecimiento", e.dirEstablecimiento());
        tag(xml, "obligadoContabilidad", e.obligadoContabilidad() ? "SI" : "NO");
        tag(xml, "tipoIdentificacionComprador", datos.comprador().tipoIdentificacion());
        tag(xml, "razonSocialComprador", datos.comprador().razonSocial());
        tag(xml, "identificacionComprador", datos.comprador().identificacion());
        opcional(xml, "direccionComprador", datos.comprador().direccion());
        tag(xml, "totalSinImpuestos", totalSinImpuestos.toPlainString());
        tag(xml, "totalDescuento", totalDescuento.toPlainString());

        abrir(xml, "totalConImpuestos");
        for (Totales t : totales) {
            abrir(xml, "totalImpuesto");
            tag(xml, "codigo", String.valueOf(t.codigo()));
            tag(xml, "codigoPorcentaje", String.valueOf(t.codigoPorcentaje()));
            tag(xml, "baseImponible", t.base().toPlainString());
            tag(xml, "tarifa", t.tarifa().setScale(2, RoundingMode.HALF_UP).toPlainString());
            tag(xml, "valor", t.valor().toPlainString());
            cerrar(xml, "totalImpuesto");
        }
        cerrar(xml, "totalConImpuestos");

        tag(xml, "importeTotal", importeTotal.toPlainString());
        tag(xml, "moneda", "USD");

        if (datos.pagos() != null && !datos.pagos().isEmpty()) {
            abrir(xml, "pagos");
            for (DatosFactura.Pago pago : datos.pagos()) {
                abrir(xml, "pago");
                tag(xml, "formaPago", pago.formaPago());
                tag(xml, "total", dinero(pago.total()).toPlainString());
                if (pago.plazo() != null) tag(xml, "plazo", dinero(pago.plazo()).toPlainString());
                opcional(xml, "unidadTiempo", pago.unidadTiempo());
                cerrar(xml, "pago");
            }
            cerrar(xml, "pagos");
        }
        cerrar(xml, "infoFactura");
        return totales;
    }

    /**
     * {@code <infoNotaCredito>} (plan §3.9): la referencia al documento
     * original, el motivo y los totales calculados desde las líneas.
     */
    private static void infoNotaCredito(StringBuilder xml, DatosNotaCredito datos, String motivo) {
        DatosFactura.Emisor e = datos.emisor();
        List<Totales> totales = calcularTotales(datos.lineas());
        BigDecimal totalSinImpuestos = dinero(cumsum(totales, Totales::base));
        BigDecimal totalImpuestos = dinero(cumsum(totales, Totales::valor));
        BigDecimal valorModificacion = dinero(totalSinImpuestos.add(totalImpuestos));

        abrir(xml, "infoNotaCredito");
        tag(xml, "fechaEmision", datos.fechaEmision().format(FECHA));
        opcional(xml, "dirEstablecimiento", e.dirEstablecimiento());
        tag(xml, "tipoIdentificacionComprador", datos.comprador().tipoIdentificacion());
        tag(xml, "razonSocialComprador", datos.comprador().razonSocial());
        tag(xml, "identificacionComprador", datos.comprador().identificacion());
        tag(xml, "obligadoContabilidad", e.obligadoContabilidad() ? "SI" : "NO");
        tag(xml, "codDocModificado", datos.codDocModificado());
        tag(xml, "numDocModificado", datos.numDocModificado());
        tag(xml, "fechaEmisionDocSustento", datos.fechaEmisionDocSustento().format(FECHA));
        tag(xml, "totalSinImpuestos", totalSinImpuestos.toPlainString());
        tag(xml, "valorModificacion", valorModificacion.toPlainString());
        tag(xml, "moneda", "USD");

        abrir(xml, "totalConImpuestos");
        for (Totales t : totales) {
            abrir(xml, "totalImpuesto");
            tag(xml, "codigo", String.valueOf(t.codigo()));
            tag(xml, "codigoPorcentaje", String.valueOf(t.codigoPorcentaje()));
            tag(xml, "baseImponible", t.base().toPlainString());
            tag(xml, "valor", t.valor().toPlainString());
            cerrar(xml, "totalImpuesto");
        }
        cerrar(xml, "totalConImpuestos");

        tag(xml, "motivo", motivo);
        cerrar(xml, "infoNotaCredito");
    }

    /** Detalle de la nota: mismas líneas que la factura que se modifica. */
    private static void detallesNotaCredito(StringBuilder xml, DatosNotaCredito datos) {
        abrir(xml, "detalles");
        for (DatosFactura.Linea linea : datos.lineas()) {
            BigDecimal bruto = linea.cantidad().multiply(linea.precioUnitario());
            BigDecimal base = dinero(bruto.subtract(linea.descuento()));
            if (base.signum() < 0) {
                throw new BusinessException("LINEA_NEGATIVA",
                        "El total de la línea «" + linea.descripcion() + "» no puede ser negativo");
            }
            BigDecimal impuesto = impuestoDe(linea, base);

            abrir(xml, "detalle");
            opcional(xml, "codigoInterno", linea.codigoPrincipal());
            tag(xml, "descripcion", linea.descripcion());
            tag(xml, "cantidad", cantidad(linea.cantidad()));
            tag(xml, "precioUnitario", dinero(linea.precioUnitario()).toPlainString());
            tag(xml, "descuento", dinero(linea.descuento()).toPlainString());
            tag(xml, "precioTotalSinImpuesto", base.toPlainString());
            abrir(xml, "impuestos");
            abrir(xml, "impuesto");
            tag(xml, "codigo", String.valueOf(linea.codigoImpuesto()));
            tag(xml, "codigoPorcentaje", String.valueOf(linea.codigoPorcentaje()));
            tag(xml, "tarifa", linea.tarifa().setScale(2, RoundingMode.HALF_UP).toPlainString());
            tag(xml, "baseImponible", base.toPlainString());
            tag(xml, "valor", impuesto.toPlainString());
            cerrar(xml, "impuesto");
            cerrar(xml, "impuestos");
            cerrar(xml, "detalle");
        }
        cerrar(xml, "detalles");
    }

    private static void detalles(StringBuilder xml, DatosFactura datos) {
        abrir(xml, "detalles");
        for (DatosFactura.Linea linea : datos.lineas()) {
            BigDecimal bruto = linea.cantidad().multiply(linea.precioUnitario());
            BigDecimal base = dinero(bruto.subtract(linea.descuento()));
            if (base.signum() < 0) {
                throw new BusinessException("LINEA_NEGATIVA",
                        "El total de la línea «" + linea.descripcion() + "» no puede ser negativo");
            }
            BigDecimal impuesto = impuestoDe(linea, base);

            abrir(xml, "detalle");
            opcional(xml, "codigoPrincipal", linea.codigoPrincipal());
            tag(xml, "descripcion", linea.descripcion());
            tag(xml, "cantidad", cantidad(linea.cantidad()));
            tag(xml, "precioUnitario", dinero(linea.precioUnitario()).toPlainString());
            tag(xml, "descuento", dinero(linea.descuento()).toPlainString());
            tag(xml, "precioTotalSinImpuesto", base.toPlainString());
            abrir(xml, "impuestos");
            abrir(xml, "impuesto");
            tag(xml, "codigo", String.valueOf(linea.codigoImpuesto()));
            tag(xml, "codigoPorcentaje", String.valueOf(linea.codigoPorcentaje()));
            tag(xml, "tarifa", linea.tarifa().setScale(2, RoundingMode.HALF_UP).toPlainString());
            tag(xml, "baseImponible", base.toPlainString());
            tag(xml, "valor", impuesto.toPlainString());
            cerrar(xml, "impuesto");
            cerrar(xml, "impuestos");
            cerrar(xml, "detalle");
        }
        cerrar(xml, "detalles");
    }

    private static void infoAdicional(StringBuilder xml, List<DatosFactura.CampoAdicional> campos) {
        if (campos == null || campos.isEmpty()) return;
        if (campos.size() > 15) {
            throw new BusinessException("CAMPOS_ADICIONALES",
                    "El comprobante admite máximo 15 campos adicionales");
        }
        abrir(xml, "infoAdicional");
        for (DatosFactura.CampoAdicional campo : campos) {
            xml.append("  <campoAdicional nombre=\"").append(esc(campo.nombre())).append("\">")
                    .append(esc(campo.valor())).append("</campoAdicional>\n");
        }
        cerrar(xml, "infoAdicional");
    }

    /** Impuestos agrupados por código y tarifa, en el orden de las líneas. */
    private static List<Totales> calcularTotales(List<DatosFactura.Linea> lineas) {
        Map<String, Totales> grupos = new LinkedHashMap<>();
        for (DatosFactura.Linea linea : lineas) {
            BigDecimal bruto = linea.cantidad().multiply(linea.precioUnitario());
            BigDecimal base = dinero(bruto.subtract(linea.descuento()));
            if (base.signum() < 0) {
                throw new BusinessException("LINEA_NEGATIVA",
                        "El total de la línea «" + linea.descripcion() + "» no puede ser negativo");
            }
            BigDecimal valor = impuestoDe(linea, base);
            String clave = linea.codigoImpuesto() + "|" + linea.codigoPorcentaje();
            Totales previo = grupos.get(clave);
            if (previo != null) {
                if (previo.tarifa().compareTo(linea.tarifa()) != 0) {
                    throw new BusinessException("TARIFA_INCONSISTENTE",
                            "Dos líneas usan el mismo código de porcentaje con tarifas distintas");
                }
                grupos.put(clave, new Totales(previo.codigo(), previo.codigoPorcentaje(), previo.tarifa(),
                        dinero(previo.base().add(base)), dinero(previo.valor().add(valor))));
            } else {
                grupos.put(clave, new Totales(linea.codigoImpuesto(), linea.codigoPorcentaje(),
                        linea.tarifa(), base, valor));
            }
        }
        return List.copyOf(grupos.values());
    }

    private static BigDecimal cumsum(List<Totales> totales, Function<Totales, BigDecimal> campo) {
        return totales.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Impuesto de una línea: el que trae la línea si viene calculado
     * (precios con IVA incluido), y en caso contrario {@code base × tarifa}.
     * Se admite una desviación de dos centavos, que es lo que puede
     * separar al redondeo de la división 1,15 del redondeo del 15%.
     */
    private static BigDecimal impuestoDe(DatosFactura.Linea linea, BigDecimal base) {
        BigDecimal canonico = dinero(base.multiply(linea.tarifa()).divide(CIEN, 2, RoundingMode.HALF_UP));
        BigDecimal impuesto = linea.impuesto() == null ? canonico : dinero(linea.impuesto());
        if (impuesto.signum() < 0 || impuesto.subtract(canonico).abs().compareTo(new BigDecimal("0.02")) > 0) {
            throw new BusinessException("IMPUESTO_INCONSISTENTE",
                    "El impuesto de la línea «" + linea.descripcion() + "» no corresponde al "
                            + linea.tarifa().setScale(2, RoundingMode.HALF_UP) + "% de su base");
        }
        return impuesto;
    }

    private static String cantidad(BigDecimal valor) {
        BigDecimal v = valor.stripTrailingZeros();
        if (v.scale() > 6) v = v.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
        return v.scale() < 0 ? v.setScale(0).toPlainString() : v.toPlainString();
    }

    private static void abrir(StringBuilder xml, String etiqueta) {
        xml.append("  <").append(etiqueta).append(">\n");
    }

    private static void cerrar(StringBuilder xml, String etiqueta) {
        xml.append("  </").append(etiqueta).append(">\n");
    }

    private static void tag(StringBuilder xml, String etiqueta, String valor) {
        xml.append("    <").append(etiqueta).append('>').append(esc(valor))
                .append("</").append(etiqueta).append(">\n");
    }

    private static void opcional(StringBuilder xml, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) tag(xml, etiqueta, valor);
    }

    private static String esc(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

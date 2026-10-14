package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Construye los datos de una nota de crédito a partir del comprobante que
 * modifica (plan §3.6 {@code NotaCreditoBuilder}, §3.9).
 *
 * <p><b>El XML autorizado de la factura original es la fuente de verdad</b>:
 * de ahí salen el comprador, el establecimiento y las líneas con sus
 * impuestos, así que la nota nunca se desvía de lo que se emitió. Sólo se
 * cambia lo propio del documento nuevo (clave de acceso, fecha, secuencial).
 *
 * <p>Dos modos de trabajo:
 * <ul>
 *   <li><b>Devolución total</b> (sin líneas): la nota arrastra todas las
 *       líneas de la factura y su importe coincide con la factura.</li>
 *   <li><b>Devolución parcial</b>: se indican la línea (1-based) y la
 *       cantidad devuelta; se copia la línea original con esa cantidad.</li>
 * </ul>
 */
public final class NotaCreditoBuilder {

    private NotaCreditoBuilder() {
    }

    /** Devolución parcial: número de línea original (empezando en 1) y cantidad. */
    public record Devolucion(int linea, BigDecimal cantidad) {
    }

    public record Entrada(
            String claveAcceso,
            LocalDate fechaEmision,
            DatosFactura.Emisor emisor,
            String xmlOriginal,
            String motivo,
            List<Devolucion> devoluciones
    ) {
    }

    /** Nota lista para generar su XML; el total es el importe con IVA. */
    public record NotaConstruida(DatosNotaCredito datos, BigDecimal total) {
    }

    /** Una línea del XML original, con su impuesto tal como se facturó. */
    private record LineaOriginal(
            String codigo,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            int codigoImpuesto,
            int codigoPorcentaje,
            BigDecimal tarifa,
            BigDecimal impuesto
    ) {
    }

    public static NotaConstruida construir(Entrada entrada) {
        String motivo = entrada.motivo() == null ? "" : entrada.motivo().trim();
        if (motivo.isEmpty()) {
            throw new BusinessException("MOTIVO_REQUERIDO", "La nota de crédito necesita un motivo");
        }
        if (motivo.length() > 300) {
            throw new BusinessException("MOTIVO_INVALIDO", "El motivo no puede superar los 300 caracteres");
        }
        if (entrada.emisor() == null || entrada.claveAcceso() == null) {
            throw new BusinessException("FACTURACION_INCOMPLETA",
                    "Faltan datos fiscales para emitir la nota de crédito");
        }

        Original original = leer(entrada.xmlOriginal());
        List<DatosFactura.Linea> lineas = seleccionar(original, entrada.devoluciones());
        DatosNotaCredito datos = new DatosNotaCredito(
                entrada.claveAcceso(),
                entrada.fechaEmision(),
                entrada.emisor(),
                original.comprador(),
                ClaveAccesoService.COD_DOC_FACTURA,
                original.numeroComprobante(),
                original.fechaEmision(),
                lineas,
                motivo,
                original.adicionales());
        return new NotaConstruida(datos, total(lineas));
    }

    // ------------------------------------------------------------------
    // Selección de líneas
    // ------------------------------------------------------------------

    private static List<DatosFactura.Linea> seleccionar(Original original, List<Devolucion> devoluciones) {
        if (devoluciones == null || devoluciones.isEmpty()) {
            List<DatosFactura.Linea> completas = new ArrayList<>();
            for (LineaOriginal linea : original.lineas()) {
                completas.add(completa(linea));
            }
            return List.copyOf(completas);
        }
        Set<Integer> tocadas = new LinkedHashSet<>();
        List<DatosFactura.Linea> lineas = new ArrayList<>();
        for (Devolucion devolucion : devoluciones) {
            if (devolucion == null || devolucion.linea() < 1) {
                throw new BusinessException("LINEA_INVALIDA", "La línea a devolver no existe");
            }
            if (!tocadas.add(devolucion.linea())) {
                throw new BusinessException("LINEA_DUPLICADA",
                        "La línea " + devolucion.linea() + " se repite en la devolución");
            }
            if (devolucion.linea() > original.lineas().size()) {
                throw new BusinessException("LINEA_INVALIDA",
                        "La factura tiene " + original.lineas().size()
                                + " líneas y se pidió devolver la " + devolucion.linea());
            }
            if (devolucion.cantidad() == null || devolucion.cantidad().signum() <= 0) {
                throw new BusinessException("CANTIDAD_INVALIDA",
                        "La cantidad a devolver de la línea " + devolucion.linea() + " debe ser mayor que cero");
            }
            LineaOriginal original_ = original.lineas().get(devolucion.linea() - 1);
            if (devolucion.cantidad().compareTo(original_.cantidad()) > 0) {
                throw new BusinessException("CANTIDAD_EXCEDE",
                        "La línea " + devolucion.linea() + " sólo tiene " + original_.cantidad().toPlainString()
                                + " unidades y se intentan devolver " + devolucion.cantidad().toPlainString());
            }
            lineas.add(reducir(original_, devolucion.cantidad()));
        }
        return List.copyOf(lineas);
    }

    /** Copia la línea tal cual se facturó, con su impuesto original. */
    private static DatosFactura.Linea completa(LineaOriginal original) {
        return new DatosFactura.Linea(original.codigo(), original.descripcion(), original.cantidad(),
                original.precioUnitario(), original.descuento(), original.codigoImpuesto(),
                original.codigoPorcentaje(), original.tarifa(), original.impuesto());
    }

    /** Copia la línea con la cantidad devuelta; el impuesto se recalcula. */
    private static DatosFactura.Linea reducir(LineaOriginal original, BigDecimal cantidad) {
        BigDecimal bruto = dinero(cantidad.multiply(original.precioUnitario()));
        BigDecimal descuento = dinero(original.descuento() == null || original.cantidad().signum() <= 0
                ? BigDecimal.ZERO
                : original.descuento().multiply(cantidad).divide(original.cantidad(), 2, RoundingMode.HALF_UP));
        BigDecimal base = dinero(bruto.subtract(descuento));
        if (base.signum() <= 0) {
            throw new BusinessException("MONTO_INVALIDO",
                    "La devolución de «" + original.descripcion() + "» quedaría en cero");
        }
        return new DatosFactura.Linea(original.codigo(), original.descripcion(), cantidad,
                original.precioUnitario(), descuento, original.codigoImpuesto(),
                original.codigoPorcentaje(), original.tarifa(), null);
    }

    private static BigDecimal total(List<DatosFactura.Linea> lineas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (DatosFactura.Linea linea : lineas) {
            BigDecimal base = dinero(linea.cantidad().multiply(linea.precioUnitario())
                    .subtract(linea.descuento()));
            BigDecimal impuesto = linea.impuesto() != null ? dinero(linea.impuesto())
                    : dinero(base.multiply(linea.tarifa()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            suma = suma.add(base.add(impuesto));
        }
        return dinero(suma);
    }

    // ------------------------------------------------------------------
    // Lectura del XML original
    // ------------------------------------------------------------------

    /** Lo que se arrastra desde la factura que se modifica. */
    private record Original(
            String numeroComprobante,
            LocalDate fechaEmision,
            DatosFactura.Comprador comprador,
            List<LineaOriginal> lineas,
            List<DatosFactura.CampoAdicional> adicionales
    ) {
    }

    private static Original leer(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new BusinessException("COMPROBANTE_SIN_XML",
                    "El comprobante original no tiene XML para emitir la nota de crédito");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            org.w3c.dom.Document documento = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element raiz = documento.getDocumentElement();
            Element info = hijo(raiz, "infoFactura");
            if (info == null) {
                throw new BusinessException("COMPROBANTE_INVALIDO",
                        "El XML original no corresponde a una factura electrónica");
            }
            return new Original(
                    numeroComprobante(raiz),
                    fecha(texto(info, "fechaEmision")),
                    new DatosFactura.Comprador(
                            exigirTexto(info, "tipoIdentificacionComprador"),
                            exigirTexto(info, "razonSocialComprador"),
                            exigirTexto(info, "identificacionComprador"),
                            texto(info, "direccionComprador")),
                    lineas(raiz),
                    adicionales(raiz));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("COMPROBANTE_INVALIDO",
                    "No se pudo leer el XML del comprobante original: " + e.getMessage());
        }
    }

    /**
     * Número del documento sustento: el XML sólo trae el secuencial de 9
     * dígitos dentro de {@code infoFactura}; el número visible
     * ({@code estab-pto-secuencial}) se reconstruye desde
     * {@code infoTributaria}, que es el que espera el XSD.
     */
    private static String numeroComprobante(Element raiz) {
        Element tributaria = hijo(raiz, "infoTributaria");
        String secuencial = texto(tributaria, "secuencial");
        if (secuencial == null) {
            throw new BusinessException("COMPROBANTE_INVALIDO", "El XML original no tiene secuencial");
        }
        String estab = texto(tributaria, "estab");
        String ptoEmi = texto(tributaria, "ptoEmi");
        return estab + "-" + ptoEmi + "-" + secuencial;
    }

    private static List<LineaOriginal> lineas(Element raiz) {
        Element detalles = hijo(raiz, "detalles");
        if (detalles == null) {
            throw new BusinessException("COMPROBANTE_INVALIDO", "El XML original no tiene líneas");
        }
        List<LineaOriginal> resultado = new ArrayList<>();
        for (Element detalle : hijos(detalles, "detalle")) {
            Element impuesto = hijo(hijo(detalle, "impuestos"), "impuesto");
            resultado.add(new LineaOriginal(
                    texto(detalle, "codigoPrincipal"),
                    exigirTexto(detalle, "descripcion"),
                    exigirNumero(detalle, "cantidad"),
                    exigirNumero(detalle, "precioUnitario"),
                    numero(detalle, "descuento") == null ? BigDecimal.ZERO : numero(detalle, "descuento"),
                    codigoImpuesto(impuesto),
                    porcentaje(impuesto),
                    impuesto == null || numero(impuesto, "tarifa") == null
                            ? FacturaBuilder.TARIFA_IVA : numero(impuesto, "tarifa"),
                    impuesto == null ? null : numero(impuesto, "valor")));
        }
        if (resultado.isEmpty()) {
            throw new BusinessException("COMPROBANTE_INVALIDO", "El XML original no tiene líneas");
        }
        return List.copyOf(resultado);
    }

    private static int codigoImpuesto(Element impuesto) {
        if (impuesto == null) {
            return FacturaBuilder.CODIGO_IMPUESTO_IVA;
        }
        try {
            return Integer.parseInt(exigirTexto(impuesto, "codigo"));
        } catch (NumberFormatException e) {
            return FacturaBuilder.CODIGO_IMPUESTO_IVA;
        }
    }

    private static int porcentaje(Element impuesto) {
        if (impuesto == null) {
            return FacturaBuilder.CODIGO_PORCENTAJE_IVA;
        }
        String codigo = texto(impuesto, "codigoPorcentaje");
        if (codigo == null) {
            return FacturaBuilder.CODIGO_PORCENTAJE_IVA;
        }
        try {
            return Integer.parseInt(codigo);
        } catch (NumberFormatException e) {
            return FacturaBuilder.CODIGO_PORCENTAJE_IVA;
        }
    }

    private static List<DatosFactura.CampoAdicional> adicionales(Element raiz) {
        Element contenedor = hijo(raiz, "infoAdicional");
        if (contenedor == null) {
            return List.of();
        }
        List<DatosFactura.CampoAdicional> resultado = new ArrayList<>();
        for (Element campo : hijos(contenedor, "campoAdicional")) {
            String valor = campo.getTextContent() == null ? "" : campo.getTextContent().trim();
            resultado.add(new DatosFactura.CampoAdicional(campo.getAttribute("nombre"), valor));
        }
        return List.copyOf(resultado);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("COMPROBANTE_INVALIDO", "El XML original no tiene fecha de emisión");
        }
        try {
            return LocalDate.parse(valor, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException("COMPROBANTE_INVALIDO",
                    "La fecha del comprobante original no es válida: " + valor);
        }
    }

    private static Element hijo(Node padre, String etiqueta) {
        if (padre == null) {
            return null;
        }
        NodeList nodos = padre.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo.getNodeType() == Node.ELEMENT_NODE && etiqueta.equals(nodo.getNodeName())) {
                return (Element) nodo;
            }
        }
        return null;
    }

    private static List<Element> hijos(Element padre, String etiqueta) {
        List<Element> resultado = new ArrayList<>();
        if (padre == null) {
            return resultado;
        }
        NodeList nodos = padre.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo.getNodeType() == Node.ELEMENT_NODE && etiqueta.equals(nodo.getNodeName())) {
                resultado.add((Element) nodo);
            }
        }
        return resultado;
    }

    private static String texto(Element padre, String etiqueta) {
        Element elemento = hijo(padre, etiqueta);
        if (elemento == null || elemento.getTextContent() == null) {
            return null;
        }
        String valor = elemento.getTextContent().trim();
        return valor.isEmpty() ? null : valor;
    }

    private static String exigirTexto(Element padre, String etiqueta) {
        String valor = texto(padre, etiqueta);
        if (valor == null) {
            throw new BusinessException("COMPROBANTE_INVALIDO",
                    "El XML original no tiene <" + etiqueta + ">");
        }
        return valor;
    }

    private static BigDecimal numero(Element padre, String etiqueta) {
        String valor = texto(padre, etiqueta);
        return valor == null ? null : new BigDecimal(valor);
    }

    private static BigDecimal exigirNumero(Element padre, String etiqueta) {
        BigDecimal valor = numero(padre, etiqueta);
        if (valor == null) {
            throw new BusinessException("COMPROBANTE_INVALIDO",
                    "El XML original no tiene <" + etiqueta + ">");
        }
        return valor;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

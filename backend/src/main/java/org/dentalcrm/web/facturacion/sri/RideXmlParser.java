package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el XML del comprobante (autorizado si existe, firmado si no) y lo
 * traduce a {@link RideDatos}. Se recorren sólo hijos directos —nunca
 * descendientes— para no mezclar los datos del comprobante con los del
 * bloque {@code ds:Signature}.
 *
 * <p>Acepta tanto la factura ({@code infoFactura}) como la nota de crédito
 * ({@code infoNotaCredito}, plan §3.9); donde no hay factura el RIDE usa
 * el equivalente: el importe total es {@code valorModificacion}, el
 * descuento no existe y el código de línea viene en {@code codigoInterno}.
 */
public final class RideXmlParser {

    private RideXmlParser() {
    }

    public static RideDatos parsear(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new BusinessException("COMPROBANTE_SIN_XML",
                    "El comprobante no tiene XML para representar en el RIDE");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            org.w3c.dom.Document documento = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element raiz = documento.getDocumentElement();
            Element tributaria = exigir(raiz, "infoTributaria");
            Element info = hijo(raiz, "infoFactura") != null
                    ? exigir(raiz, "infoFactura")
                    : exigir(raiz, "infoNotaCredito");
            boolean esNotaCredito = hijo(raiz, "infoFactura") == null;

            return new RideDatos(
                    texto(tributaria, "claveAcceso"),
                    texto(info, "fechaEmision"),
                    texto(tributaria, "ambiente"),
                    texto(tributaria, "tipoEmision"),
                    texto(tributaria, "razonSocial"),
                    texto(tributaria, "nombreComercial"),
                    texto(tributaria, "ruc"),
                    texto(tributaria, "dirMatriz"),
                    texto(info, "dirEstablecimiento"),
                    texto(info, "obligadoContabilidad"),
                    texto(info, "tipoIdentificacionComprador"),
                    texto(info, "razonSocialComprador"),
                    texto(info, "identificacionComprador"),
                    texto(info, "direccionComprador"),
                    lineas(raiz),
                    numero(info, "totalSinImpuestos"),
                    numero(info, "totalDescuento"),
                    impuestos(info),
                    esNotaCredito ? numero(info, "valorModificacion") : numero(info, "importeTotal"),
                    pagos(info),
                    adicionales(raiz));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("RIDE_XML_INVALIDO",
                    "No se pudo leer el XML del comprobante: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Lectura del árbol
    // ------------------------------------------------------------------

    private static List<RideDatos.Linea> lineas(Element raiz) {
        Element detalles = hijo(raiz, "detalles");
        if (detalles == null) {
            return List.of();
        }
        List<RideDatos.Linea> resultado = new ArrayList<>();
        for (Element detalle : hijos(detalles, "detalle")) {
            Element impuesto = hijo(hijo(detalle, "impuestos"), "impuesto");
            resultado.add(new RideDatos.Linea(
                    texto(detalle, "codigoPrincipal"),
                    texto(detalle, "descripcion"),
                    numero(detalle, "cantidad"),
                    numero(detalle, "precioUnitario"),
                    numero(detalle, "descuento"),
                    numero(detalle, "precioTotalSinImpuesto"),
                    impuesto == null ? null : numero(impuesto, "tarifa"),
                    impuesto == null ? null : numero(impuesto, "valor")));
        }
        return List.copyOf(resultado);
    }

    private static List<RideDatos.Impuesto> impuestos(Element factura) {
        Element contenedor = hijo(factura, "totalConImpuestos");
        if (contenedor == null) {
            return List.of();
        }
        List<RideDatos.Impuesto> resultado = new ArrayList<>();
        for (Element total : hijos(contenedor, "totalImpuesto")) {
            BigDecimal base = numero(total, "baseImponible");
            BigDecimal valor = numero(total, "valor");
            BigDecimal tarifa = numero(total, "tarifa");
            // En la nota de crédito la tarifa no se emite: se deduce del importe.
            if (tarifa == null && base != null && valor != null && base.signum() > 0) {
                tarifa = valor.multiply(new BigDecimal("100")).divide(base, 2, java.math.RoundingMode.HALF_UP);
            }
            resultado.add(new RideDatos.Impuesto(tarifa, base, valor));
        }
        return List.copyOf(resultado);
    }

    private static List<RideDatos.Pago> pagos(Element factura) {
        Element contenedor = hijo(factura, "pagos");
        if (contenedor == null) {
            return List.of();
        }
        List<RideDatos.Pago> resultado = new ArrayList<>();
        for (Element pago : hijos(contenedor, "pago")) {
            resultado.add(new RideDatos.Pago(texto(pago, "formaPago"), numero(pago, "total")));
        }
        return List.copyOf(resultado);
    }

    private static List<RideDatos.Campo> adicionales(Element raiz) {
        Element contenedor = hijo(raiz, "infoAdicional");
        if (contenedor == null) {
            return List.of();
        }
        List<RideDatos.Campo> resultado = new ArrayList<>();
        for (Element campo : hijos(contenedor, "campoAdicional")) {
            String valor = campo.getTextContent() == null ? "" : campo.getTextContent().trim();
            resultado.add(new RideDatos.Campo(campo.getAttribute("nombre"), valor));
        }
        return List.copyOf(resultado);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Element exigir(Element padre, String etiqueta) {
        Element elemento = hijo(padre, etiqueta);
        if (elemento == null) {
            throw new BusinessException("RIDE_XML_INVALIDO",
                    "El XML no contiene el bloque <" + etiqueta + ">");
        }
        return elemento;
    }

    private static Element hijo(Element padre, String etiqueta) {
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

    private static BigDecimal numero(Element padre, String etiqueta) {
        String valor = texto(padre, etiqueta);
        return valor == null ? null : new BigDecimal(valor);
    }
}

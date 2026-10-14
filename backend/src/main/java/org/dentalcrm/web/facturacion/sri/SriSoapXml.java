package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Envelopes SOAP y lectura de las respuestas de los servicios offline del
 * SRI, descritos en su WSDL (document/literal, {@code elementFormDefault=
 * "unqualified}).
 *
 * <p>Se usa XPath con {@code local-name()} porque las respuestas del SRI
 * traen prefijos variables ({@code ns1:}, {@code soap:}, o ninguno).
 */
final class SriSoapXml {

    static final String NS_RECEPCION = "http://ec.gob.sri.ws.recepcion";
    static final String NS_AUTORIZACION = "http://ec.gob.sri.ws.autorizacion";
    static final String NS_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";

    private SriSoapXml() {
    }

    /** {@code validarComprobante}: el XML firmado viaja codificado en base64. */
    static String envelopeRecepcion(String xmlFirmadoBase64) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="%s" xmlns:rec="%s">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <rec:validarComprobante>
                      <xml>%s</xml>
                    </rec:validarComprobante>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(NS_SOAP, NS_RECEPCION, escapar(xmlFirmadoBase64));
    }

    /** {@code autorizacionComprobante}: consulta por clave de acceso. */
    static String envelopeAutorizacion(String claveAcceso) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="%s" xmlns:aut="%s">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <aut:autorizacionComprobante>
                      <claveAccesoComprobante>%s</claveAccesoComprobante>
                    </aut:autorizacionComprobante>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(NS_SOAP, NS_AUTORIZACION, escapar(claveAcceso));
    }

    static RespuestaRecepcion parseRecepcion(String cuerpo) {
        Document doc = documento(cuerpo);
        String estado = texto(doc,
                "//*[local-name()='RespuestaRecepcionComprobante']/*[local-name()='estado']");
        if (estado == null || estado.isBlank()) {
            throw invalida("la respuesta no trae el estado de recepción");
        }
        return new RespuestaRecepcion(estado.trim().toUpperCase(), mensajes(doc,
                "//*[local-name()='RespuestaRecepcionComprobante']"
                        + "/*[local-name()='comprobantes']/*[local-name()='comprobante']"
                        + "/*[local-name()='mensajes']/*[local-name()='mensaje']"));
    }

    static RespuestaAutorizacion parseAutorizacion(String cuerpo) {
        Document doc = documento(cuerpo);
        NodeList autorizaciones = nodos(doc,
                "//*[local-name()='RespuestaAutorizacionComprobante']"
                        + "/*[local-name()='autorizaciones']/*[local-name()='autorizacion']");
        if (autorizaciones.getLength() == 0) {
            // sin autorizaciones el SRI aún no procesó el comprobante (PPR)
            return new RespuestaAutorizacion(RespuestaAutorizacion.EN_PROCESO,
                    texto(doc, "//*[local-name()='RespuestaAutorizacionComprobante']/*[local-name()='claveAccesoConsultada']"),
                    null, null, null, null, List.of(), true);
        }
        Node autorizacion = autorizaciones.item(0);
        String estado = hijoTexto(autorizacion, "estado");
        if (estado == null || estado.isBlank()) {
            throw invalida("la autorización no trae estado");
        }
        return new RespuestaAutorizacion(
                estado.trim().toUpperCase(),
            texto(doc, "//*[local-name()='RespuestaAutorizacionComprobante']/*[local-name()='claveAccesoConsultada']"),
                hijoTexto(autorizacion, "numeroAutorizacion"),
                fecha(hijoTexto(autorizacion, "fechaAutorizacion")),
                hijoTexto(autorizacion, "ambiente"),
                hijoTexto(autorizacion, "comprobante"),
                mensajes(autorizacion, "*[local-name()='mensajes']/*[local-name()='mensaje']"),
                false);
    }

    /** {@code faultstring} del SOAP Fault, o null si la respuesta no es un fallo. */
    static String fault(String cuerpo) {
        try {
            return texto(documento(cuerpo), "//*[local-name()='faultstring']");
        } catch (BusinessException e) {
            return null;
        }
    }

    /** Primer trozo del cuerpo, para diagnosticar respuestas que no son XML. */
    static String recorte(String cuerpo) {
        String limpio = cuerpo == null ? "" : cuerpo.trim();
        return limpio.length() > 300 ? limpio.substring(0, 300) : limpio;
    }

    private static Document documento(String xml) {
        if (xml == null || xml.isBlank()) {
            throw invalida("respuesta vacía");
        }
        // el SRI a veces envuelve el XML en un documento HTML de error
        int inicio = xml.indexOf('<');
        if (inicio > 0) {
            xml = xml.substring(inicio);
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
        } catch (Exception e) {
            throw invalida("no se pudo leer el XML de respuesta: " + e.getMessage());
        }
    }

    private static String texto(Document doc, String expresion) {
        String valor = (String) evaluar(doc, expresion, XPathConstants.STRING);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static NodeList nodos(Document doc, String expresion) {
        return (NodeList) evaluar(doc, expresion, XPathConstants.NODESET);
    }

    private static List<MensajeSri> mensajes(Document doc, String expresion) {
        NodeList lista = nodos(doc, expresion);
        List<MensajeSri> resultado = new ArrayList<>();
        for (int i = 0; i < lista.getLength(); i++) {
            Node nodo = lista.item(i);
            resultado.add(new MensajeSri(
                    hijoTexto(nodo, "identificador"),
                    hijoTexto(nodo, "mensaje"),
                    hijoTexto(nodo, "informacionAdicional"),
                    hijoTexto(nodo, "tipo")));
        }
        return resultado;
    }

    private static List<MensajeSri> mensajes(Node nodo, String expresion) {
        NodeList lista = (NodeList) evaluar(nodo, expresion, XPathConstants.NODESET);
        List<MensajeSri> resultado = new ArrayList<>();
        for (int i = 0; i < lista.getLength(); i++) {
            Node hijo = lista.item(i);
            resultado.add(new MensajeSri(
                    hijoTexto(hijo, "identificador"),
                    hijoTexto(hijo, "mensaje"),
                    hijoTexto(hijo, "informacionAdicional"),
                    hijoTexto(hijo, "tipo")));
        }
        return resultado;
    }

    private static String hijoTexto(Node nodo, String nombre) {
        NodeList hijos = nodo.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.ELEMENT_NODE && nombre.equals(hijo.getLocalName())) {
                String texto = hijo.getTextContent();
                return texto == null ? null : texto.trim();
            }
        }
        return null;
    }

    private static Object evaluar(Node nodo, String expresion, javax.xml.namespace.QName tipo) {
        return evaluar((Object) nodo, expresion, tipo);
    }

    private static Object evaluar(Object contexto, String expresion, javax.xml.namespace.QName tipo) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate(expresion, contexto, tipo);
        } catch (XPathExpressionException e) {
            throw invalida("no se pudo leer la respuesta con la expresión " + expresion);
        }
    }

    private static Instant fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor.trim()).toInstant();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static BusinessException invalida(String detalle) {
        return new BusinessException("SRI_RESPUESTA_INVALIDA",
                "El SRI devolvió una respuesta que no se pudo interpretar: " + detalle);
    }
}

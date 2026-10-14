package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validación del XML contra el esquema oficial del SRI antes de firmar
 * (plan §3.5 "XML → XSD → Firma").
 *
 * <p>Los esquemas viven en {@code classpath:sri/}: {@code factura_V1.1.0.xsd}
 * (SHA-256 {@code 62db9bf0ecceb00ef2b7ed136e59224815e5e5e33c77efc6a0552001e052eb8b})
 * y {@code NotaCredito_V1.1.0.xsd} (SHA-256
 * {@code 379e9f270d71ab4578f130a736c9abe5eb1d7969375ecdef55d2973583ec5ab0}),
 * ambos descargados de la sección "Esquemas XSD Y XML" del portal de
 * facturación electrónica del SRI, más la importación
 * {@code xmldsig-core-schema.xsd}, que JAXP resuelve contra la URL del
 * esquema principal. El esquema se elige por el elemento raíz del
 * documento ({@code factura}, {@code notaCredito}, …), así que el mismo
 * validador sirve para todos los comprobantes de la Fase G/H. El bloque
 * {@code ds:Signature} es opcional en los esquemas, así que también se
 * puede validar un comprobante ya firmado.
 */
@Component
public class SriXmlValidator {

    private static final String ESQUEMA_FACTURA = "/sri/factura_V1.1.0.xsd";
    private static final String ESQUEMA_NOTA_CREDITO = "/sri/NotaCredito_V1.1.0.xsd";
    /** Claves en minúsculas: {@link #raizDe} normaliza el elemento raíz. */
    private static final String RAIZ_FACTURA = "factura";
    private static final String RAIZ_NOTA_CREDITO = "notacredito";

    private final Map<String, Schema> esquemas;

    public SriXmlValidator() {
        this.esquemas = Map.of(
                RAIZ_FACTURA, cargar(ESQUEMA_FACTURA),
                RAIZ_NOTA_CREDITO, cargar(ESQUEMA_NOTA_CREDITO));
    }

    private static Schema cargar(String ubicacion) {
        try {
            URL recurso = SriXmlValidator.class.getResource(ubicacion);
            if (recurso == null) {
                throw new IllegalStateException("No se encontró el esquema " + ubicacion);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // El esquema importa xmldsig-core-schema.xsd: sólo se permite
            // resolver esa importación desde el propio classpath (file en
            // desarrollo, jar/nested dentro del fat jar de Spring Boot),
            // nunca desde la red.
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar,nested");
            return factory.newSchema(new StreamSource(recurso.toString()));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el esquema " + ubicacion, e);
        }
    }

    /** Lanza {@code XML_INVALIDO} si el comprobante no cumple su XSD. */
    public void validar(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new BusinessException("XML_VACIO", "No hay comprobante para validar");
        }
        Schema esquema = esquemas.get(raizDe(xml));
        if (esquema == null) {
            throw new BusinessException("TIPO_COMPROBANTE_INVALIDO",
                    "El documento no corresponde a un comprobante electrónico soportado");
        }
        List<String> errores = new ArrayList<>();
        try {
            Validator validator = esquema.newValidator();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                }

                @Override
                public void error(SAXParseException exception) {
                    errores.add(descripcion(exception));
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    errores.add(descripcion(exception));
                }
            });
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new BusinessException("XML_INVALIDO",
                    "El comprobante no es XML válido: " + e.getMessage());
        }
        if (!errores.isEmpty()) {
            throw new BusinessException("XML_INVALIDO",
                    "El comprobante no cumple el esquema del SRI: " + errores.get(0)
                            + (errores.size() > 1 ? " (+" + (errores.size() - 1) + " errores)" : ""));
        }
    }

    /**
     * Elemento raíz del documento, en minúsculas y sin prefijos. Se saltan
     * la declaración {@code <?xml?>}, los comentarios y el DOCTYPE para
     * quedarse con el primer elemento real.
     */
    private static String raizDe(String xml) {
        int i = xml.indexOf('<');
        while (i >= 0 && i < xml.length() - 1) {
            char siguiente = xml.charAt(i + 1);
            if (siguiente == '?' || siguiente == '!') {
                int fin = xml.indexOf('>', i);
                if (fin < 0) {
                    return "";
                }
                i = xml.indexOf('<', fin + 1);
                continue;
            }
            if (Character.isWhitespace(siguiente)) {
                i = xml.indexOf('<', i + 1);
                continue;
            }
            int fin = i + 1;
            while (fin < xml.length() && !Character.isWhitespace(xml.charAt(fin))
                    && xml.charAt(fin) != '>' && xml.charAt(fin) != '/') {
                fin++;
            }
            String etiqueta = xml.substring(i + 1, fin);
            int dosPuntos = etiqueta.indexOf(':');
            return (dosPuntos >= 0 ? etiqueta.substring(dosPuntos + 1) : etiqueta).toLowerCase();
        }
        return "";
    }

    private static String descripcion(SAXParseException e) {
        String lugar = e.getLineNumber() > 0 ? "línea " + e.getLineNumber() : "posición desconocida";
        return lugar + ": " + e.getMessage();
    }
}

package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.IntegracionSecretCipher;
import org.dentalcrm.service.PrivateFileCipher;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Firma electrónica del comprobante — plan §3.6 {@code ElectronicSignatureService}.
 *
 * <p>Se distinguen dos modos que comparten la misma interfaz de uso:
 * <ul>
 *   <li><b>CERTIFICADO</b>: el consultorio tiene su .p12 (V24) y aquí se ha
 *       guardado su contraseña; se firma con XMLDSig enveloped, RSA-SHA1,
 *       como exige la ficha técnica del SRI.</li>
 *   <li><b>SIMULADA</b>: falta el certificado o la contraseña y el XML sale
 *       sin el bloque {@code <ds:Signature>}. Sirve para recorrer todo el
 *       flujo en local; el webservice del SRI nunca se llama en este modo
 *       (ver {@link AuthorizationService}, que sólo permite simular en el
 *       ambiente de pruebas).</li>
 * </ul>
 */
@Service
public class ElectronicSignatureService {

    public static final String MODO_SIMULADA = "SIMULADA";
    public static final String MODO_CERTIFICADO = "CERTIFICADO";
    public static final String PREFIJO_CIFRADO = "enc:v1:";

    private static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    private final PrivateFileCipher archivoCifrado;
    private final IntegracionSecretCipher secretos;

    public ElectronicSignatureService(PrivateFileCipher archivoCifrado, IntegracionSecretCipher secretos) {
        this.archivoCifrado = archivoCifrado;
        this.secretos = secretos;
    }

    /** XML resultante y modo de firma realmente usado ({@code SIMULADA} o {@code CERTIFICADO}). */
    public record FirmaResultado(String xml, String modo) {
    }

    public record CertificadoInfo(String titular, Instant validoDesde, Instant validoHasta) {
    }

    private record Credencial(PrivateKey llave, X509Certificate certificado) {
    }

    /** Valida el almacén antes de persistir su contraseña cifrada. */
    public CertificadoInfo validarCertificado(Consultorio consultorio, String password) {
        if (consultorio == null || consultorio.getFirmaDigital() == null
                || consultorio.getFirmaDigital().length == 0) {
            throw new BusinessException("FIRMA_CERTIFICADO_NO_CARGADO",
                    "Carga primero el archivo .p12 del emisor");
        }
        byte[] p12 = archivoCifrado.decrypt(consultorio.getFirmaDigital());
        Credencial credencial = cargarCredencial(p12, password);
        return new CertificadoInfo(credencial.certificado().getSubjectX500Principal().getName(),
                credencial.certificado().getNotBefore().toInstant(),
                credencial.certificado().getNotAfter().toInstant());
    }

    /**
     * @param xml                 comprobante ya validado contra el XSD
     * @param consultorio         aporta el .p12 cifrado (puede venir sin certificado)
     * @param passwordCifrada     contraseña del .p12 guardada en la configuración (cifrada)
     */
    public FirmaResultado firmar(String xml, Consultorio consultorio, String passwordCifrada) {
        if (consultorio == null || consultorio.getFirmaDigital() == null
                || passwordCifrada == null || passwordCifrada.isBlank()) {
            return new FirmaResultado(xml, MODO_SIMULADA);
        }
        byte[] certificado = archivoCifrado.decrypt(consultorio.getFirmaDigital());
        String password = descifrar(passwordCifrada);
        try {
            return new FirmaResultado(firmarConCertificado(xml, certificado, password), MODO_CERTIFICADO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("FIRMA_FALLIDA",
                    "No se pudo firmar el comprobante: " + mensaje(e));
        }
    }

    /** true si hay .p12 y contraseña: en ese caso la firma es real. */
    public static boolean firmaReal(Consultorio consultorio, String passwordCifrada) {
        return consultorio != null && consultorio.getFirmaDigital() != null
                && passwordCifrada != null && !passwordCifrada.isBlank();
    }

    private String firmarConCertificado(String xml, byte[] p12, String password) throws Exception {
        Credencial credencial = cargarCredencial(p12, password);
        PrivateKey llave = credencial.llave();
        X509Certificate certificado = credencial.certificado();

        Document documento = parsear(xml);
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        Reference referencia = factory.newReference("",
            factory.newDigestMethod(DigestMethod.SHA1, null),
            Collections.singletonList(factory.newTransform(Transform.ENVELOPED,
                (TransformParameterSpec) null)),
            null, null);
        SignedInfo firmado = factory.newSignedInfo(
            factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                (C14NMethodParameterSpec) null),
            factory.newSignatureMethod(RSA_SHA1, null),
            List.of(referencia));
        KeyInfoFactory claves = factory.getKeyInfoFactory();
        X509Data datos = claves.newX509Data(List.of(certificado));

        DOMSignContext contexto = new DOMSignContext(llave, documento.getDocumentElement());
        contexto.setDefaultNamespacePrefix("ds");
        XMLSignature firma = factory.newXMLSignature(firmado, claves.newKeyInfo(List.of(datos)));
        firma.sign(contexto);

        return serializar(documento);
        }

        private Credencial cargarCredencial(byte[] p12, String password) {
        KeyStore almacén;
        try {
            almacén = KeyStore.getInstance("PKCS12");
        } catch (java.security.KeyStoreException e) {
            throw new BusinessException("FIRMA_P12_INVALIDO", "No se pudo abrir el archivo PKCS#12");
        }
        try (ByteArrayInputStream entrada = new ByteArrayInputStream(p12)) {
            almacén.load(entrada, (password == null ? "" : password).toCharArray());
        } catch (java.io.IOException e) {
            throw new BusinessException("FIRMA_PASSWORD_INVALIDA",
                "La contraseña no es válida o el archivo PKCS#12 está dañado");
        } catch (Exception e) {
            throw new BusinessException("FIRMA_P12_INVALIDO",
                "No se pudo leer el archivo PKCS#12");
        }
        String alias;
        try {
            alias = primeraLlave(almacén);
        } catch (Exception e) {
            throw new BusinessException("FIRMA_SIN_LLAVE", "No se pudo leer el archivo PKCS#12");
        }
        if (alias == null) {
            throw new BusinessException("FIRMA_SIN_LLAVE", "El archivo .p12 no contiene una llave privada");
        }
        PrivateKey llave;
        try {
            llave = (PrivateKey) almacén.getKey(alias, (password == null ? "" : password).toCharArray());
        } catch (UnrecoverableKeyException e) {
            throw new BusinessException("FIRMA_PASSWORD_INVALIDA",
                    "La contraseña del certificado .p12 no es correcta");
        } catch (Exception e) {
            throw new BusinessException("FIRMA_SIN_LLAVE", "No se pudo leer la llave privada del .p12");
        }
        X509Certificate certificado;
        try {
            certificado = (X509Certificate) almacén.getCertificate(alias);
        } catch (java.security.KeyStoreException e) {
            throw new BusinessException("FIRMA_SIN_LLAVE", "No se pudo leer el certificado del .p12");
        }
        if (llave == null || certificado == null) {
            throw new BusinessException("FIRMA_SIN_LLAVE", "El certificado .p12 está incompleto");
        }
            try {
                certificado.checkValidity();
            } catch (java.security.cert.CertificateExpiredException e) {
                throw new BusinessException("FIRMA_CERTIFICADO_VENCIDO", "El certificado .p12 está vencido");
            } catch (java.security.cert.CertificateNotYetValidException e) {
                throw new BusinessException("FIRMA_CERTIFICADO_NO_VIGENTE", "El certificado .p12 aún no está vigente");
            }
            if (!"RSA".equalsIgnoreCase(llave.getAlgorithm())) {
                throw new BusinessException("FIRMA_ALGORITMO_INVALIDO",
                    "El certificado debe tener una llave privada RSA compatible con la firma SRI configurada");
            }
            boolean[] usos = certificado.getKeyUsage();
            if (usos != null && (usos.length == 0 || !usos[0])) {
                throw new BusinessException("FIRMA_USO_INVALIDO",
                    "El certificado no permite uso de firma digital");
            }
            return new Credencial(llave, certificado);
    }

    private static String primeraLlave(KeyStore almacén) throws Exception {
        java.util.Enumeration<String> aliases;
        try {
            aliases = almacén.aliases();
        } catch (java.security.KeyStoreException e) {
            throw new BusinessException("FIRMA_SIN_LLAVE", "No se pudo leer el archivo PKCS#12");
        }
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (almacén.isKeyEntry(alias)) return alias;
        }
        return null;
    }

    private static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica.newDocumentBuilder().parse(new InputSource(new java.io.StringReader(xml)));
    }

    /** Serializa sin sangrado: la indentación alteraría la forma canónica firmada. */
    private static String serializar(Document documento) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter salida = new StringWriter();
        transformer.transform(new DOMSource(documento), new StreamResult(salida));
        return salida.toString();
    }

    private String descifrar(String valor) {
        if (valor.startsWith(PREFIJO_CIFRADO)) {
            return secretos.decrypt(valor.substring(PREFIJO_CIFRADO.length()));
        }
        return valor;
    }

    private static String mensaje(Exception e) {
        String mensaje = e.getMessage();
        return mensaje == null || mensaje.isBlank() ? e.getClass().getSimpleName() : mensaje;
    }
}

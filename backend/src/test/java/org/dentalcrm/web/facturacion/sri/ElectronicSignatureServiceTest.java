package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.IntegracionSecretCipher;
import org.dentalcrm.service.PrivateFileCipher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ElectronicSignatureServiceTest {

    private static final String PASSWORD = "pruebas123";
    private static final String PREFIJO = ElectronicSignatureService.PREFIJO_CIFRADO;

    private final PrivateFileCipher archivos =
            new PrivateFileCipher("unit-test-signing-key-with-at-least-32-characters");
    private final IntegracionSecretCipher secretos =
            new IntegracionSecretCipher("unit-test-key-with-at-least-32-characters");
    private final ElectronicSignatureService servicio = new ElectronicSignatureService(archivos, secretos);
    private final SriXmlValidator validador = new SriXmlValidator();

    @Test
    void firmaSimuladaCuandoNoHayCertificado() {
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());

        var resultado = servicio.firmar(xml, new Consultorio(), null);

        assertEquals(ElectronicSignatureService.MODO_SIMULADA, resultado.modo());
        assertEquals(xml, resultado.xml());
        assertFalse(resultado.xml().contains("Signature"));
    }

    @Test
    void firmaSimuladaCuandoNoHayPassword() {
        Consultorio consultorio = consultorioConCertificado();
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());

        var resultado = servicio.firmar(xml, consultorio, null);

        assertEquals(ElectronicSignatureService.MODO_SIMULADA, resultado.modo());
    }

    @Test
    void firmaConCertificadoInsertaElBloqueDeFirma() {
        Consultorio consultorio = consultorioConCertificado();
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());
        String password = PREFIJO + secretos.encrypt(PASSWORD);

        var resultado = servicio.firmar(xml, consultorio, password);

        assertEquals(ElectronicSignatureService.MODO_CERTIFICADO, resultado.modo());
        assertTrue(resultado.xml().contains("Signature"), "debe llevar el bloque <ds:Signature>");
        assertTrue(resultado.xml().contains("http://www.w3.org/2000/09/xmldsig#"));
        assertDoesNotThrow(() -> validador.validar(resultado.xml()),
                "el comprobante firmado debe seguir cumpliendo el XSD");
        assertNotEquals(xml, resultado.xml());
    }

    @Test
    void passwordIncorrectaReportaElErrorDeLaFirma() {
        Consultorio consultorio = consultorioConCertificado();
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());
        String password = PREFIJO + secretos.encrypt("otra-clave");

        var error = assertThrows(BusinessException.class, () -> servicio.firmar(xml, consultorio, password));

        assertEquals("FIRMA_PASSWORD_INVALIDA", error.getCode());
    }

    @Test
    void firmaRealExigeCertificadoYPassword() {
        Consultorio conCertificado = consultorioConCertificado();
        Consultorio sinCertificado = new Consultorio();
        String password = PREFIJO + secretos.encrypt(PASSWORD);

        assertTrue(ElectronicSignatureService.firmaReal(conCertificado, password));
        assertFalse(ElectronicSignatureService.firmaReal(sinCertificado, password));
        assertFalse(ElectronicSignatureService.firmaReal(conCertificado, null));
        assertFalse(ElectronicSignatureService.firmaReal(null, password));
    }

    private Consultorio consultorioConCertificado() {
        Consultorio consultorio = new Consultorio();
        consultorio.setId(1L);
        consultorio.setNombre("Little Smile");
        consultorio.setFirmaDigital(archivos.encrypt(certificado()));
        consultorio.setFirmaDigitalNombre("firma-test.p12");
        return consultorio;
    }

    private byte[] certificado() {
        try (InputStream entrada = ElectronicSignatureServiceTest.class.getResourceAsStream("/firma-test.p12")) {
            assertNotNull(entrada, "falta src/test/resources/firma-test.p12");
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

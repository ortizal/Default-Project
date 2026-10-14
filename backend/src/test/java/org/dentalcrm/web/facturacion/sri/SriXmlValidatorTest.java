package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SriXmlValidatorTest {

    private final SriXmlValidator validador = new SriXmlValidator();

    @Test
    void aceptaUnComprobanteGenerado() {
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());

        assertDoesNotThrow(() -> validador.validar(xml));
    }

    @Test
    void rechazaXmlQueNoEsXml() {
        var error = assertThrows(BusinessException.class,
                () -> validador.validar("<factura><detalle></factura>"));

        assertEquals("XML_INVALIDO", error.getCode());
        assertTrue(error.getMessage().contains("no es XML válido"));
    }

    @Test
    void rechazaComprobanteSinLosCamposObligatorios() {
        String incompleto = """
                <?xml version="1.0" encoding="UTF-8"?>
                <factura id="comprobante" version="1.1.0">
                  <infoTributaria>
                    <ambiente>1</ambiente>
                  </infoTributaria>
                </factura>
                """;

        var error = assertThrows(BusinessException.class, () -> validador.validar(incompleto));

        assertEquals("XML_INVALIDO", error.getCode());
        assertTrue(error.getMessage().contains("línea"));
    }

    @Test
    void rechazaClaveDeAccesoQueNoTiene49Digitos() {
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());
        String malaClave = xml.replace("<claveAcceso>" + FixturesFactura.CLAVE + "</claveAcceso>",
                "<claveAcceso>123</claveAcceso>");

        var error = assertThrows(BusinessException.class, () -> validador.validar(malaClave));

        assertEquals("XML_INVALIDO", error.getCode());
    }

    @Test
    void rechazaXmlVacio() {
        var error = assertThrows(BusinessException.class, () -> validador.validar(" "));

        assertEquals("XML_VACIO", error.getCode());
    }
}

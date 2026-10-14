package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.TipoComprobanteSri;
import org.dentalcrm.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fase G.4b: RIDE del comprobante (plan §3.11). El parser se prueba con
 * un XML real del generador y el PDF sólo se comprueba que salga bien
 * formado: el contenido sale del propio XML, que ya tiene sus pruebas.
 */
class RideGeneratorTest {

    private final RideGenerator generator = new RideGenerator();

    @Test
    void leeDelXmlLosDatosQueElRideDebeMostrar() {
        RideDatos datos = RideXmlParser.parsear(xml());

        assertEquals("Little Smile S.A.", datos.razonSocialEmisor());
        assertEquals("Little Smile Odontología", datos.nombreComercial());
        assertEquals("1791234567001", datos.rucEmisor());
        assertEquals("08/10/2026", datos.fechaEmision());
        assertEquals("1", datos.ambiente());

        assertEquals("05", datos.tipoIdentificacionComprador());
        assertEquals("Juan Pérez", datos.razonSocialComprador());
        assertEquals("1712345678", datos.identificacionComprador());
        assertEquals("Calle Falsa 123", datos.direccionComprador());

        assertEquals(2, datos.lineas().size());
        assertEquals("Consulta odontológica", datos.lineas().get(0).descripcion());
        assertEquals(0, datos.lineas().get(1).descuento().compareTo(new BigDecimal("5.10")));

        assertEquals(1, datos.impuestos().size());
        assertEquals(0, datos.impuestos().get(0).tarifa().compareTo(new BigDecimal("15.00")));

        assertEquals(1, datos.pagos().size());
        assertEquals("01", datos.pagos().get(0).formaPago());

        assertEquals(1, datos.adicionales().size());
        assertEquals("Correo", datos.adicionales().get(0).nombre());
        assertEquals("juan@ejemplo.com", datos.adicionales().get(0).valor());

        // El total del XML es la suma de su base más su impuesto, al céntimo.
        assertEquals(0, datos.totalSinImpuestos()
                .add(datos.impuestos().get(0).valor())
                .compareTo(datos.importeTotal()));
    }

    @Test
    void ignoraLoQueHayaDentroDelBloqueDeFirma() {
        String xml = xml().replace("</factura>",
                "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
                        + "<Object><razonSocial>SOCIEDAD FALSA</razonSocial>"
                        + "<importeTotal>999.99</importeTotal></Object></ds:Signature>"
                        + "</factura>");

        RideDatos datos = RideXmlParser.parsear(xml);

        assertEquals("Little Smile S.A.", datos.razonSocialEmisor());
        assertNotEquals(0, datos.importeTotal().compareTo(new BigDecimal("999.99")));
    }

    @Test
    void generaUnPdfBienFormado() {
        byte[] pdf = generator.generar(comprobante());

        assertNotNull(pdf);
        assertTrue(pdf.length > 800, "el PDF pesa demasiado poco: " + pdf.length);
        assertTrue(cabecera(pdf).startsWith("%PDF"), "no empieza como PDF");
        assertTrue(cola(pdf).contains("%%EOF"), "no termina como PDF");
        assertEquals("ride-001-001-000000001.pdf", generator.nombreArchivo(comprobante()));
    }

    @Test
    void fallaSiElComprobanteNoTieneXml() {
        ComprobanteElectronico comprobante = new ComprobanteElectronico();
        comprobante.setNumeroComprobante("001-001-000000002");

        BusinessException error = assertThrows(BusinessException.class,
                () -> generator.generar(comprobante));

        assertEquals("COMPROBANTE_SIN_XML", error.getCode());
    }

    // ------------------------------------------------------------------

    private static String xml() {
        return SriXmlGenerator.generarFactura(FixturesFactura.factura());
    }

    private static ComprobanteElectronico comprobante() {
        ComprobanteElectronico comprobante = new ComprobanteElectronico();
        comprobante.setTipoComprobante(TipoComprobanteSri.FACTURA);
        comprobante.setNumeroComprobante("001-001-000000001");
        comprobante.setClaveAcceso(FixturesFactura.CLAVE);
        comprobante.setXmlFirmado(xml());
        comprobante.setSimulado(true);
        comprobante.setAutorizacionNumero(FixturesFactura.CLAVE);
        comprobante.setAutorizacionFecha(Instant.parse("2026-10-08T16:00:00Z"));
        return comprobante;
    }

    private static String cabecera(byte[] pdf) {
        return new String(pdf, 0, Math.min(8, pdf.length), StandardCharsets.ISO_8859_1);
    }

    private static String cola(byte[] pdf) {
        int desde = Math.max(0, pdf.length - 64);
        return new String(pdf, desde, pdf.length - desde, StandardCharsets.ISO_8859_1);
    }
}

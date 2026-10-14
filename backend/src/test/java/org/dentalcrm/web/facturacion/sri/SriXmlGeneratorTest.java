package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SriXmlGeneratorTest {

    private final SriXmlValidator validador = new SriXmlValidator();

    /** La factura de {@link FixturesFactura} devuelta entera como nota de crédito. */
    private static DatosNotaCredito notaDeCredito() {
        return new DatosNotaCredito(FixturesFactura.CLAVE, FixturesFactura.FECHA,
                notaEmisor(), FixturesFactura.comprador(), "01", "001-001-000000001",
                FixturesFactura.FECHA, FixturesFactura.factura().lineas(),
                "Se devuelve el tratamiento",
                List.of(new DatosFactura.CampoAdicional("Correo", "juan@ejemplo.com")));
    }

    private static DatosFactura.Emisor notaEmisor() {
        DatosFactura.Emisor factura = FixturesFactura.emisor();
        return new DatosFactura.Emisor(factura.razonSocial(), factura.nombreComercial(), factura.ruc(),
                factura.dirMatriz(), factura.ambiente(), factura.tipoEmision(),
                ClaveAccesoService.COD_DOC_NOTA_CREDITO, factura.estab(), factura.ptoEmi(),
                1, factura.dirEstablecimiento(), factura.obligadoContabilidad(),
                factura.agenteRetencion());
    }

    @Test
    void generaUnXmlQueCumpleElEsquemaDelSri() {
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());

        assertDoesNotThrow(() -> validador.validar(xml));
        assertTrue(xml.contains("<factura id=\"comprobante\" version=\"1.1.0\">"));
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.endsWith("</factura>"));
    }

    @Test
    void calculaSubtotalesImpuestosEImporteTotal() {
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());

        // línea 1: 1 × 100.00 = 100.00 · línea 2: 2 × 25.50 − 5.10 = 45.90
        assertTrue(xml.contains("<totalSinImpuestos>145.90</totalSinImpuestos>"));
        assertTrue(xml.contains("<totalDescuento>5.10</totalDescuento>"));
        assertTrue(xml.contains("<importeTotal>167.79</importeTotal>"));
        // impuestos agrupados en una sola tarifa (15%)
        assertTrue(xml.contains("<baseImponible>145.90</baseImponible>"));
        assertTrue(xml.contains("<valor>21.89</valor>"));
        assertTrue(xml.contains("<codigoPorcentaje>4</codigoPorcentaje>"));
        assertTrue(xml.contains("<tarifa>15.00</tarifa>"));
        assertTrue(xml.contains("<precioTotalSinImpuesto>45.90</precioTotalSinImpuesto>"));
        assertTrue(xml.contains("<descuento>5.10</descuento>"));
    }

    @Test
    void incluyeDatosTributariosDelEmisorYDelReceptor() {
        String xml = SriXmlGenerator.generarFactura(FixturesFactura.factura());

        assertTrue(xml.contains("<ambiente>1</ambiente>"));
        assertTrue(xml.contains("<tipoEmision>1</tipoEmision>"));
        assertTrue(xml.contains("<ruc>1791234567001</ruc>"));
        assertTrue(xml.contains("<claveAcceso>" + FixturesFactura.CLAVE + "</claveAcceso>"));
        assertTrue(xml.contains("<codDoc>01</codDoc>"));
        assertTrue(xml.contains("<secuencial>000000001</secuencial>"));
        assertTrue(xml.contains("<fechaEmision>08/10/2026</fechaEmision>"));
        assertTrue(xml.contains("<tipoIdentificacionComprador>05</tipoIdentificacionComprador>"));
        assertTrue(xml.contains("<identificacionComprador>1712345678</identificacionComprador>"));
        assertTrue(xml.contains("<formaPago>01</formaPago>"));
        assertTrue(xml.contains("<campoAdicional nombre=\"Correo\">juan@ejemplo.com</campoAdicional>"));
    }

    @Test
    void escapaCaracteresEspecialesDelXml() {
        DatosFactura factura = FixturesFactura.factura();
        DatosFactura conSimbolos = new DatosFactura(
                factura.claveAcceso(), factura.fechaEmision(), factura.emisor(), factura.comprador(),
                List.of(new DatosFactura.Linea("P-1", "Ortodoncia & \"brackets\" <suaves>", new BigDecimal("1"),
                        new BigDecimal("350.00"), BigDecimal.ZERO, 2, 4, new BigDecimal("15"))),
                factura.pagos(), factura.adicionales());

        String xml = SriXmlGenerator.generarFactura(conSimbolos);

        assertTrue(xml.contains("Ortodoncia &amp; &quot;brackets&quot; &lt;suaves&gt;"));
        assertDoesNotThrow(() -> validador.validar(xml));
    }

    @Test
    void rechazaFacturaSinLineas() {
        DatosFactura sinLineas = new DatosFactura(FixturesFactura.CLAVE, FixturesFactura.FECHA,
                FixturesFactura.factura().emisor(), FixturesFactura.factura().comprador(),
                List.of(), List.of(), List.of());

        var error = assertThrows(BusinessException.class, () -> SriXmlGenerator.generarFactura(sinLineas));

        assertEquals("SIN_DETALLE", error.getCode());
    }

    @Test
    void rechazaLineaConTotalNegativo() {
        DatosFactura negativa = new DatosFactura(FixturesFactura.CLAVE, FixturesFactura.FECHA,
                FixturesFactura.factura().emisor(), FixturesFactura.factura().comprador(),
                List.of(new DatosFactura.Linea("X", "Descuento mayor que la línea", new BigDecimal("1"),
                        new BigDecimal("10.00"), new BigDecimal("50.00"), 2, 4, new BigDecimal("15"))),
                List.of(), List.of());

        var error = assertThrows(BusinessException.class, () -> SriXmlGenerator.generarFactura(negativa));

        assertEquals("LINEA_NEGATIVA", error.getCode());
    }

    @Test
    void generaUnaNotaDeCreditoQueCumpleElEsquemaDelSri() {
        String xml = SriXmlGenerator.generarNotaCredito(notaDeCredito());

        assertDoesNotThrow(() -> validador.validar(xml));
        assertTrue(xml.contains("<notaCredito id=\"comprobante\" version=\"1.1.0\">"));
        assertTrue(xml.endsWith("</notaCredito>"));
        assertTrue(xml.contains("<codDoc>04</codDoc>"));
        assertTrue(xml.contains("<codDocModificado>01</codDocModificado>"));
        assertTrue(xml.contains("<numDocModificado>001-001-000000001</numDocModificado>"));
        assertTrue(xml.contains("<fechaEmisionDocSustento>08/10/2026</fechaEmisionDocSustento>"));
        assertTrue(xml.contains("<motivo>Se devuelve el tratamiento</motivo>"));
        assertTrue(xml.contains("<codigoInterno>CONS-001</codigoInterno>"));
        assertFalse(xml.contains("<codigoPrincipal>"), "la nota usa codigoInterno");
        assertFalse(xml.contains("<importeTotal>"));
        assertFalse(xml.contains("<pagos>"), "la nota no lleva formas de pago");
        assertFalse(xml.contains("<direccionComprador>"), "el esquema de la nota no lo admite");
    }

    @Test
    void laNotaDeCreditoRepiteLosTotalesDeLaFactura() {
        String xml = SriXmlGenerator.generarNotaCredito(notaDeCredito());

        assertTrue(xml.contains("<totalSinImpuestos>145.90</totalSinImpuestos>"));
        assertTrue(xml.contains("<valorModificacion>167.79</valorModificacion>"));
        assertTrue(xml.contains("<baseImponible>145.90</baseImponible>"));
        assertTrue(xml.contains("<valor>21.89</valor>"));
        assertFalse(xml.contains("<totalDescuento>"));
    }

    @Test
    void elTotalImpuestoDeLaNotaNoLlevaTarifa() {
        String xml = SriXmlGenerator.generarNotaCredito(notaDeCredito());

        int inicio = xml.indexOf("<totalImpuesto>");
        int fin = xml.indexOf("</totalImpuesto>", inicio);
        String bloque = xml.substring(inicio, fin);

        assertFalse(bloque.contains("<tarifa>"), "el esquema de la nota no admite tarifa ahí");
        assertTrue(xml.substring(inicio, fin).contains("<codigoPorcentaje>4</codigoPorcentaje>"));
        assertTrue(xml.contains("<tarifa>15.00</tarifa>"), "en el detalle sí se emite");
    }

    @Test
    void rechazaNotaDeCreditoSinMotivo() {
        DatosNotaCredito sinMotivo = new DatosNotaCredito(FixturesFactura.CLAVE, FixturesFactura.FECHA,
                notaEmisor(), FixturesFactura.comprador(), "01", "001-001-000000001",
                FixturesFactura.FECHA, FixturesFactura.factura().lineas(), "  ", List.of());

        var error = assertThrows(BusinessException.class, () -> SriXmlGenerator.generarNotaCredito(sinMotivo));

        assertEquals("MOTIVO_REQUERIDO", error.getCode());
    }

    @Test
    void rechazaNotaDeCreditoSinLineas() {
        DatosNotaCredito vacia = new DatosNotaCredito(FixturesFactura.CLAVE, FixturesFactura.FECHA,
                notaEmisor(), FixturesFactura.comprador(), "01", "001-001-000000001",
                FixturesFactura.FECHA, List.of(), "Se devuelve el tratamiento", List.of());

        var error = assertThrows(BusinessException.class, () -> SriXmlGenerator.generarNotaCredito(vacia));

        assertEquals("SIN_DETALLE", error.getCode());
    }

    @Test
    void sinPagosNoSeEmiteElBloqueCorrespondiente() {
        DatosFactura sinPagos = new DatosFactura(FixturesFactura.CLAVE, FixturesFactura.FECHA,
                FixturesFactura.factura().emisor(), FixturesFactura.factura().comprador(),
                FixturesFactura.factura().lineas(), List.of(), List.of());

        String xml = SriXmlGenerator.generarFactura(sinPagos);

        assertFalse(xml.contains("<pagos>"));
        assertDoesNotThrow(() -> validador.validar(xml));
    }
}

package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fase H.1: la nota de crédito se construye desde el XML de la factura que
 * modifica (plan §3.9 «referencia obligatoria al documento original»).
 */
class NotaCreditoBuilderTest {

    private static final String MOTIVO = "Se devuelve el tratamiento por error de diagnóstico";
    private static final LocalDate EMISION_ORIGINAL = FixturesFactura.FECHA;

    private final String xmlOriginal = SriXmlGenerator.generarFactura(FixturesFactura.factura());

    @Test
    void devuelveLaFacturaEnteraCuandoNoSeIndicanLineas() {
        NotaCreditoBuilder.NotaConstruida nota = construir(null, null);

        assertEquals(2, nota.datos().lineas().size(), "arrastra las dos líneas de la factura");
        assertEquals(0, nota.total().compareTo(new BigDecimal("167.79")),
                "la devolución total devuelve exactamente lo facturado");
        assertEquals("001-001-000000001", nota.datos().numDocModificado(),
                "el número se reconstruye con estab y punto de emisión");
        assertEquals("01", nota.datos().codDocModificado());
        assertEquals(EMISION_ORIGINAL, nota.datos().fechaEmisionDocSustento());
        assertEquals("05", nota.datos().comprador().tipoIdentificacion());
        assertEquals("juan@ejemplo.com", nota.datos().adicionales().getFirst().valor());
        assertEquals(MOTIVO, nota.datos().motivo());
    }

    @Test
    void devuelveUnaLineaParcialConSuCantidad() {
        NotaCreditoBuilder.NotaConstruida nota = construir(List.of(
                new NotaCreditoBuilder.Devolucion(2, new BigDecimal("1"))), null);

        assertEquals(1, nota.datos().lineas().size());
        DatosFactura.Linea linea = nota.datos().lineas().getFirst();
        assertEquals(new BigDecimal("1"), linea.cantidad());
        assertEquals(0, linea.precioUnitario().compareTo(new BigDecimal("25.50")),
                "conserva el precio unitario original");
        assertEquals(0, linea.descuento().compareTo(new BigDecimal("2.55")),
                "el descuento se prorratea con la cantidad");
        assertNull(linea.impuesto(), "el impuesto de la nota lo recalcula el generador");
        assertEquals(0, nota.total().compareTo(new BigDecimal("26.39")),
                "22.95 de base + 3.44 de IVA");
    }

    @Test
    void rechazaDevolverMasDeLoFacturado() {
        var error = assertThrows(BusinessException.class, () -> construir(List.of(
                new NotaCreditoBuilder.Devolucion(2, new BigDecimal("3"))), null));

        assertEquals("CANTIDAD_EXCEDE", error.getCode());
    }

    @Test
    void rechazaUnaLineaQueNoExiste() {
        var error = assertThrows(BusinessException.class, () -> construir(List.of(
                new NotaCreditoBuilder.Devolucion(5, BigDecimal.ONE)), null));

        assertEquals("LINEA_INVALIDA", error.getCode());
    }

    @Test
    void rechazaRepetirLaMismaLinea() {
        var error = assertThrows(BusinessException.class, () -> construir(List.of(
                new NotaCreditoBuilder.Devolucion(1, BigDecimal.ONE),
                new NotaCreditoBuilder.Devolucion(1, BigDecimal.ONE)), null));

        assertEquals("LINEA_DUPLICADA", error.getCode());
    }

    @Test
    void rechazaDevolverCeroUnidades() {
        var error = assertThrows(BusinessException.class, () -> construir(List.of(
                new NotaCreditoBuilder.Devolucion(1, BigDecimal.ZERO)), null));

        assertEquals("CANTIDAD_INVALIDA", error.getCode());
    }

    @Test
    void exigeMotivo() {
        var error = assertThrows(BusinessException.class, () -> construir(null, "   "));

        assertEquals("MOTIVO_REQUERIDO", error.getCode());
    }

    @Test
    void exigeQueElMotivoCabeEnElCampoDelSri() {
        var error = assertThrows(BusinessException.class, () -> construir(null, "x".repeat(301)));

        assertEquals("MOTIVO_INVALIDO", error.getCode());
    }

    @Test
    void exigeElXmlDeLaFacturaOriginal() {
        NotaCreditoBuilder.Entrada entrada = new NotaCreditoBuilder.Entrada(
                FixturesFactura.CLAVE, EMISION_ORIGINAL, emisor(), null, MOTIVO, List.of());

        var error = assertThrows(BusinessException.class, () -> NotaCreditoBuilder.construir(entrada));

        assertEquals("COMPROBANTE_SIN_XML", error.getCode());
    }

    @Test
    void rechazaUnXmlQueNoEsUnaFactura() {
        NotaCreditoBuilder.Entrada entrada = new NotaCreditoBuilder.Entrada(
                FixturesFactura.CLAVE, EMISION_ORIGINAL, emisor(), "<otro/>", MOTIVO, List.of());

        var error = assertThrows(BusinessException.class, () -> NotaCreditoBuilder.construir(entrada));

        assertEquals("COMPROBANTE_INVALIDO", error.getCode());
    }

    @Test
    void laNotaDeCreditoTotalCumpleElEsquemaDelSri() {
        NotaCreditoBuilder.NotaConstruida nota = construir(null, null);

        String xml = SriXmlGenerator.generarNotaCredito(nota.datos());

        assertDoesNotThrow(() -> new SriXmlValidator().validar(xml));
        assertTrue(xml.contains("<codigoInterno>LIM-02</codigoInterno>"));
        assertTrue(xml.contains("<valorModificacion>167.79</valorModificacion>"));
    }

    @Test
    void laNotaDeCreditoParcialCumpleElEsquemaDelSri() {
        NotaCreditoBuilder.NotaConstruida nota = construir(
                List.of(new NotaCreditoBuilder.Devolucion(2, new BigDecimal("1"))), null);

        String xml = SriXmlGenerator.generarNotaCredito(nota.datos());

        assertDoesNotThrow(() -> new SriXmlValidator().validar(xml));
        assertTrue(xml.contains("<valorModificacion>26.39</valorModificacion>"));
        assertTrue(xml.contains("<descripcion>Profilaxis &amp; revisión</descripcion>"));
    }

    // ------------------------------------------------------------------

    private NotaCreditoBuilder.NotaConstruida construir(List<NotaCreditoBuilder.Devolucion> devoluciones,
                                                        String motivo) {
        return NotaCreditoBuilder.construir(new NotaCreditoBuilder.Entrada(
                FixturesFactura.CLAVE, EMISION_ORIGINAL, emisor(),
                xmlOriginal, motivo == null ? MOTIVO : motivo, devoluciones));
    }

    private static DatosFactura.Emisor emisor() {
        DatosFactura.Emisor factura = FixturesFactura.emisor();
        return new DatosFactura.Emisor(factura.razonSocial(), factura.nombreComercial(), factura.ruc(),
                factura.dirMatriz(), factura.ambiente(), factura.tipoEmision(),
                ClaveAccesoService.COD_DOC_NOTA_CREDITO, factura.estab(),
                factura.ptoEmi(), factura.secuencial(), factura.dirEstablecimiento(),
                factura.obligadoContabilidad(), factura.agenteRetencion());
    }
}

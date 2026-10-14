package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fase G.4: los precios de la cuenta <b>incluyen IVA</b>, así que el
 * comprobante los reparte en base + impuesto y debe cuadrar al céntimo con
 * lo que el paciente debe.
 */
class FacturaBuilderTest {

    private final SriXmlValidator validador = new SriXmlValidator();

    @Test
    void importeTotalCuadraAlCentimoConLaCuenta() {
        // 100.00 + 50.01 = 150.01 con IVA ya incluido en los precios
        FacturaBuilder.FacturaConstruida construida = construir(
                List.of(cargo("Consulta odontológica", 1, "100.00", "100.00"),
                        cargo("Endodoncia", 1, "50.01", "50.01")),
                "0.00",
                List.of(new FacturaBuilder.Cobro(FormaPago.EFECTIVO, new BigDecimal("60.00"))),
                null);

        assertEquals(0, construida.totales().total().compareTo(new BigDecimal("150.01")));
        assertEquals(0, construida.totales().base().add(construida.totales().impuesto())
                .compareTo(construida.totales().total()));

        String xml = SriXmlGenerator.generarFactura(construida.datos());
        assertDoesNotThrow(() -> validador.validar(xml));
        assertTrue(xml.contains("<importeTotal>150.01</importeTotal>"));
        // el cobrado en efectivo y el resto a crédito (código 20 vigente)
        assertEquals(List.of("01", "20"), construida.datos().pagos().stream()
                .map(DatosFactura.Pago::formaPago).toList());
        assertEquals(0, construida.datos().pagos().get(1).total()
                .compareTo(new BigDecimal("90.01")));
    }

    @Test
    void importeTotalExactoConCantidadFraccionada() {
        // 3 × 33.34 = 100.02 y 1 × 8.35: casos en los que el redondeo del
        // 15% no coincide con el IVA ya cobrado
        FacturaBuilder.FacturaConstruida construida = construir(
                List.of(cargo("Material", 3, "33.34", "100.02"),
                        cargo("Revisiones", 1, "8.35", "8.35")),
                "0.00", List.of(), null);

        String xml = SriXmlGenerator.generarFactura(construida.datos());
        assertDoesNotThrow(() -> validador.validar(xml));
        assertTrue(xml.contains("<importeTotal>108.37</importeTotal>"));
        assertEquals(0, construida.totales().total().compareTo(new BigDecimal("108.37")));
    }

    @Test
    void elDescuentoDeLaCuentaSeReparteProporcionalmente() {
        // 3 cargos de 10.00 y un descuento global de 1.00 → 0.34/0.33/0.33
        FacturaBuilder.FacturaConstruida construida = construir(
                List.of(cargo("A", 1, "10.00", "10.00"),
                        cargo("B", 1, "10.00", "10.00"),
                        cargo("C", 1, "10.00", "10.00")),
                "1.00", List.of(), null);

        String xml = SriXmlGenerator.generarFactura(construida.datos());
        assertDoesNotThrow(() -> validador.validar(xml));
        assertTrue(xml.contains("<importeTotal>29.00</importeTotal>"));
        assertTrue(xml.contains("<totalSinImpuestos>25.22</totalSinImpuestos>"));
        assertTrue(xml.contains("<totalDescuento>0.88</totalDescuento>"));
        assertEquals(0, construida.totales().total().compareTo(new BigDecimal("29.00")));
    }

    @Test
    void losCobrosSeAgrupanPorCodigoDelCatalogoVigente() {
        FacturaBuilder.FacturaConstruida construida = construir(
                List.of(cargo("Tratamiento", 1, "100.00", "100.00")),
                "0.00",
                List.of(new FacturaBuilder.Cobro(FormaPago.EFECTIVO, new BigDecimal("10.00")),
                        new FacturaBuilder.Cobro(FormaPago.TARJETA, new BigDecimal("20.00")),
                        new FacturaBuilder.Cobro(FormaPago.TRANSFERENCIA, new BigDecimal("10.00")),
                        new FacturaBuilder.Cobro(FormaPago.OTRO, new BigDecimal("5.00"))),
                null);

        // 01 efectivo · 16 tarjeta de débito · 20 transferencia y otros,
        // con el saldo todavía no cobrado dentro del 20
        assertEquals(List.of("01", "16", "20"), construida.datos().pagos().stream()
                .map(DatosFactura.Pago::formaPago).toList());
        assertEquals(0, construida.datos().pagos().get(0).total().compareTo(new BigDecimal("10.00")));
        assertEquals(0, construida.datos().pagos().get(1).total().compareTo(new BigDecimal("20.00")));
        assertEquals(0, construida.datos().pagos().get(2).total().compareTo(new BigDecimal("70.00")));
    }

    @Test
    void loCobradoNoPuedeSuperarElTotalDeLaFactura() {
        FacturaBuilder.FacturaConstruida construida = construir(
                List.of(cargo("Tratamiento", 1, "100.00", "100.00")),
                "0.00",
                List.of(new FacturaBuilder.Cobro(FormaPago.EFECTIVO, new BigDecimal("60.00")),
                        new FacturaBuilder.Cobro(FormaPago.TARJETA, new BigDecimal("50.00"))),
                null);

        assertEquals(0, construida.datos().pagos().stream()
                .map(DatosFactura.Pago::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(new BigDecimal("100.00")));
        assertEquals(List.of("01", "16"), construida.datos().pagos().stream()
                .map(DatosFactura.Pago::formaPago).toList());
    }

    @Test
    void sinCobrosElSaldoSeEmiteACredito() {
        FacturaBuilder.FacturaConstruida construida = construir(
                List.of(cargo("Tratamiento", 1, "100.00", "100.00")),
                "0.00", List.of(), null);

        assertEquals(1, construida.datos().pagos().size());
        assertEquals("20", construida.datos().pagos().get(0).formaPago());
        assertEquals(0, construida.datos().pagos().get(0).total().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void lasFormasPagoExplicitasDebenSumarElTotal() {
        BusinessException error = assertThrows(BusinessException.class, () -> construir(
                List.of(cargo("Tratamiento", 1, "100.00", "100.00")),
                "0.00", List.of(),
                List.of(new DatosFactura.Pago("01", new BigDecimal("30.00"), null, null))));

        assertEquals("FORMAS_PAGO_NO_COINCIDEN", error.getCode());
    }

    @Test
    void rechazaFormasPagoFueraDelCatalogo() {
        BusinessException error = assertThrows(BusinessException.class, () -> construir(
                List.of(cargo("Tratamiento", 1, "100.00", "100.00")),
                "0.00", List.of(),
                List.of(new DatosFactura.Pago("99", new BigDecimal("100.00"), null, null))));

        assertEquals("FORMA_PAGO_INVALIDA", error.getCode());
    }

    @Test
    void rechazaCuentasSinCargos() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> construir(List.of(), "0.00", List.of(), null));

        assertEquals("SIN_DETALLE", error.getCode());
    }

    @Test
    void rechazaDescuentoMayorQueLosCargos() {
        BusinessException error = assertThrows(BusinessException.class, () -> construir(
                List.of(cargo("Tratamiento", 1, "100.00", "100.00")),
                "150.00", List.of(), null));

        assertEquals("DESCUENTO_EXCEDE", error.getCode());
    }

    @Test
    void traduceLasFormasDePagoAlCatalogoVigente() {
        assertEquals("01", FormaPagoCatalogo.codigo(FormaPago.EFECTIVO));
        assertEquals("16", FormaPagoCatalogo.codigo(FormaPago.TARJETA));
        assertEquals("20", FormaPagoCatalogo.codigo(FormaPago.TRANSFERENCIA));
        assertEquals("20", FormaPagoCatalogo.codigo(FormaPago.OTRO));
        assertDoesNotThrow(() -> FormaPagoCatalogo.validar("21"));
        assertThrows(BusinessException.class, () -> FormaPagoCatalogo.validar("22"));
        assertEquals("SIN UTILIZACIÓN DEL SISTEMA FINANCIERO", FormaPagoCatalogo.etiqueta("01"));
        assertEquals("TARJETA DE DÉBITO", FormaPagoCatalogo.etiqueta("16"));
        assertEquals("OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO",
                FormaPagoCatalogo.etiqueta("20"));
        assertEquals("ENDOSO DE TÍTULOS", FormaPagoCatalogo.etiqueta("21"));
        assertNull(FormaPagoCatalogo.etiqueta("04"), "los códigos retirados no tienen etiqueta");
    }

    // ------------------------------------------------------------------

    private FacturaBuilder.FacturaConstruida construir(List<FacturaBuilder.LineaCuenta> cargos,
                                                       String descuentoCuenta,
                                                       List<FacturaBuilder.Cobro> cobros,
                                                       List<DatosFactura.Pago> pagosExplicitos) {
        return FacturaBuilder.construir(new FacturaBuilder.Entrada(
                FixturesFactura.CLAVE, FixturesFactura.FECHA,
                FixturesFactura.emisor(), FixturesFactura.comprador(),
                cargos, new BigDecimal(descuentoCuenta), cobros, pagosExplicitos, List.of()));
    }

    private static FacturaBuilder.LineaCuenta cargo(String descripcion, int cantidad,
                                                    String precio, String total) {
        return new FacturaBuilder.LineaCuenta(descripcion, cantidad,
                new BigDecimal(precio), new BigDecimal(total));
    }
}

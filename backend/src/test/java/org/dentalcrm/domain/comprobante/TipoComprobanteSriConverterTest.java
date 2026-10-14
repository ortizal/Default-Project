package org.dentalcrm.domain.comprobante;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * El tipo de comprobante se guarda con el código del SRI ({@code 01}),
 * que es lo que admite la columna {@code tipo_comprobante} de V41.
 */
class TipoComprobanteSriConverterTest {

    private final TipoComprobanteSriConverter converter = new TipoComprobanteSriConverter();

    @Test
    void guardaElCodigoDelSri() {
        assertEquals("01", converter.convertToDatabaseColumn(TipoComprobanteSri.FACTURA));
        assertEquals("04", converter.convertToDatabaseColumn(TipoComprobanteSri.NOTA_CREDITO));
        assertEquals("07", converter.convertToDatabaseColumn(TipoComprobanteSri.RETENCION));
    }

    @Test
    void recuperaElTipoDesdeElCodigo() {
        assertEquals(TipoComprobanteSri.FACTURA, converter.convertToEntityAttribute("01"));
        assertEquals(TipoComprobanteSri.NOTA_DEBITO, converter.convertToEntityAttribute("05"));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}

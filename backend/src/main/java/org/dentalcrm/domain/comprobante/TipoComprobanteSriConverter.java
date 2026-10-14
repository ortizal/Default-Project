package org.dentalcrm.domain.comprobante;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda el tipo de comprobante con su código del SRI ({@code 01} factura,
 * {@code 04} nota de crédito…) en lugar del nombre del enum: es lo que
 * exige la tabla {@code comprobantes_electronicos} (V41) y lo que viaja en
 * la clave de acceso.
 */
@Converter(autoApply = false)
public class TipoComprobanteSriConverter implements AttributeConverter<TipoComprobanteSri, String> {

    @Override
    public String convertToDatabaseColumn(TipoComprobanteSri atributo) {
        return atributo == null ? null : atributo.codigo();
    }

    @Override
    public TipoComprobanteSri convertToEntityAttribute(String dato) {
        return dato == null ? null : TipoComprobanteSri.porCodigo(dato);
    }
}

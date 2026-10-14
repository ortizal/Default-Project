package org.dentalcrm.domain.comprobante;

import org.dentalcrm.exception.BusinessException;

/**
 * Catálogo de comprobantes electrónicos del SRI (ficha técnica, tabla 5).
 * Sólo se implementan los que la Fase G necesita; el resto queda previsto.
 */
public enum TipoComprobanteSri {
    FACTURA("01"),
    NOTA_CREDITO("04"),
    NOTA_DEBITO("05"),
    GUIA_REMISION("06"),
    RETENCION("07");

    private final String codigo;

    TipoComprobanteSri(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static TipoComprobanteSri porCodigo(String codigo) {
        for (TipoComprobanteSri tipo : values()) {
            if (tipo.codigo.equals(codigo)) {
                return tipo;
            }
        }
        throw new BusinessException("TIPO_COMPROBANTE_INVALIDO",
                "Tipo de comprobante desconocido: " + codigo);
    }
}

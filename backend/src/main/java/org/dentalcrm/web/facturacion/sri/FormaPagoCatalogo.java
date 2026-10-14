package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.exception.BusinessException;

/**
 * Catálogo de formas de pago del SRI y su traducción desde
 * {@link FormaPago}, la forma en que la cuenta del paciente registra el
 * dinero recibido (Fase F, plan §3.4).
 *
 * <p>Se usa el catálogo <b>vigente</b> de la tabla 15 de la ficha técnica:
 * <ul>
 *   <li>{@code 01} — efectivo, sin sistema financiero;</li>
 *   <li>{@code 16} — tarjeta de débito (y {@code 19} si el pago es a
 *       crédito, que se pide expresamente en la emisión);</li>
 *   <li>{@code 20} — otros con sistema financiero: transferencias,
 *       depósitos y el saldo de la cuenta que todavía no se cobra.</li>
 * </ul>
 */
public final class FormaPagoCatalogo {

    public static final String EFECTIVO = "01";
    public static final String TARJETA_DEBITO = "16";
    public static final String TARJETA_CREDITO = "19";
    public static final String OTROS_SISTEMA_FINANCIERO = "20";

    private FormaPagoCatalogo() {
    }

    /** Código SRI para la forma de pago registrada en la cuenta. */
    public static String codigo(FormaPago forma) {
        if (forma == null) {
            return OTROS_SISTEMA_FINANCIERO;
        }
        return switch (forma) {
            case EFECTIVO -> EFECTIVO;
            case TARJETA -> TARJETA_DEBITO;
            case TRANSFERENCIA, OTRO -> OTROS_SISTEMA_FINANCIERO;
        };
    }

    /** Valida un código pedido en la emisión (patrón del XSD: 01 … 21). */
    public static void validar(String codigo) {
        if (codigo == null || !codigo.matches("([0][1-9])|([1][0-9])|([2][0-1])")) {
            throw new BusinessException("FORMA_PAGO_INVALIDA",
                    "Código de forma de pago desconocido: " + codigo);
        }
    }

    /**
     * Etiqueta oficial para el RIDE y la bandeja. Son las formas de pago
     * <b>vigentes</b> de la tabla del SRI (01 y 15…21); los códigos
     * antiguos que el XSD todavía admite quedan sin etiqueta y el
     * comprobante los muestra sólo con su número.
     */
    public static String etiqueta(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case EFECTIVO -> "SIN UTILIZACIÓN DEL SISTEMA FINANCIERO";
            case "15" -> "COMPENSACIÓN DE DEUDAS";
            case TARJETA_DEBITO -> "TARJETA DE DÉBITO";
            case "17" -> "DINERO ELECTRÓNICO";
            case "18" -> "TARJETA PREPAGO";
            case TARJETA_CREDITO -> "TARJETA DE CRÉDITO";
            case OTROS_SISTEMA_FINANCIERO -> "OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO";
            case "21" -> "ENDOSO DE TÍTULOS";
            default -> null;
        };
    }
}

package org.dentalcrm.domain.configuracion;

/**
 * Ambiente de los servicios web del SRI (plan §3.5). PRUEBAS es el
 * ambiente por defecto: ningún comprobante sale a producción sin que
 * alguien cambie la configuración.
 */
public enum AmbienteSri {
    PRUEBAS,
    PRODUCCION
}

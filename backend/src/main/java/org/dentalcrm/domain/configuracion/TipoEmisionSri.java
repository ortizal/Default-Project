package org.dentalcrm.domain.configuracion;

/**
 * Tipo de emisión del comprobante electrónico (ficha técnica del SRI,
 * tabla 2): NORMAL es la emisión ordinaria y CONTINGENCIA la que se usa
 * cuando el servicio de autorización del SRI no está disponible. El
 * ambiente PRUEBAS/PRODUCCIÓN se configura aparte en {@link AmbienteSri}.
 */
public enum TipoEmisionSri {
    NORMAL,
    CONTINGENCIA
}

package org.dentalcrm.web.facturacion.sri;

/**
 * Adaptador hacia los servicios offline del SRI (plan §3.6 {@code SriGateway}).
 *
 * <p>Es una interfaz para que la lógica de estados ({@link AuthorizationService})
 * se pueda probar sin red y para poder sustituir la implementación cuando el
 * SRI publique un servicio nuevo, sin tocar el resto del módulo.
 */
public interface SriGateway {

    /**
     * Envía el XML firmado (en base64, como exige el webservice) al servicio
     * de recepción.
     *
     * @throws org.dentalcrm.exception.BusinessException con código
     *         {@code SRI_SIN_CONEXION}, {@code SRI_ERROR_HTTP} o
     *         {@code SRI_RESPUESTA_INVALIDA} si el servicio no responde bien
     */
    RespuestaRecepcion recepcionar(String xmlFirmadoBase64, SriEntorno entorno);

    /** Consulta el estado de autorización de una clave de acceso. */
    RespuestaAutorizacion autorizar(String claveAcceso, SriEntorno entorno);
}

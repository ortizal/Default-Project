package org.dentalcrm.domain.comprobante;

/**
 * Estados de generación del comprobante (plan §3.8): lo que hizo la
 * aplicación, antes de hablar con el SRI.
 */
public enum EstadoGeneracion {
    /** Recién creado, todavía no tiene XML. */
    BORRADOR,
    /** XML generado y validado contra el XSD, aún sin firmar. */
    GENERADO,
    /** XML firmado electrónicamente con el certificado del emisor. */
    FIRMADO,
    /** Enviado al SRI y aceptado en recepción. */
    ENVIADO,
    /** Fallo en la generación, firma o envío: ver errorMensaje. */
    ERROR
}

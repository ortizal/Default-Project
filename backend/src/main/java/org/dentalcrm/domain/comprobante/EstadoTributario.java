package org.dentalcrm.domain.comprobante;

/**
 * Estados tributarios del comprobante (plan §3.8): lo que dice el SRI.
 * Se persisten por separado de {@link EstadoGeneracion}.
 */
public enum EstadoTributario {
    /** Enviado o pendiente de consultar; el SRI aún no responde. */
    PENDIENTE,
    /** El SRI autorizó el comprobante (AUT). */
    AUTORIZADO,
    /** El SRI lo devolvió en recepción: no cumple estructura/reglas. */
    DEVUELTO,
    /** El SRI lo consultó y respondió NO AUTORIZADO (NAT). */
    NO_AUTORIZADO,
    /** Anulado por el emisor (plan §3.10); nunca se borra la fila. */
    ANULADO
}

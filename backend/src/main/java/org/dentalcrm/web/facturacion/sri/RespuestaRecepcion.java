package org.dentalcrm.web.facturacion.sri;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Respuesta del webservice de recepción: {@code RECIBIDA} (el SRI aceptó
 * el XML para procesar) o {@code DEVUELTA} (no cumple y hay que corregir).
 */
public record RespuestaRecepcion(String estado, List<MensajeSri> mensajes) {

    public static final String RECIBIDA = "RECIBIDA";
    public static final String DEVUELTA = "DEVUELTA";

    public boolean recibida() {
        return RECIBIDA.equalsIgnoreCase(estado);
    }

    public boolean devuelta() {
        return DEVUELTA.equalsIgnoreCase(estado);
    }

    /** Resumen de los mensajes del SRI, línea por mensaje. */
    public String detalle() {
        return mensajes.stream().map(MensajeSri::formato).collect(Collectors.joining("\n"));
    }
}

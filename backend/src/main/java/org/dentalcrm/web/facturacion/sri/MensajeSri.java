package org.dentalcrm.web.facturacion.sri;

/**
 * Mensaje del SRI (identificador, texto, información adicional y tipo
 * ERROR/ADVERTENCIA/INFO) tal como viene del webservice.
 */
public record MensajeSri(String identificador, String mensaje, String informacionAdicional, String tipo) {

    /** Texto legible para guardar en {@code sri_respuesta} y mostrar en la bandeja. */
    public String formato() {
        StringBuilder sb = new StringBuilder();
        if (identificador != null && !identificador.isBlank()) {
            sb.append('[').append(identificador).append("] ");
        }
        if (mensaje != null) {
            sb.append(mensaje);
        }
        if (informacionAdicional != null && !informacionAdicional.isBlank()) {
            sb.append(" — ").append(informacionAdicional);
        }
        return sb.toString().trim();
    }
}

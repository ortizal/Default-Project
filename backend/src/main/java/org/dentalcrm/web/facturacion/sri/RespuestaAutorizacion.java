package org.dentalcrm.web.facturacion.sri;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Respuesta del webservice de autorización por clave de acceso.
 *
 * <p>{@code EN_PROCESO} es el estado transitorio del SRI (PPR): todavía no
 * tiene el resultado; hay que volver a consultar. El XML autorizado llega
 * dentro de {@code xmlAutorizado} y es el que se conserva para el RIDE (§3.11).
 */
public record RespuestaAutorizacion(String estado,
                                    String claveAccesoConsultada,
                                    String numeroAutorizacion,
                                    Instant fechaAutorizacion,
                                    String ambiente,
                                    String xmlAutorizado,
                                    List<MensajeSri> mensajes,
                                    boolean sinResultado) {

    public static final String AUTORIZADO = "AUTORIZADO";
    public static final String NO_AUTORIZADO = "NO_AUTORIZADO";
    public static final String EN_PROCESO = "EN_PROCESO";

    public boolean autorizado() {
        return AUTORIZADO.equalsIgnoreCase(estado);
    }

    public boolean noAutorizado() {
        return NO_AUTORIZADO.equalsIgnoreCase(estado);
    }

    public boolean enProceso() {
        return sinResultado || EN_PROCESO.equalsIgnoreCase(estado);
    }

    /** Resumen de los mensajes del SRI, línea por mensaje. */
    public String detalle() {
        return mensajes.stream().map(MensajeSri::formato).collect(Collectors.joining("\n"));
    }
}

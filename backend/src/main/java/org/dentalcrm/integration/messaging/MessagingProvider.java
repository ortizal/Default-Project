package org.dentalcrm.integration.messaging;

/**
 * Abstracción de proveedor de mensajería (plan 2.2): la lógica de negocio
 * depende de esta interfaz, no directamente de OpenWA.
 */
public interface MessagingProvider {

    String nombre();

    boolean estaConfigurado();

    ResultadoConexion iniciarSesion(String sesionId);

    String estadoSesion(String sesionId);

    void cerrarSesion(String sesionId);

    void enviarMensaje(String sesionId, String telefono, String texto);

    /**
     * Envía un documento (PDF, imagen, etc.) por WhatsApp. Implementado por
     * OpenWA vía send-document (base64).
     */
    void enviarDocumento(String sesionId, String telefono, String nombreArchivo,
                         String mime, byte[] contenido, String caption);

    /**
     * Devuelve el nombre interno de la sesión a partir del identificador
     * externo que el proveedor reporta en sus webhooks (p.ej. el UUID de OpenWA).
     */
    String nombreDeSesionExterna(String idExterno);

    /**
     * Convierte un identificador de sesión interno al nombre externo que el
     * proveedor usa para la sesión (p.ej. sanear caracteres no permitidos).
     * Por defecto se devuelve el identificador tal cual.
     */
    default String normalizarSesion(String id) {
        return id == null ? null : id.trim();
    }

    record ResultadoConexion(String estado, String detalle, String qr, String error) {
    }
}
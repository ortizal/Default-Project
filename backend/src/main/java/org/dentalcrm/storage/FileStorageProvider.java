package org.dentalcrm.storage;

import java.util.Optional;

/**
 * Abstracción de almacenamiento de archivos del sistema (radiografías,
 * consentimientos, RIDE, etc.). La implementación por defecto es local;
 * una futura implementación S3 sólo debe implementar esta interfaz.
 *
 * <p>Las claves son rutas relativas con "/" (ej. {@code "1/historia/paciente-7/rx.png"}).
 * Nunca deben exponerse rutas físicas al cliente.
 */
public interface FileStorageProvider {

    /**
     * Guarda el contenido y devuelve la clave del archivo.
     *
     * @param carpeta     subcarpeta lógica (puede ser {@code null} o vacía)
     * @param nombreArchivo nombre de archivo seguro (sin separadores de ruta)
     * @param contenido   bytes a guardar
     * @return clave relativa identificativa del archivo
     */
    String guardar(String carpeta, String nombreArchivo, byte[] contenido);

    /** Lee el archivo identificado por la clave. */
    Optional<byte[]> leer(String clave);

    /** Elimina el archivo identificado por la clave (no falla si no existe). */
    void eliminar(String clave);

    /** Indica si existe un archivo con esa clave. */
    boolean existe(String clave);
}

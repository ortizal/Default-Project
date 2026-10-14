package org.dentalcrm.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Almacenamiento local en disco. La raíz se configura con
 * {@code app.storage.directorio} (env {@code STORAGE_LOCAL_DIR}); en Docker
 * se monta el volumen {@code storage_data} para que los archivos sobrevivan
 * a la reconstrucción de la imagen.
 */
@Component
public class LocalFileStorageProvider implements FileStorageProvider {

    private final Path raiz;

    public LocalFileStorageProvider(@Value("${app.storage.directorio:./data/archivos}") String directorio) {
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String nombreArchivo, byte[] contenido) {
        Path destino = resolver(carpeta, nombreArchivo);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo: " + e.getMessage(), e);
        }
        return claveDe(destino);
    }

    @Override
    public Optional<byte[]> leer(String clave) {
        Path archivo = deClave(clave);
        try {
            if (!Files.isRegularFile(archivo)) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(archivo));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(String clave) {
        Path archivo = deClave(clave);
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existe(String clave) {
        return Files.isRegularFile(deClave(clave));
    }

    /** Raíz absoluta (sólo para tests y diagnóstico). */
    public Path raiz() {
        return raiz;
    }

    private Path resolver(String carpeta, String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new IllegalArgumentException("El nombre de archivo es obligatorio");
        }
        if (nombreArchivo.contains("/") || nombreArchivo.contains("\\")) {
            throw new IllegalArgumentException("El nombre de archivo no puede contener separadores de ruta");
        }
        Path destino = raiz;
        if (carpeta != null && !carpeta.isBlank()) {
            destino = destino.resolve(carpeta);
        }
        destino = destino.resolve(nombreArchivo).normalize();
        if (!destino.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        return destino;
    }

    private Path deClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave de archivo es obligatoria");
        }
        Path resuelta = raiz.resolve(clave.replace('\\', '/')).normalize();
        if (!resuelta.startsWith(raiz)) {
            throw new IllegalArgumentException("Clave de archivo inválida");
        }
        return resuelta;
    }

    private String claveDe(Path archivo) {
        return raiz.relativize(archivo).toString().replace('\\', '/');
    }
}

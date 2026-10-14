package org.dentalcrm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Firmas/hash del sistema: SHA-256 en hex para integridad de archivos
 * (radiografías, consentimientos, RIDE) y huellas de datos sensibles.
 */
public final class HashUtil {

    private HashUtil() {
    }

    /** SHA-256 en minúsculas hex de los bytes indicados. */
    public static String sha256Hex(byte[] contenido) {
        if (contenido == null) {
            throw new IllegalArgumentException("El contenido es obligatorio");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }

    /** SHA-256 en minúsculas hex del texto en UTF-8. */
    public static String sha256Hex(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El texto es obligatorio");
        }
        return sha256Hex(texto.getBytes(StandardCharsets.UTF_8));
    }
}

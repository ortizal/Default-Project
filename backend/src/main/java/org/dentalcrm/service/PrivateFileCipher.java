package org.dentalcrm.service;

import org.dentalcrm.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Component
public class PrivateFileCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] PURPOSE = "dentalcrm-private-file-v1".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String configuredKey;

    public PrivateFileCipher(@Value("${app.private-file.encryption-key}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public byte[] encrypt(byte[] value) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(PURPOSE);
            byte[] encrypted = cipher.doFinal(value);
            return ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
        } catch (Exception e) {
            throw new BusinessException("PRIVATE_FILE_ENCRYPTION_ERROR", "No se pudo proteger el archivo privado");
        }
    }

    public byte[] decrypt(byte[] value) {
        if (value == null || value.length <= NONCE_BYTES) {
            throw new BusinessException("PRIVATE_FILE_DECRYPTION_ERROR", "El archivo privado almacenado no es válido");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(PURPOSE);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new BusinessException("PRIVATE_FILE_DECRYPTION_ERROR", "No se pudo descifrar el archivo privado");
        }
    }

    private SecretKeySpec key() throws Exception {
        if (configuredKey == null || configuredKey.length() < 32) {
            throw new IllegalStateException("APP_FIRMA_DIGITAL_ENCRYPTION_KEY debe tener al menos 32 caracteres");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
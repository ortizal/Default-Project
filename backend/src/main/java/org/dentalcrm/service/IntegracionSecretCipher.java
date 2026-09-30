package org.dentalcrm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class IntegracionSecretCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] PURPOSE = "dentalcrm-integration-secret-v1".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String configuredKey;

    public IntegracionSecretCipher(@Value("${app.integraciones.encryption-key}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(PURPOSE);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger la credencial de integración");
        }
    }

    public String decrypt(String value) {
        try {
            byte[] raw = Base64.getDecoder().decode(value);
            if (raw.length <= NONCE_BYTES) throw new IllegalArgumentException("secreto cifrado inválido");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, raw, 0, NONCE_BYTES));
            cipher.updateAAD(PURPOSE);
            return new String(cipher.doFinal(raw, NONCE_BYTES, raw.length - NONCE_BYTES), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar la credencial de integración");
        }
    }

    private SecretKeySpec key() throws Exception {
        if (configuredKey == null || configuredKey.length() < 32) {
            throw new IllegalStateException("APP_INTEGRACIONES_ENCRYPTION_KEY debe tener al menos 32 caracteres");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
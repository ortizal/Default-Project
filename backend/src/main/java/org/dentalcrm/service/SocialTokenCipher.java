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
import java.util.Base64;

@Component
public class SocialTokenCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String configuredKey;

    public SocialTokenCipher(@Value("${app.social.token-encryption-key}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (Exception e) {
            throw new BusinessException("SOCIAL_TOKEN_ERROR", "No se pudo proteger el token social");
        }
    }

    private SecretKeySpec key() throws Exception {
        if (configuredKey == null || configuredKey.length() < 32) {
            throw new IllegalStateException("SOCIAL_TOKEN_ENCRYPTION_KEY debe tener al menos 32 caracteres");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
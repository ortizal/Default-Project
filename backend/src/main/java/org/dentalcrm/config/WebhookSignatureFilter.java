package org.dentalcrm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifica la firma HMAC-SHA256 (X-OpenWA-Signature) de los webhooks de OpenWA.
 * Se habilita solo si OPENWA_WEBHOOK_SECRET está configurado.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private final String webhookSecret;

    public WebhookSignatureFilter(@Value("${app.openwa.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        byte[] cuerpo;
        try (InputStream in = request.getInputStream()) {
            cuerpo = in.readAllBytes();
        }
        if (cuerpo.length == 0) {
            chain.doFilter(request, response);
            return;
        }

        String firma = request.getHeader("X-OpenWA-Signature");
        if (firma == null || !verificar(cuerpo, firma)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Firma de webhook inválida\"}");
            return;
        }

        chain.doFilter(new CuerpoCacheRequest(request, cuerpo), response);
    }

    private boolean verificar(byte[] cuerpo, String firma) {
        try {
            if (!firma.startsWith("sha256=")) {
                return false;
            }
            String recibida = firma.substring("sha256=".length());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperada = mac.doFinal(cuerpo);
            byte[] recv = hexABytes(recibida);
            return recv.length == esperada.length && MessageDigest.isEqual(recv, esperada);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hexABytes(String hex) {
        int n = hex.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static class CuerpoCacheRequest extends HttpServletRequestWrapper {

        private final byte[] cuerpo;

        CuerpoCacheRequest(HttpServletRequest request, byte[] cuerpo) {
            super(request);
            this.cuerpo = cuerpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(cuerpo);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bis.read();
                }

                @Override
                public boolean isFinished() {
                    return bis.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // streams síncronos no usan listener
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cuerpo), StandardCharsets.UTF_8));
        }
    }
}
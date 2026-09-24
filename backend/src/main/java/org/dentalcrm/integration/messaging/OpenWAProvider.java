package org.dentalcrm.integration.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import org.dentalcrm.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenWAProvider implements MessagingProvider {

    private static final String NOMBRE = "OPENWA";

    private final WebClient client;
    private final String url;
    private final String apiKey;
    private final String webhookUrl;
    private final String webhookSecret;

    public OpenWAProvider(WebClient.Builder builder,
                          @Value("${app.openwa.url}") String url,
                          @Value("${app.openwa.api-key}") String apiKey,
                          @Value("${app.openwa.webhook-url:}") String webhookUrl,
                          @Value("${app.openwa.webhook-secret:}") String webhookSecret) {
        this.client = builder.clone().baseUrl(url).build();
        this.url = url;
        this.apiKey = apiKey;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean estaConfigurado() {
        return url != null && !url.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public ResultadoConexion iniciarSesion(String sesionId) {
        String nombre = normalizarNombre(sesionId);
        String idExterno = asegurarSesion(nombre);
        if (idExterno == null) {
            return new ResultadoConexion("ERROR", "OpenWA no creó la sesión '" + nombre + "'", null,
                    "Respuesta inesperada al crear la sesión");
        }
        registrarWebhook(idExterno);

        // El start responde 400 si la sesión ya estaba iniciada; se tolera.
        ejecutarTolerante(client.post().uri("/api/sessions/{id}/start", idExterno), 400, 409);

        // Esperar brevemente a que la sesión produzca el QR (initializing -> qr_ready).
        for (int intento = 0; intento < 6; intento++) {
            JsonNode sesion = ejecutarTolerante(
                    client.get().uri("/api/sessions/{id}", idExterno), 404);
            String estado = sesion == null ? "initializing" : sesion.path("status").asText("initializing");
            if ("ready".equalsIgnoreCase(estado)) {
                return new ResultadoConexion("CONECTADA", "Sesión ya vinculada", null, null);
            }
            JsonNode qr = ejecutarTolerante(
                    client.get().uri("/api/sessions/{id}/qr", idExterno), 400, 404);
            if (qr != null && qr.hasNonNull("qrCode")) {
                return new ResultadoConexion("CONECTANDO", "Escanea el código QR", qr.get("qrCode").asText(), null);
            }
            dormir(1000);
        }
        return new ResultadoConexion(normalizarEstado(estadoBruto(idExterno)),
                "Generando código QR", null, null);
    }

    @Override
    public String estadoSesion(String sesionId) {
        String idExterno = buscarId(sesionId);
        if (idExterno == null) {
            return "DESCONECTADA";
        }
        return normalizarEstado(estadoBruto(idExterno));
    }

    @Override
    public void cerrarSesion(String sesionId) {
        String idExterno = buscarId(sesionId);
        if (idExterno == null) {
            return;
        }
        ejecutarTolerante(client.post().uri("/api/sessions/{id}/stop", idExterno), 400, 404, 409);
    }

    @Override
    public void enviarMensaje(String sesionId, String telefono, String texto) {
        String idExterno = buscarId(sesionId);
        if (idExterno == null) {
            throw new BusinessException("SESION_NO_CONECTADA",
                    "La sesión '" + sesionId + "' no existe en OpenWA");
        }
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            throw new BusinessException("TELEFONO_INVALIDO", "El teléfono de destino no es válido");
        }
        ejecutar(client.post()
                .uri("/api/sessions/{id}/messages/send-text", idExterno)
                .bodyValue(Map.of("chatId", digitos + "@c.us", "text", texto)));
    }

    @Override
    public void enviarDocumento(String sesionId, String telefono, String nombreArchivo,
                                String mime, byte[] contenido, String caption) {
        String idExterno = buscarId(sesionId);
        if (idExterno == null) {
            throw new BusinessException("SESION_NO_CONECTADA",
                    "La sesión '" + sesionId + "' no existe en OpenWA");
        }
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            throw new BusinessException("TELEFONO_INVALIDO", "El teléfono de destino no es válido");
        }
        if (contenido == null || contenido.length == 0) {
            throw new BusinessException("DOCUMENTO_VACIO", "El documento a enviar no tiene contenido");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("chatId", digitos + "@c.us");
        body.put("base64", Base64.getEncoder().encodeToString(contenido));
        body.put("mimetype", mime == null || mime.isBlank() ? "application/octet-stream" : mime);
        body.put("filename", nombreArchivo == null || nombreArchivo.isBlank() ? "documento.pdf" : nombreArchivo);
        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
        }
        ejecutar(client.post()
                .uri("/api/sessions/{id}/messages/send-document", idExterno)
                .bodyValue(body));
    }

    @Override
    public String nombreDeSesionExterna(String idExterno) {
        if (idExterno == null || idExterno.isBlank()) {
            return null;
        }
        JsonNode sesion = ejecutarTolerante(client.get()
                .uri("/api/sessions/{id}", idExterno), 404);
        return sesion == null ? null : sesion.path("name").asText(null);
    }

    @Override
    public String normalizarSesion(String id) {
        return normalizarNombre(id);
    }

    // ------------------------------------------------------------------
    // Helpers de OpenWA
    // ------------------------------------------------------------------

    private String asegurarSesion(String nombre) {
        String idExterno = buscarId(nombre);
        if (idExterno != null) {
            return idExterno;
        }
        JsonNode creada = ejecutarTolerante(client.post()
                .uri("/api/sessions")
                .bodyValue(Map.of("name", nombre)), 409);
        return creada == null ? null : creada.path("id").asText(null);
    }

    private String buscarId(String nombre) {
        String objetivo = normalizarNombre(nombre);
        JsonNode lista = ejecutar(client.get().uri("/api/sessions"));
        if (lista != null && lista.isArray()) {
            for (JsonNode nodo : lista) {
                if (objetivo.equalsIgnoreCase(normalizarNombre(nodo.path("name").asText("")))) {
                    return nodo.path("id").asText(null);
                }
            }
        }
        return null;
    }

    private String estadoBruto(String idExterno) {
        JsonNode sesion = ejecutarTolerante(client.get()
                .uri("/api/sessions/{id}", idExterno), 404);
        return sesion == null ? "ERROR" : sesion.path("status").asText("ERROR");
    }

    private void registrarWebhook(String idExterno) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        JsonNode lista = ejecutar(client.get().uri("/api/sessions/{id}/webhooks", idExterno));
        if (lista != null && lista.isArray()) {
            for (JsonNode nodo : lista) {
                if (webhookUrl.equals(nodo.path("url").asText(""))) {
                    return;
                }
            }
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("url", webhookUrl);
        body.put("events", List.of("message.received"));
        body.put("retryCount", 3);
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            body.put("secret", webhookSecret);
        }
        ejecutar(client.post().uri("/api/sessions/{id}/webhooks", idExterno).bodyValue(body));
    }

    private String normalizarNombre(String sesionId) {
        String nombre = sesionId == null ? "" : sesionId.trim();
        nombre = nombre.replaceAll("[^a-zA-Z0-9-]", "-");
        if (!nombre.matches("[a-zA-Z0-9-]{3,}")) {
            nombre = "bot-" + nombre;
        }
        return nombre.length() > 50 ? nombre.substring(0, 50) : nombre;
    }

    private String normalizarEstado(String raw) {
        if (raw == null) {
            return "ERROR";
        }
        return switch (raw.toLowerCase()) {
            case "ready" -> "CONECTADA";
            case "initializing", "qr_ready", "authenticating", "disconnected" -> "CONECTANDO";
            case "created" -> "DESCONECTADA";
            case "action_required", "failed" -> "ERROR";
            default -> "ERROR";
        };
    }

    private JsonNode ejecutar(WebClient.RequestHeadersSpec<?> spec) {
        try {
            return spec.headers(h -> h.set("X-API-Key", apiKey))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException e) {
            throw error(e);
        }
    }

    private JsonNode ejecutarTolerante(WebClient.RequestHeadersSpec<?> spec, int... codigosTolerados) {
        try {
            return ejecutar(spec);
        } catch (BusinessException e) {
            if (esTolerable(e, codigosTolerados)) {
                return null;
            }
            throw e;
        }
    }

    private boolean esTolerable(BusinessException e, int[] codigos) {
        int status = e.getStatus() == null ? -1 : e.getStatus().value();
        for (int codigo : codigos) {
            if (codigo == status) {
                return true;
            }
        }
        return false;
    }

    private BusinessException error(WebClientResponseException e) {
        String detalle = e.getResponseBodyAsString();
        if (detalle == null) {
            detalle = "";
        }
        if (detalle.length() > 300) {
            detalle = detalle.substring(0, 300);
        }
        return new BusinessException(HttpStatus.valueOf(e.getStatusCode().value()), "OPENWA_API_ERROR",
                "Error en la llamada a OpenWA (" + e.getStatusCode().value() + "): " + detalle);
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
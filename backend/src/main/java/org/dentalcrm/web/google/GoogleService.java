package org.dentalcrm.web.google;

import com.fasterxml.jackson.databind.JsonNode;
import org.dentalcrm.domain.google.GoogleCredential;
import org.dentalcrm.domain.google.GoogleCredentialRepository;
import org.dentalcrm.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleService {

    private static final String SCOPE_CALENDAR = "https://www.googleapis.com/auth/calendar.events";
    private static final String SCOPE_CALENDAR_READ = "https://www.googleapis.com/auth/calendar.readonly";
    private static final String SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email";

    private final WebClient apiClient;
    private final WebClient oauthClient;
    private final GoogleCredentialRepository credentialRepository;

    private final String authBaseUrl;
    private final String oauthBaseUrl;
    private final String apiBaseUrl;

    public GoogleService(WebClient.Builder builder,
                         @Value("${app.google.auth-base-url}") String authBaseUrl,
                         @Value("${app.google.oauth-base-url}") String oauthBaseUrl,
                         @Value("${app.google.api-base-url}") String apiBaseUrl,
                         GoogleCredentialRepository credentialRepository) {
        this.apiClient = builder.clone().baseUrl(apiBaseUrl).build();
        this.oauthClient = builder.clone().baseUrl(oauthBaseUrl).build();
        this.authBaseUrl = authBaseUrl;
        this.oauthBaseUrl = oauthBaseUrl;
        this.apiBaseUrl = apiBaseUrl;
        this.credentialRepository = credentialRepository;
    }

    private String clientId() {
        return credentialRepository.findTopByOrderByIdAsc().map(GoogleCredential::getClientId).orElse("");
    }

    private String clientSecret() {
        return credentialRepository.findTopByOrderByIdAsc().map(GoogleCredential::getClientSecret).orElse("");
    }

    private String redirectUri() {
        return credentialRepository.findTopByOrderByIdAsc().map(GoogleCredential::getRedirectUri).orElse("");
    }

    public boolean estaConfigurado() {
        String cid = clientId();
        String csec = clientSecret();
        return cid != null && !cid.isBlank() && csec != null && !csec.isBlank();
    }

    public void arrojarSiNoConfigurado() {
        if (!estaConfigurado()) {
            throw new BusinessException("GOOGLE_NO_CONFIGURADO",
                    "La integración con Google Calendar no está configurada (falta GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET)");
        }
    }

    public String urlAutenticacion() {
        return authBaseUrl + "/o/oauth2/v2/auth"
                + "?client_id=" + enc(clientId())
                + "&redirect_uri=" + enc(redirectUri())
                + "&response_type=code"
                + "&scope=" + enc(scopeCompleto())
                + "&access_type=offline"
                + "&prompt=consent";
    }

    public String scopeCompleto() {
        return SCOPE_CALENDAR + " " + SCOPE_CALENDAR_READ + " " + SCOPE_USERINFO_EMAIL;
    }

    public GoogleTokenResponse intercambiarCodigo(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId());
        form.add("client_secret", clientSecret());
        form.add("redirect_uri", redirectUri());
        form.add("grant_type", "authorization_code");
        return GoogleTokenResponse.from(postForm("/token", form));
    }

    public GoogleTokenResponse refrescarToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId());
        form.add("client_secret", clientSecret());
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        return GoogleTokenResponse.from(postForm("/token", form));
    }

    public String obtenerEmail(String accessToken) {
        JsonNode json = apiClient.get()
                .uri("/oauth2/v2/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return json == null ? null : json.path("email").asText(null);
    }

    public List<CalendarioRemoto> listarCalendarios(String accessToken) {
        JsonNode json = apiClient.get()
                .uri("/calendar/v3/users/me/calendarList")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        List<CalendarioRemoto> resultado = new ArrayList<>();
        JsonNode items = json == null ? null : json.path("items");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                resultado.add(new CalendarioRemoto(
                        item.path("id").asText(null),
                        item.path("summary").asText(null),
                        item.path("timeZone").asText(null)));
            }
        }
        return resultado;
    }

    public String crearEvento(String accessToken, String calendarId, EventoGoogle evento) {
        JsonNode json = apiClient.post()
                .uri("/calendar/v3/calendars/{calendarId}/events", calendarId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payloadEvento(evento))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return json == null ? null : json.path("id").asText(null);
    }

    public void actualizarEvento(String accessToken, String calendarId, String eventId, EventoGoogle evento) {
        apiClient.put()
                .uri("/calendar/v3/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payloadEvento(evento))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public void eliminarEvento(String accessToken, String calendarId, String eventId) {
        apiClient.delete()
                .uri("/calendar/v3/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode obtenerEvento(String accessToken, String calendarId, String eventId) {
        return apiClient.get()
                .uri("/calendar/v3/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public Object payloadEvento(EventoGoogle evento) {
        return java.util.Map.of(
                "summary", evento.summary(),
                "description", evento.description(),
                "start", java.util.Map.of(
                        "dateTime", evento.fecha() + "T" + evento.horaInicio(),
                        "timeZone", evento.timeZone()),
                "end", java.util.Map.of(
                        "dateTime", evento.fecha() + "T" + evento.horaFin(),
                        "timeZone", evento.timeZone()));
    }

    private JsonNode postForm(String uri, MultiValueMap<String, String> form) {
        try {
            return oauthClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw errorGoogle(e);
        }
    }

    private BusinessException errorGoogle(WebClientResponseException e) {
        String detalle = e.getResponseBodyAsString();
        if (detalle != null && detalle.length() > 300) {
            detalle = detalle.substring(0, 300);
        }
        return new BusinessException(HttpStatus.BAD_GATEWAY, "GOOGLE_API_ERROR",
                "Error en la llamada a Google (" + e.getStatusCode().value() + "): " + detalle);
    }

    private static String enc(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    public record GoogleTokenResponse(String accessToken, String refreshToken, Integer expiresIn,
                                      String scope) {
        static GoogleTokenResponse from(JsonNode json) {
            return new GoogleTokenResponse(
                    json.get("access_token").asText(),
                    json.has("refresh_token") ? json.get("refresh_token").asText(null) : null,
                    json.has("expires_in") ? json.get("expires_in").asInt() : null,
                    json.has("scope") ? json.get("scope").asText(null) : null);
        }
    }

    public record CalendarioRemoto(String calendarId, String summary, String timeZone) {
    }

    public record EventoGoogle(String summary, String description, LocalDate fecha,
                               LocalTime horaInicio, LocalTime horaFin, String timeZone) {
    }
}

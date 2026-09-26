package org.dentalcrm.web.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.google.GoogleCredential;
import org.dentalcrm.domain.google.GoogleCredentialRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.google.GoogleService.EventoGoogle;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GoogleCredentialRepository credRepo(String clientId, String clientSecret) {
        GoogleCredentialRepository repo = mock(GoogleCredentialRepository.class);
        GoogleCredential cred = new GoogleCredential();
        cred.setClientId(clientId);
        cred.setClientSecret(clientSecret);
        cred.setRedirectUri("http://localhost:8080/api/v1/google/callback");
        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cred));
        return repo;
    }

    private GoogleCredentialRepository emptyCredRepo() {
        GoogleCredentialRepository repo = mock(GoogleCredentialRepository.class);
        when(repo.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        return repo;
    }

    @Test
    void urlAutenticacionIncluyeParametros() {
        GoogleService s = new GoogleService(WebClient.builder(),
                "https://accounts.google.com", "https://oauth2.googleapis.com",
                "https://www.googleapis.com", credRepo("cliente-123", "secreto-456"));
        String url = s.urlAutenticacion();
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
        assertTrue(url.contains("client_id=cliente-123"));
        assertTrue(url.contains("redirect_uri=" + java.net.URLEncoder.encode("http://localhost:8080/api/v1/google/callback", java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("scope="));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
    }

    @Test
    void configuracionRequiereClientIdYSecret() {
        assertFalse(new GoogleService(WebClient.builder(), "a", "b", "c", emptyCredRepo()).estaConfigurado());
        assertTrue(new GoogleService(WebClient.builder(), "a", "b", "c", credRepo("id", "secret")).estaConfigurado());
    }

    @Test
    void payloadEventoContieneDatosYCumplePlan() {
        GoogleService s = new GoogleService(WebClient.builder(), "a", "b", "c", credRepo("id", "secret"));
        EventoGoogle e = new EventoGoogle("Cita odontológica - Juan Pérez",
                "Servicio: Limpieza\nOdontólogo: María López\nTeléfono: +593999999",
                LocalDate.of(2026, 9, 16), LocalTime.of(10, 30), LocalTime.of(11, 30),
                "America/Guayaquil");
        Object payload = s.payloadEvento(e);
        Map<?, ?> map = (Map<?, ?>) payload;
        assertEquals("Cita odontológica - Juan Pérez", map.get("summary"));
        @SuppressWarnings("unchecked")
        Map<String, Object> start = (Map<String, Object>) map.get("start");
        assertEquals("2026-09-16T10:30:00", start.get("dateTime"));
        assertEquals("America/Guayaquil", start.get("timeZone"));
        @SuppressWarnings("unchecked")
        Map<String, Object> end = (Map<String, Object>) map.get("end");
        assertEquals("2026-09-16T11:30:00", end.get("dateTime"));
    }

    @Test
    void intercambiarCodigoDevuelveToken() throws Exception {
        JsonNode tokenJson = MAPPER.readTree("""
                {"access_token":"tok123","refresh_token":"ref456","expires_in":3600,"scope":"calendar.events"}
                """);
        GoogleService s = servicioConToken(tokenJson);

        GoogleService.GoogleTokenResponse tok = s.intercambiarCodigo("code-abc");
        assertEquals("tok123", tok.accessToken());
        assertEquals("ref456", tok.refreshToken());
        assertEquals(3600, tok.expiresIn());
    }

    @Test
    void intercambiarCodigoConErrorLanzaBusiness() {
        byte[] cuerpoError = "{\"error\":\"invalid_grant\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        GoogleService s = servicioConError(cuerpoError);

        assertThrows(BusinessException.class, () -> s.intercambiarCodigo("code-malo"));
    }

    private GoogleService servicioConToken(JsonNode respuesta) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec post = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec body = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headers = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec spec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(post);
        when(post.uri(anyString())).thenReturn(body);
        when(body.contentType(any(MediaType.class))).thenReturn(body);
        when(body.body(any(BodyInserter.class))).thenReturn(headers);
        when(headers.retrieve()).thenReturn(spec);
        when(spec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(respuesta));

        return new GoogleService(builderCon(webClient), "a", "b", "c", credRepo("id", "secret"));
    }

    private GoogleService servicioConError(byte[] cuerpo) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec post = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec body = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headers = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec spec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(post);
        when(post.uri(anyString())).thenReturn(body);
        when(body.contentType(any(MediaType.class))).thenReturn(body);
        when(body.body(any(BodyInserter.class))).thenReturn(headers);
        when(headers.retrieve()).thenReturn(spec);
        when(spec.bodyToMono(JsonNode.class))
                .thenReturn(Mono.error(new WebClientResponseException(400, "Bad Request",
                        HttpHeaders.EMPTY, cuerpo, java.nio.charset.StandardCharsets.UTF_8)));

        return new GoogleService(builderCon(webClient), "a", "b", "c", credRepo("id", "secret"));
    }

    private WebClient.Builder builderCon(WebClient webClient) {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        when(builder.clone()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);
        return builder;
    }
}

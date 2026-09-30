package org.dentalcrm.web.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.social.SocialAccount;
import org.dentalcrm.domain.social.SocialAccountRepository;
import org.dentalcrm.domain.social.SocialPlatform;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.SocialTokenCipher;
import org.dentalcrm.web.social.dto.SocialPublishResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialPublishServiceTest {

    private static final String CLAVE = "clave-de-prueba-suficientemente-larga-123";

    private final SocialTokenCipher cipher = spy(new SocialTokenCipher(CLAVE));
    private final AuditService auditoria = mock(AuditService.class);
    private final SocialAccountRepository repositorio = mock(SocialAccountRepository.class);

    private WebClient.RequestBodyUriSpec post;
    private WebClient.RequestBodySpec body;

    @Test
    void publicaEnFacebookSinImagen() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));

        SocialPublishResponse respuesta = servicio(exito("{\"id\":\"111_222\"}"))
                .publicar(7L, "Hola pacientes", null);

        assertEquals("111_222", respuesta.publicacionId());
        assertEquals(SocialPlatform.FACEBOOK, respuesta.platform());
        assertNotNull(respuesta.publicadoEn());
        ArgumentCaptor<String> uri = ArgumentCaptor.forClass(String.class);
        verify(post).uri(uri.capture());
        assertTrue(uri.getValue().endsWith("/PAGE1/feed"), uri.getValue());
        verify(auditoria).registrar(eq("PUBLICAR_EN_REDES"), eq("REDES_SOCIALES"), eq("CUENTA_SOCIAL"), eq(7L));
    }

    @Test
    void publicaEnFacebookConImagen() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        MockMultipartFile imagen = imagen("promo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        SocialPublishResponse respuesta = servicio(exito("{\"id\":\"333\"}"))
                .publicar(7L, "Promo de blanqueo", imagen);

        assertEquals("333", respuesta.publicacionId());
        ArgumentCaptor<String> uri = ArgumentCaptor.forClass(String.class);
        verify(post).uri(uri.capture());
        assertTrue(uri.getValue().endsWith("/PAGE1/photos"), uri.getValue());
        ArgumentCaptor<BodyInserter> cuerpo = ArgumentCaptor.forClass(BodyInserter.class);
        verify(body).body(cuerpo.capture());
        assertNotNull(cuerpo.getValue());
    }

    @Test
    void construyeElCuerpoDelMultipartConLaImagen() {
        byte[] bytes = {9, 8, 7};
        var cuerpo = SocialPublishService.cuerpoFotos("Nuevo horario", "token-secreto", bytes,
                "image/png", "cartel.png");

        assertEquals("Nuevo horario", cuerpo.getFirst("message"));
        assertEquals("token-secreto", cuerpo.getFirst("access_token"));
        HttpEntity<?> fuente = (HttpEntity<?>) cuerpo.getFirst("source");
        assertEquals(MediaType.IMAGE_PNG, fuente.getHeaders().getContentType());
        assertArrayEquals(bytes, (byte[]) fuente.getBody());
        assertEquals("cartel.png", fuente.getHeaders().getContentDisposition().getFilename());
    }

    @Test
    void construyeElCuerpoDelFeed() {
        var cuerpo = SocialPublishService.cuerpoFeed("Días libres", "token");
        assertEquals("Días libres", cuerpo.getFirst("message"));
        assertEquals("token", cuerpo.getFirst("access_token"));
    }

    @Test
    void descifraElTokenAntesDePublicar() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));

        servicio(exito("{\"id\":\"42\"}")).publicar(7L, "Texto", null);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(cipher).decrypt(token.capture());
        assertEquals("token-meta", cipher.decrypt(token.getValue()));
    }

    @Test
    void rechazaCuentaInexistente() {
        when(repositorio.findById(9L)).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(9L, "Texto", null));
        assertEquals("SOCIAL_ACCOUNT_MISSING", error.getCode());
        verify(auditoria, never()).registrar(anyString(), anyString(), anyString(), any());
    }

    @Test
    void rechazaTextoVacio() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(7L, "   ", null));
        assertEquals("SOCIAL_TEXT_REQUIRED", error.getCode());
    }

    @Test
    void rechazaTextoLargo() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(7L, "x".repeat(2201), null));
        assertEquals("SOCIAL_TEXT_TOO_LONG", error.getCode());
    }

    @Test
    void rechazaImagenDeMasDeCincoMB() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        MockMultipartFile imagen = imagen("gigante.jpg", "image/jpeg",
                new byte[(int) SocialPublishService.MAX_IMAGEN_BYTES + 1]);
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(7L, "Texto", imagen));
        assertEquals("SOCIAL_IMAGE_TOO_LARGE", error.getCode());
    }

    @Test
    void rechazaImagenQueNoEsDeImagen() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        MockMultipartFile imagen = imagen("contrato.pdf", "application/pdf", new byte[]{1});
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(7L, "Texto", imagen));
        assertEquals("SOCIAL_IMAGE_INVALID", error.getCode());
    }

    @Test
    void rechazaInstagramPorFaltaDeUrlPublica() {
        SocialAccount cuenta = cuenta(SocialPlatform.INSTAGRAM);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(7L, "Texto", null));
        assertEquals("SOCIAL_URL_PUBLICA_REQUERIDA", error.getCode());
        assertTrue(error.getMessage().contains("URL pública"));
    }

    @Test
    void rechazaTikTokPorFaltaDeUrlPublica() {
        SocialAccount cuenta = cuenta(SocialPlatform.TIKTOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio(exito("{}")).publicar(7L, "Texto", null));
        assertEquals("SOCIAL_URL_PUBLICA_REQUERIDA", error.getCode());
    }

    @Test
    void convierteElErrorDelProveedorEnBusinessException() {
        byte[] cuerpo = "{\"error\":{\"message\":\"Invalid OAuth access token\"}}".getBytes(StandardCharsets.UTF_8);
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));

        BusinessException error = assertThrows(BusinessException.class, () -> servicio(
                Mono.error(new WebClientResponseException(400, "Bad Request", HttpHeaders.EMPTY, cuerpo,
                        StandardCharsets.UTF_8))).publicar(7L, "Texto", null));

        assertEquals("SOCIAL_PUBLISH_FAILED", error.getCode());
        assertTrue(error.getMessage().contains("Invalid OAuth access token"));
        verify(auditoria, never()).registrar(anyString(), anyString(), anyString(), any());
    }

    @Test
    void avisaCuandoElProveedorNoResponde() {
        SocialAccount cuenta = cuenta(SocialPlatform.FACEBOOK);

        when(repositorio.findById(7L)).thenReturn(Optional.of(cuenta));

        BusinessException error = assertThrows(BusinessException.class, () -> servicio(
                Mono.error(new WebClientRequestException(new java.io.IOException("sin red"), HttpMethod.POST,
                        URI.create("https://graph.facebook.com/v21.0/PAGE1/feed"), HttpHeaders.EMPTY)))
                .publicar(7L, "Texto", null));

        assertEquals("SOCIAL_PROVIDER_UNAVAILABLE", error.getCode());
    }

    // --- helpers -------------------------------------------------------------

    private SocialPublishService servicio(Mono<JsonNode> respuesta) {
        WebClient webClient = webClientCon(respuesta);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        when(builder.clone()).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);
        return new SocialPublishService(repositorio, cipher, auditoria, builder);
    }

    private WebClient webClientCon(Mono<JsonNode> respuesta) {
        WebClient webClient = mock(WebClient.class);
        post = mock(WebClient.RequestBodyUriSpec.class);
        body = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headers = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec spec = mock(WebClient.ResponseSpec.class);
        when(webClient.post()).thenReturn(post);
        when(post.uri(anyString())).thenReturn(body);
        when(body.contentType(any(MediaType.class))).thenReturn(body);
        when(body.body(any(BodyInserter.class))).thenReturn(headers);
        when(headers.retrieve()).thenReturn(spec);
        when(spec.bodyToMono(JsonNode.class)).thenReturn(respuesta);
        return webClient;
    }

    private Mono<JsonNode> exito(String json) {
        try {
            return Mono.just(new ObjectMapper().readTree(json));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private SocialAccount cuenta(SocialPlatform platform) {
        SocialAccount cuenta = new SocialAccount();
        cuenta.setId(7L);
        cuenta.setPlatform(platform);
        cuenta.setExternalAccountId(platform == SocialPlatform.TIKTOK ? "TT1" : "PAGE1");
        cuenta.setAccountName("Clínica Dental");
        cuenta.setAccessToken(cipher.encrypt("token-meta"));
        return cuenta;
    }

    private MockMultipartFile imagen(String nombre, String tipo, byte[] contenido) {
        return new MockMultipartFile("imagen", nombre, tipo, contenido);
    }
}

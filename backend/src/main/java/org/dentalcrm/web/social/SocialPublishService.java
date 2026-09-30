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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

@Service
public class SocialPublishService {

    static final int MAX_TEXTO = 2200;
    static final long MAX_IMAGEN_BYTES = 5L * 1024 * 1024;
    static final String GRAFICA_BASE = "https://graph.facebook.com/v21.0/";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] TIPOS_IMAGEN = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
    private static final String MODULO = "REDES_SOCIALES";

    private final SocialAccountRepository accountRepository;
    private final SocialTokenCipher tokenCipher;
    private final AuditService auditService;
    private final WebClient webClient;

    public SocialPublishService(SocialAccountRepository accountRepository,
                                SocialTokenCipher tokenCipher,
                                AuditService auditService,
                                WebClient.Builder builder) {
        this.accountRepository = accountRepository;
        this.tokenCipher = tokenCipher;
        this.auditService = auditService;
        this.webClient = builder.clone().build();
    }

    public SocialPublishResponse publicar(Long cuentaId, String texto, MultipartFile imagen) {
        SocialAccount cuenta = cuentaId == null
                ? null
                : accountRepository.findById(cuentaId).orElse(null);
        if (cuenta == null) {
            throw new BusinessException("SOCIAL_ACCOUNT_MISSING", "La cuenta seleccionada ya no está conectada");
        }

        String contenido = texto == null ? "" : texto.trim();
        if (contenido.isEmpty()) {
            throw new BusinessException("SOCIAL_TEXT_REQUIRED", "Escribe el texto de la publicación");
        }
        if (contenido.length() > MAX_TEXTO) {
            throw new BusinessException("SOCIAL_TEXT_TOO_LONG",
                    "El texto no puede superar " + MAX_TEXTO + " caracteres");
        }

        if (cuenta.getPlatform() == SocialPlatform.INSTAGRAM) {
            throw new BusinessException("SOCIAL_URL_PUBLICA_REQUERIDA",
                    "Instagram sólo acepta fotos servidas desde una URL pública y el CRM todavía no aloja imágenes. "
                            + "Por ahora la publicación está disponible en Facebook.");
        }
        if (cuenta.getPlatform() == SocialPlatform.TIKTOK) {
            throw new BusinessException("SOCIAL_URL_PUBLICA_REQUERIDA",
                    "TikTok sólo acepta fotos con URL pública (PULL_FROM_URL) y el CRM todavía no aloja imágenes. "
                            + "Por ahora la publicación está disponible en Facebook.");
        }

        String token = tokenCipher.decrypt(cuenta.getAccessToken());
        if (token == null || token.isBlank()) {
            throw new BusinessException("SOCIAL_TOKEN_ERROR", "Vuelve a conectar la cuenta de " + cuenta.getAccountName());
        }

        byte[] contenidoImagen = null;
        String tipoImagen = null;
        String nombreImagen = null;
        if (imagen != null && !imagen.isEmpty()) {
            if (imagen.getSize() > MAX_IMAGEN_BYTES) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "SOCIAL_IMAGE_TOO_LARGE",
                        "La imagen no puede superar 5 MB");
            }
            String contentType = imagen.getContentType() == null
                    ? ""
                    : imagen.getContentType().toLowerCase(Locale.ROOT);
            if (!esTipoImagen(contentType)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "SOCIAL_IMAGE_INVALID",
                        "La imagen debe estar en formato JPEG, PNG o WebP");
            }
            try {
                contenidoImagen = imagen.getBytes();
            } catch (IOException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "SOCIAL_IMAGE_INVALID",
                        "No se pudo leer la imagen seleccionada");
            }
            tipoImagen = contentType;
            nombreImagen = nombreArchivo(imagen.getOriginalFilename());
        }

        SocialPublishResponse respuesta = publicarEnFacebook(cuenta, token, contenido,
                contenidoImagen, tipoImagen, nombreImagen);
        auditService.registrar("PUBLICAR_EN_REDES", MODULO, "CUENTA_SOCIAL", cuenta.getId());
        return respuesta;
    }

    private SocialPublishResponse publicarEnFacebook(SocialAccount cuenta, String token, String texto,
                                                     byte[] imagen, String tipoImagen, String nombreImagen) {
        String destino = GRAFICA_BASE + cuenta.getExternalAccountId();
        JsonNode respuesta;
        if (imagen == null) {
            respuesta = bloquear(webClient.post()
                    .uri(destino + "/feed")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(cuerpoFeed(texto, token)))
                    .retrieve().bodyToMono(JsonNode.class), "Facebook");
        } else {
            respuesta = bloquear(webClient.post()
                    .uri(destino + "/photos")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(cuerpoFotos(texto, token, imagen, tipoImagen, nombreImagen)))
                    .retrieve().bodyToMono(JsonNode.class), "Facebook");
        }

        String publicacionId = respuesta == null ? null : respuesta.path("id").asText(null);
        if (publicacionId == null || publicacionId.isBlank()) {
            throw new BusinessException("SOCIAL_PUBLISH_FAILED", "Facebook no confirmó la publicación");
        }
        return new SocialPublishResponse(cuenta.getId(), cuenta.getPlatform(), publicacionId, Instant.now());
    }

    static MultiValueMap<String, String> cuerpoFeed(String texto, String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("message", texto);
        form.add("access_token", token);
        return form;
    }

    static MultiValueMap<String, Object> cuerpoFotos(String texto, String token, byte[] imagen,
                                                     String tipoImagen, String nombreImagen) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.parseMediaType(tipoImagen));
        cabeceras.setContentDispositionFormData("source", nombreImagen);
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("message", texto);
        multipart.add("access_token", token);
        multipart.add("source", new HttpEntity<>(imagen, cabeceras));
        return multipart;
    }

    private <T> T bloquear(Mono<T> solicitud, String red) {
        try {
            return solicitud.block();
        } catch (WebClientResponseException e) {
            String detalle = detalleError(e);
            throw new BusinessException("SOCIAL_PUBLISH_FAILED",
                    "No se pudo publicar en " + red + (detalle == null ? "" : ": " + detalle));
        } catch (WebClientRequestException e) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOCIAL_PROVIDER_UNAVAILABLE",
                    "No se pudo contactar con " + red + ". Inténtalo de nuevo en unos minutos.");
        }
    }

    private String detalleError(WebClientResponseException e) {
        String cuerpo = e.getResponseBodyAsString();
        if (cuerpo == null || cuerpo.isBlank()) return null;
        try {
            JsonNode nodo = MAPPER.readTree(cuerpo);
            String mensaje = nodo.path("error").path("message").asText(null);
            if (mensaje == null || mensaje.isBlank()) {
                mensaje = nodo.path("message").asText(null);
            }
            return mensaje == null || mensaje.isBlank() ? null : mensaje;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean esTipoImagen(String contentType) {
        for (String tipo : TIPOS_IMAGEN) {
            if (tipo.equals(contentType)) return true;
        }
        return false;
    }

    private String nombreArchivo(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) return "publicacion.jpg";
        String normalizado = nombreOriginal.replace('\\', '/');
        return normalizado.substring(normalizado.lastIndexOf('/') + 1);
    }
}

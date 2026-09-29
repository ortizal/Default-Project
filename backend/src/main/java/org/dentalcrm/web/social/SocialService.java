package org.dentalcrm.web.social;

import com.fasterxml.jackson.databind.JsonNode;
import org.dentalcrm.domain.social.SocialAccount;
import org.dentalcrm.domain.social.SocialAccountRepository;
import org.dentalcrm.domain.social.SocialOAuthRequest;
import org.dentalcrm.domain.social.SocialOAuthRequestRepository;
import org.dentalcrm.domain.social.SocialPlatform;
import org.dentalcrm.domain.social.SocialProvider;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.SocialTokenCipher;
import org.dentalcrm.web.social.dto.SocialAccountResponse;
import org.dentalcrm.web.social.dto.SocialProviderStatusResponse;
import org.dentalcrm.web.social.dto.SocialStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class SocialService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String META_SCOPE = "pages_show_list,instagram_basic,pages_read_engagement";
    private static final String TIKTOK_SCOPE = "user.info.basic,user.info.profile";

    private final SocialAccountRepository accountRepository;
    private final SocialOAuthRequestRepository requestRepository;
    private final SocialTokenCipher tokenCipher;
    private final WebClient webClient;
    private final String metaClientId;
    private final String metaClientSecret;
    private final String metaRedirectUri;
    private final String tiktokClientKey;
    private final String tiktokClientSecret;
    private final String tiktokRedirectUri;
    private final String frontendUrl;

    public SocialService(SocialAccountRepository accountRepository,
                         SocialOAuthRequestRepository requestRepository,
                         SocialTokenCipher tokenCipher,
                         WebClient.Builder builder,
                         @Value("${app.social.meta.client-id:}") String metaClientId,
                         @Value("${app.social.meta.client-secret:}") String metaClientSecret,
                         @Value("${app.social.meta.redirect-uri:}") String metaRedirectUri,
                         @Value("${app.social.tiktok.client-key:}") String tiktokClientKey,
                         @Value("${app.social.tiktok.client-secret:}") String tiktokClientSecret,
                         @Value("${app.social.tiktok.redirect-uri:}") String tiktokRedirectUri,
                         @Value("${app.social.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.accountRepository = accountRepository;
        this.requestRepository = requestRepository;
        this.tokenCipher = tokenCipher;
        this.webClient = builder.clone().build();
        this.metaClientId = metaClientId;
        this.metaClientSecret = metaClientSecret;
        this.metaRedirectUri = metaRedirectUri;
        this.tiktokClientKey = tiktokClientKey;
        this.tiktokClientSecret = tiktokClientSecret;
        this.tiktokRedirectUri = tiktokRedirectUri;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(readOnly = true)
    public SocialStatusResponse status() {
        return new SocialStatusResponse(providerStatus(SocialProvider.META), providerStatus(SocialProvider.TIKTOK));
    }

    @Transactional
    public String iniciarConexion(SocialProvider provider) {
        if (!estaConfigurado(provider)) {
            throw new BusinessException("SOCIAL_NOT_CONFIGURED", "La integración solicitada no está configurada en el servidor");
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SocialOAuthRequest request = new SocialOAuthRequest();
        request.setState(state);
        request.setProvider(provider);
        request.setTenantId(TenantContext.actualOrDefault());
        request.setExpiresAt(Instant.now().plusSeconds(600));
        requestRepository.deleteAll(requestRepository.findAll().stream()
                .filter(pending -> pending.getExpiresAt().isBefore(Instant.now())).toList());
        requestRepository.save(request);

        if (provider == SocialProvider.META) {
            return UriComponentsBuilder.fromUriString("https://www.facebook.com/v21.0/dialog/oauth")
                    .queryParam("client_id", metaClientId)
                    .queryParam("redirect_uri", metaRedirectUri)
                    .queryParam("response_type", "code")
                    .queryParam("scope", META_SCOPE)
                    .queryParam("state", state)
                    .build().encode().toUriString();
        }
        return UriComponentsBuilder.fromUriString("https://www.tiktok.com/v2/auth/authorize/")
                .queryParam("client_key", tiktokClientKey)
                .queryParam("redirect_uri", tiktokRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", TIKTOK_SCOPE)
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    public Long tenantDeSolicitud(SocialProvider provider, String state) {
        return requestRepository.findByStateAndProvider(state, provider)
                .filter(pending -> pending.getExpiresAt().isAfter(Instant.now()))
                .map(SocialOAuthRequest::getTenantId)
                .orElseThrow(() -> new BusinessException("SOCIAL_STATE_INVALID", "La solicitud OAuth expiró o no es válida"));
    }

    @Transactional
    public void completarConexion(SocialProvider provider, String code, String state) {
        SocialOAuthRequest request = requestRepository.findByStateAndProvider(state, provider)
                .filter(pending -> pending.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessException("SOCIAL_STATE_INVALID", "La solicitud OAuth expiró o no es válida"));
        if (code == null || code.isBlank()) {
            throw new BusinessException("SOCIAL_AUTH_ERROR", "El proveedor no devolvió un código de autorización");
        }
        if (provider == SocialProvider.META) completarMeta(code);
        else completarTikTok(code);
        requestRepository.delete(request);
    }

    @Transactional
    public void desconectar(SocialProvider provider) {
        if (provider == SocialProvider.META) {
            accountRepository.deleteByPlatform(SocialPlatform.FACEBOOK);
            accountRepository.deleteByPlatform(SocialPlatform.INSTAGRAM);
        } else {
            accountRepository.deleteByPlatform(SocialPlatform.TIKTOK);
        }
    }

    private SocialProviderStatusResponse providerStatus(SocialProvider provider) {
        List<SocialAccountResponse> accounts = platforms(provider).stream()
                .flatMap(platform -> accountRepository.findByPlatformOrderByAccountName(platform).stream())
                .map(account -> new SocialAccountResponse(account.getId(), account.getPlatform(), account.getAccountName()))
                .toList();
        return new SocialProviderStatusResponse(estaConfigurado(provider), accounts);
    }

    private boolean estaConfigurado(SocialProvider provider) {
        if (provider == SocialProvider.META) {
            return presente(metaClientId) && presente(metaClientSecret) && presente(metaRedirectUri);
        }
        return presente(tiktokClientKey) && presente(tiktokClientSecret) && presente(tiktokRedirectUri);
    }

    private void completarMeta(String code) {
        JsonNode token = webClient.get().uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/v21.0/oauth/access_token")
                        .queryParam("client_id", metaClientId).queryParam("client_secret", metaClientSecret)
                        .queryParam("redirect_uri", metaRedirectUri).queryParam("code", code).build().encode().toUri())
                .retrieve().bodyToMono(JsonNode.class).block();
        String accessToken = token == null ? null : token.path("access_token").asText(null);
        if (!presente(accessToken)) throw new BusinessException("SOCIAL_AUTH_ERROR", "Meta no devolvió un token válido");

        JsonNode pages = webClient.get().uri(uriBuilder -> uriBuilder
                        .scheme("https").host("graph.facebook.com").path("/v21.0/me/accounts")
                        .queryParam("fields", "id,name,access_token,instagram_business_account{id,username}")
                        .queryParam("access_token", accessToken).build())
                .retrieve().bodyToMono(JsonNode.class).block();
        JsonNode data = pages == null ? null : pages.path("data");
        if (data != null && data.isArray()) {
            for (JsonNode page : data) {
                String pageToken = page.path("access_token").asText(accessToken);
                guardarCuenta(SocialPlatform.FACEBOOK, page.path("id").asText(null),
                        page.path("name").asText("Facebook"), pageToken, null,
                        token.path("expires_in").asLong(0));
                JsonNode instagram = page.path("instagram_business_account");
                if (instagram.isObject()) {
                    guardarCuenta(SocialPlatform.INSTAGRAM, instagram.path("id").asText(null),
                            instagram.path("username").asText("Instagram"), pageToken, null,
                            token.path("expires_in").asLong(0));
                }
            }
        }
    }

    private void completarTikTok(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_key", tiktokClientKey);
        form.add("client_secret", tiktokClientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", tiktokRedirectUri);
        JsonNode token = webClient.post().uri("https://open.tiktokapis.com/v2/oauth/token/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve().bodyToMono(JsonNode.class).block();
        JsonNode tokenData = token == null ? null : token.path("data");
        String accessToken = tokenData == null ? null : tokenData.path("access_token").asText(null);
        if (!presente(accessToken)) throw new BusinessException("SOCIAL_AUTH_ERROR", "TikTok no devolvió un token válido");

        JsonNode profile = webClient.get().uri(uriBuilder -> uriBuilder.scheme("https").host("open.tiktokapis.com")
                        .path("/v2/user/info/").queryParam("fields", "open_id,display_name").build())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve().bodyToMono(JsonNode.class).block();
        JsonNode user = profile == null ? null : profile.path("data").path("user");
        guardarCuenta(SocialPlatform.TIKTOK, user == null ? null : user.path("open_id").asText(null),
                user == null ? "TikTok" : user.path("display_name").asText("TikTok"), accessToken,
                tokenData.path("refresh_token").asText(null), tokenData.path("expires_in").asLong(0));
    }

    private void guardarCuenta(SocialPlatform platform, String externalId, String name,
                               String accessToken, String refreshToken, long expiresIn) {
        if (!presente(externalId)) return;
        SocialAccount account = accountRepository.findFirstByPlatformAndExternalAccountId(platform, externalId)
                .orElseGet(SocialAccount::new);
        account.setPlatform(platform);
        account.setExternalAccountId(externalId);
        account.setAccountName(name);
        account.setAccessToken(tokenCipher.encrypt(accessToken));
        account.setRefreshToken(tokenCipher.encrypt(refreshToken));
        account.setExpiresAt(expiresIn > 0 ? Instant.now().plusSeconds(expiresIn) : null);
        accountRepository.save(account);
    }

    private List<SocialPlatform> platforms(SocialProvider provider) {
        return provider == SocialProvider.META
                ? List.of(SocialPlatform.FACEBOOK, SocialPlatform.INSTAGRAM)
                : List.of(SocialPlatform.TIKTOK);
    }

    private boolean presente(String value) {
        return value != null && !value.isBlank();
    }

    public String urlRetorno(boolean conectado) {
        return frontendUrl.replaceAll("/+$", "") + "/redes-sociales?social=" + (conectado ? "connected" : "error");
    }
}
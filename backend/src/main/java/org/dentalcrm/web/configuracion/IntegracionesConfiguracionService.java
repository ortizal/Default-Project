package org.dentalcrm.web.configuracion;

import org.dentalcrm.domain.configuracion.IntegracionConfiguracion;
import org.dentalcrm.domain.configuracion.IntegracionConfiguracionRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.IntegracionSecretCipher;
import org.dentalcrm.web.configuracion.dto.CorreoConfigRequest;
import org.dentalcrm.web.configuracion.dto.IntegracionesConfigResponse;
import org.dentalcrm.web.configuracion.dto.MetaConfigRequest;
import org.dentalcrm.web.configuracion.dto.OpenWaConfigRequest;
import org.dentalcrm.web.configuracion.dto.TikTokConfigRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
public class IntegracionesConfiguracionService {

    private static final String PREFIJO_CIFRADO = "enc:v1:";
    private static final String MODULO = "INTEGRACIONES";

    private final IntegracionConfiguracionRepository repository;
    private final AuditService auditService;
    private final IntegracionSecretCipher cipher;
    private final Defaults defaults;

    public IntegracionesConfiguracionService(
            IntegracionConfiguracionRepository repository,
            AuditService auditService,
            IntegracionSecretCipher cipher,
            @Value("${app.openwa.url:}") String openwaUrl,
            @Value("${app.openwa.api-key:}") String openwaApiKey,
            @Value("${app.openwa.webhook-url:}") String openwaWebhookUrl,
            @Value("${app.openwa.webhook-secret:}") String openwaWebhookSecret,
            @Value("${app.mail.host:}") String mailHost,
            @Value("${app.mail.port:587}") int mailPort,
            @Value("${app.mail.username:}") String mailUsername,
            @Value("${app.mail.password:}") String mailPassword,
            @Value("${app.mail.from:}") String mailFrom,
            @Value("${app.social.meta.client-id:}") String metaClientId,
            @Value("${app.social.meta.client-secret:}") String metaClientSecret,
            @Value("${app.social.meta.redirect-uri:}") String metaRedirectUri,
            @Value("${app.social.tiktok.client-key:}") String tikTokClientKey,
            @Value("${app.social.tiktok.client-secret:}") String tikTokClientSecret,
            @Value("${app.social.tiktok.redirect-uri:}") String tikTokRedirectUri) {
        this.repository = repository;
        this.auditService = auditService;
        this.cipher = cipher;
        this.defaults = new Defaults(openwaUrl, openwaApiKey, openwaWebhookUrl, openwaWebhookSecret,
            mailHost, mailPort, mailUsername, mailPassword, mailFrom, metaClientId,
            metaClientSecret, metaRedirectUri, tikTokClientKey, tikTokClientSecret, tikTokRedirectUri);
    }

    @Transactional(readOnly = true)
    public IntegracionesConfigResponse obtener() {
        RuntimeConfig config = runtimeConfig();
        return respuesta(config);
    }

    @Transactional
    public IntegracionesConfigResponse actualizarOpenWa(OpenWaConfigRequest request) {
        validarUrl(request.url(), "La URL de OpenWA");
        validarUrlOpcional(request.webhookUrl(), "La URL del webhook");
        IntegracionConfiguracion config = obtenerOCrear();
        config.setOpenwaUrl(limpiar(request.url()));
        config.setOpenwaWebhookUrl(limpiar(request.webhookUrl()));
        if (noVacio(request.apiKey())) config.setOpenwaApiKey(cifrar(request.apiKey()));
        if (noVacio(request.webhookSecret())) config.setOpenwaWebhookSecret(cifrar(request.webhookSecret()));
        repository.save(config);
        auditar("ACTUALIZAR_OPENWA", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse actualizarCorreo(CorreoConfigRequest request) {
        IntegracionConfiguracion config = obtenerOCrear();
        config.setMailHost(limpiar(request.host()));
        config.setMailPort(request.puerto());
        config.setMailUsername(limpiar(request.usuario()));
        config.setMailFrom(limpiar(request.remitente()));
        if (noVacio(request.password())) config.setMailPassword(cifrar(request.password()));
        repository.save(config);
        auditar("ACTUALIZAR_SMTP", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse actualizarMeta(MetaConfigRequest request) {
        validarUrlOpcional(request.redirectUri(), "La URL de retorno de Meta");
        IntegracionConfiguracion config = obtenerOCrear();
        config.setMetaClientId(limpiar(request.clientId()));
        config.setMetaRedirectUri(limpiar(request.redirectUri()));
        if (noVacio(request.clientSecret())) config.setMetaClientSecret(cifrar(request.clientSecret()));
        repository.save(config);
        auditar("ACTUALIZAR_META", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse actualizarTikTok(TikTokConfigRequest request) {
        validarUrlOpcional(request.redirectUri(), "La URL de retorno de TikTok");
        IntegracionConfiguracion config = obtenerOCrear();
        config.setTiktokClientKey(limpiar(request.clientKey()));
        config.setTiktokRedirectUri(limpiar(request.redirectUri()));
        if (noVacio(request.clientSecret())) config.setTiktokClientSecret(cifrar(request.clientSecret()));
        repository.save(config);
        auditar("ACTUALIZAR_TIKTOK", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse restablecerOpenWa() {
        IntegracionConfiguracion config = obtenerOCrear();
        config.setOpenwaUrl(null);
        config.setOpenwaApiKey(null);
        config.setOpenwaWebhookUrl(null);
        config.setOpenwaWebhookSecret(null);
        repository.save(config);
        auditar("RESTABLECER_OPENWA", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse restablecerCorreo() {
        IntegracionConfiguracion config = obtenerOCrear();
        config.setMailHost(null);
        config.setMailPort(null);
        config.setMailUsername(null);
        config.setMailPassword(null);
        config.setMailFrom(null);
        repository.save(config);
        auditar("RESTABLECER_SMTP", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse restablecerMeta() {
        IntegracionConfiguracion config = obtenerOCrear();
        config.setMetaClientId(null);
        config.setMetaClientSecret(null);
        config.setMetaRedirectUri(null);
        repository.save(config);
        auditar("RESTABLECER_META", config);
        return respuesta(runtimeConfig());
    }

    @Transactional
    public IntegracionesConfigResponse restablecerTikTok() {
        IntegracionConfiguracion config = obtenerOCrear();
        config.setTiktokClientKey(null);
        config.setTiktokClientSecret(null);
        config.setTiktokRedirectUri(null);
        repository.save(config);
        auditar("RESTABLECER_TIKTOK", config);
        return respuesta(runtimeConfig());
    }

    @Transactional(readOnly = true)
    public OpenWaRuntime openWa() {
        RuntimeConfig config = runtimeConfig();
        return new OpenWaRuntime(config.openwaUrl, config.openwaApiKey,
                config.openwaWebhookUrl, config.openwaWebhookSecret);
    }

    @Transactional(readOnly = true)
    public MailRuntime correo() {
        RuntimeConfig config = runtimeConfig();
        return new MailRuntime(config.mailHost, config.mailPort, config.mailUsername,
                config.mailPassword, config.mailFrom);
    }

    @Transactional(readOnly = true)
    public SocialRuntime redesSociales() {
        RuntimeConfig config = runtimeConfig();
        return new SocialRuntime(config.metaClientId, config.metaClientSecret, config.metaRedirectUri,
                config.tikTokClientKey, config.tikTokClientSecret, config.tikTokRedirectUri);
    }

    private RuntimeConfig runtimeConfig() {
        IntegracionConfiguracion stored = repository.findByTenantId(TenantContext.actualOrDefault()).orElse(null);
        return new RuntimeConfig(
                elegir(stored == null ? null : stored.getOpenwaUrl(), defaults.openwaUrl),
                secreto(stored == null ? null : stored.getOpenwaApiKey(), defaults.openwaApiKey),
                elegir(stored == null ? null : stored.getOpenwaWebhookUrl(), defaults.openwaWebhookUrl),
                secreto(stored == null ? null : stored.getOpenwaWebhookSecret(), defaults.openwaWebhookSecret),
                elegir(stored == null ? null : stored.getMailHost(), defaults.mailHost),
                stored != null && stored.getMailPort() != null ? stored.getMailPort() : defaults.mailPort,
                elegir(stored == null ? null : stored.getMailUsername(), defaults.mailUsername),
                secreto(stored == null ? null : stored.getMailPassword(), defaults.mailPassword),
                elegir(stored == null ? null : stored.getMailFrom(), defaults.mailFrom),
                elegir(stored == null ? null : stored.getMetaClientId(), defaults.metaClientId),
                secreto(stored == null ? null : stored.getMetaClientSecret(), defaults.metaClientSecret),
                elegir(stored == null ? null : stored.getMetaRedirectUri(), defaults.metaRedirectUri),
                elegir(stored == null ? null : stored.getTiktokClientKey(), defaults.tikTokClientKey),
                secreto(stored == null ? null : stored.getTiktokClientSecret(), defaults.tikTokClientSecret),
                elegir(stored == null ? null : stored.getTiktokRedirectUri(), defaults.tikTokRedirectUri),
                stored != null && (stored.getOpenwaUrl() != null || stored.getOpenwaApiKey() != null
                    || stored.getOpenwaWebhookUrl() != null || stored.getOpenwaWebhookSecret() != null),
                stored != null && (stored.getMailHost() != null || stored.getMailPort() != null
                    || stored.getMailUsername() != null || stored.getMailPassword() != null
                    || stored.getMailFrom() != null),
                stored != null && (stored.getMetaClientId() != null || stored.getMetaClientSecret() != null
                    || stored.getMetaRedirectUri() != null),
                stored != null && (stored.getTiktokClientKey() != null || stored.getTiktokClientSecret() != null
                    || stored.getTiktokRedirectUri() != null));
    }

    private IntegracionesConfigResponse respuesta(RuntimeConfig config) {
        boolean openwaConfigured = noVacio(config.openwaUrl) && noVacio(config.openwaApiKey);
        boolean mailConfigured = noVacio(config.mailHost) && noVacio(config.mailFrom);
        return new IntegracionesConfigResponse(
                new IntegracionesConfigResponse.OpenWa(config.openwaUrl, config.openwaWebhookUrl,
                        openwaConfigured, noVacio(config.openwaApiKey), noVacio(config.openwaWebhookSecret),
                        !config.openwaOverride),
                new IntegracionesConfigResponse.Correo(config.mailHost, config.mailPort,
                        config.mailUsername, config.mailFrom, mailConfigured,
                    noVacio(config.mailPassword), !config.mailOverride),
                new IntegracionesConfigResponse.Meta(config.metaClientId, config.metaRedirectUri,
                    noVacio(config.metaClientId) && noVacio(config.metaClientSecret)
                        && noVacio(config.metaRedirectUri),
                    noVacio(config.metaClientSecret), !config.metaOverride),
                new IntegracionesConfigResponse.TikTok(config.tikTokClientKey, config.tikTokRedirectUri,
                    noVacio(config.tikTokClientKey) && noVacio(config.tikTokClientSecret)
                        && noVacio(config.tikTokRedirectUri),
                    noVacio(config.tikTokClientSecret), !config.tikTokOverride));
    }

    private IntegracionConfiguracion obtenerOCrear() {
        return repository.findByTenantId(TenantContext.actualOrDefault()).orElseGet(() -> {
            IntegracionConfiguracion config = new IntegracionConfiguracion();
            config.setTenantId(TenantContext.actualOrDefault());
            return config;
        });
    }

    private void auditar(String accion, IntegracionConfiguracion config) {
        auditService.registrar(accion, MODULO, "CONFIGURACION_INTEGRACION", config.getTenantId());
    }

    private String cifrar(String value) {
        return PREFIJO_CIFRADO + cipher.encrypt(value.trim());
    }

    private String secreto(String stored, String fallback) {
        if (!noVacio(stored)) return fallback;
        return stored.startsWith(PREFIJO_CIFRADO)
                ? cipher.decrypt(stored.substring(PREFIJO_CIFRADO.length()))
                : stored;
    }

    private String elegir(String stored, String fallback) {
        return noVacio(stored) ? stored : fallback;
    }

    private boolean noVacio(String value) {
        return value != null && !value.isBlank();
    }

    private String limpiar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validarUrl(String value, String label) {
        if (!noVacio(value)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "URL_REQUERIDA", label + " es obligatoria");
        }
        validarUrlOpcional(value, label);
    }

    private void validarUrlOpcional(String value, String label) {
        if (!noVacio(value)) return;
        try {
            URI uri = URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "URL_INVALIDA", label + " no es válida");
        }
    }

    public record OpenWaRuntime(String url, String apiKey, String webhookUrl, String webhookSecret) {}
    public record MailRuntime(String host, int port, String username, String password, String from) {}
    public record SocialRuntime(String metaClientId, String metaClientSecret, String metaRedirectUri,
                                String tikTokClientKey, String tikTokClientSecret, String tikTokRedirectUri) {}

    private record RuntimeConfig(String openwaUrl, String openwaApiKey, String openwaWebhookUrl,
                                 String openwaWebhookSecret, String mailHost, int mailPort,
                                 String mailUsername, String mailPassword, String mailFrom,
                                 String metaClientId, String metaClientSecret, String metaRedirectUri,
                                 String tikTokClientKey, String tikTokClientSecret, String tikTokRedirectUri,
                                 boolean openwaOverride, boolean mailOverride, boolean metaOverride,
                                 boolean tikTokOverride) {}

    private record Defaults(String openwaUrl, String openwaApiKey, String openwaWebhookUrl,
                            String openwaWebhookSecret, String mailHost, int mailPort,
                            String mailUsername, String mailPassword, String mailFrom,
                            String metaClientId, String metaClientSecret, String metaRedirectUri,
                            String tikTokClientKey, String tikTokClientSecret, String tikTokRedirectUri) {}
}
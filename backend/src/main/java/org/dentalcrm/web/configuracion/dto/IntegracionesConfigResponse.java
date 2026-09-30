package org.dentalcrm.web.configuracion.dto;

public record IntegracionesConfigResponse(
        OpenWa openWa,
        Correo correo,
        Meta meta,
        TikTok tikTok
) {
    public record OpenWa(
            String url,
            String webhookUrl,
            boolean configurado,
            boolean apiKeyConfigurada,
            boolean webhookSecretConfigurado,
            boolean usaValoresEntorno
    ) {}

    public record Correo(
            String host,
            int puerto,
            String usuario,
            String remitente,
            boolean configurado,
            boolean passwordConfigurada,
            boolean usaValoresEntorno
    ) {}

    public record Meta(
            String clientId,
            String redirectUri,
            boolean configurado,
            boolean clientSecretConfigurado,
            boolean usaValoresEntorno
    ) {}

    public record TikTok(
            String clientKey,
            String redirectUri,
            boolean configurado,
            boolean clientSecretConfigurado,
            boolean usaValoresEntorno
    ) {}
}
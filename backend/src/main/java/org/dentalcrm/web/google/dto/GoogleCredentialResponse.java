package org.dentalcrm.web.google.dto;

public record GoogleCredentialResponse(
        Long id,
        String clientId,
        String clientSecret,
        String redirectUri,
        String authBaseUrl,
        String oauthBaseUrl,
        String apiBaseUrl,
        Boolean configurada
) {}

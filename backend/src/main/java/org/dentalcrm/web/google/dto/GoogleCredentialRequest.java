package org.dentalcrm.web.google.dto;

public record GoogleCredentialRequest(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authBaseUrl,
        String oauthBaseUrl,
        String apiBaseUrl
) {}

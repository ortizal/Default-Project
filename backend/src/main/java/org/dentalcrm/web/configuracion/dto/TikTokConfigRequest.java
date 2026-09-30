package org.dentalcrm.web.configuracion.dto;

import jakarta.validation.constraints.Size;

public record TikTokConfigRequest(
        @Size(max = 255) String clientKey,
        @Size(max = 4000) String clientSecret,
        @Size(max = 1000) String redirectUri
) {}
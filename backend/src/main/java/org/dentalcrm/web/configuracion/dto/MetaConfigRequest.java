package org.dentalcrm.web.configuracion.dto;

import jakarta.validation.constraints.Size;

public record MetaConfigRequest(
        @Size(max = 255) String clientId,
        @Size(max = 4000) String clientSecret,
        @Size(max = 1000) String redirectUri
) {}
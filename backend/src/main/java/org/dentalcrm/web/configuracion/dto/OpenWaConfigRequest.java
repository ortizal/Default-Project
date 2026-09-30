package org.dentalcrm.web.configuracion.dto;

import jakarta.validation.constraints.Size;

public record OpenWaConfigRequest(
        @Size(max = 500) String url,
        @Size(max = 4000) String apiKey,
        @Size(max = 1000) String webhookUrl,
        @Size(max = 4000) String webhookSecret
) {}
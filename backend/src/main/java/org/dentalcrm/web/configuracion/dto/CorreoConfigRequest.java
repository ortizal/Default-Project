package org.dentalcrm.web.configuracion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CorreoConfigRequest(
        @Size(max = 255) String host,
        @Min(1) @Max(65535) Integer puerto,
        @Size(max = 255) String usuario,
        @Size(max = 4000) String password,
        @Email @Size(max = 255) String remitente
) {}
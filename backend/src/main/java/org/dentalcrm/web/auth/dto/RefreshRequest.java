package org.dentalcrm.web.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "El token es obligatorio") String token
) {}
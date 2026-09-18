package org.dentalcrm.web.auth.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String username,
        List<String> roles
) {
    public static LoginResponse of(String token, long expiresInMs, String username, List<String> roles) {
        return new LoginResponse(token, "Bearer", expiresInMs, username, roles);
    }
}
package org.dentalcrm.web.usuario.dto;

import org.dentalcrm.domain.usuario.Usuario;

import java.time.Instant;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String nombres,
        String apellidos,
        String telefono,
        String estado,
        List<String> roles,
        Instant ultimoLogin,
        Instant createdAt,
        Instant updatedAt
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getNombres(), u.getApellidos(),
                u.getTelefono(), u.getEstado(),
                u.getRoles().stream().map(r -> r.getCodigo()).sorted().toList(),
                u.getUltimoLogin(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
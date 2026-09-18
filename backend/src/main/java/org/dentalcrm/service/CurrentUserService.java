package org.dentalcrm.service;

import org.dentalcrm.domain.usuario.Usuario;
import org.dentalcrm.domain.usuario.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<String> usernameActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud)) {
            return Optional.empty();
        }
        return Optional.of(ud.getUsername());
    }

    public Optional<Usuario> usuarioActual() {
        return usernameActual().flatMap(usuarioRepository::buscarGlobalPorUsername);
    }

    public Optional<Long> idUsuarioActual() {
        return usuarioActual().map(Usuario::getId);
    }
}
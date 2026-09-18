package org.dentalcrm.web.auth;

import org.dentalcrm.domain.usuario.Usuario;
import org.dentalcrm.domain.usuario.UsuarioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.security.JwtService;
import org.dentalcrm.web.auth.dto.LoginRequest;
import org.dentalcrm.web.auth.dto.LoginResponse;
import org.dentalcrm.web.auth.dto.RefreshRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserDetailsService userDetailsService,
                       UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.buscarGlobalPorUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Usuario o contraseña incorrectos"));

        TenantContext.set(usuario.getTenantId());
        String token = jwtService.generateToken(userDetails, usuario.getTenantId());

        usuario.setUltimoLogin(Instant.now());
        usuarioRepository.save(usuario);

        List<String> roles = usuario.getRoles().stream().map(r -> r.getCodigo()).toList();

        return LoginResponse.of(token, expirationMs, userDetails.getUsername(), roles);
    }

    public LoginResponse refresh(RefreshRequest request) {
        String username = jwtService.extractUsername(request.token());
        if (username == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "Token inválido");
        }
        Usuario usuario = usuarioRepository.buscarGlobalPorUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "Token inválido"));
        String token = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                usuario.getUsername(), usuario.getPassword(),
                "ACTIVO".equals(usuario.getEstado()), true, true, true, List.of()), usuario.getTenantId());
        return LoginResponse.of(token, expirationMs, usuario.getUsername(),
                usuario.getRoles().stream().map(r -> r.getCodigo()).toList());
    }
}
package org.dentalcrm.web.auth;

import org.dentalcrm.domain.usuario.Permiso;
import org.dentalcrm.domain.usuario.Rol;
import org.dentalcrm.domain.usuario.Usuario;
import org.dentalcrm.domain.usuario.UsuarioRepository;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.security.JwtService;
import org.dentalcrm.web.auth.dto.LoginRequest;
import org.dentalcrm.web.auth.dto.LoginResponse;
import org.dentalcrm.web.auth.dto.RefreshRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private UsuarioRepository usuarioRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        authService = new AuthService(authenticationManager, jwtService,
                mock(org.springframework.security.core.userdetails.UserDetailsService.class), usuarioRepository);
        ReflectionTestUtils.setField(authService, "expirationMs", 3_600_000L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static Permiso permiso(String codigo) {
        Permiso p = new Permiso();
        p.setCodigo(codigo);
        p.setNombre(codigo);
        return p;
    }

    private static Rol rol(String codigo, String... permisos) {
        Rol r = new Rol();
        r.setCodigo(codigo);
        r.setNombre(codigo);
        for (String p : permisos) {
            r.getPermisos().add(permiso(p));
        }
        return r;
    }

    private static Usuario usuarioCon(Rol... roles) {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setTenantId(1L);
        u.setUsername("admin");
        u.setEmail("admin@clinica.test");
        u.setPassword("$2b$12$hash");
        u.setEstado("ACTIVO");
        u.setRoles(Set.of(roles));
        return u;
    }

    private void autenticarComo(UserDetails userDetails) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
    }

    @Test
    void loginDevuelveRolesYPermisosDeTodosLosRoles() {
        UserDetails principal = new User("admin", "x", List.of());
        autenticarComo(principal);
        Usuario usuario = usuarioCon(
                rol("ADMIN", "CITAS_READ", "PACIENTES_READ"),
                rol("RECEPCION", "PACIENTES_READ", "CAJA_WRITE"));
        when(usuarioRepository.buscarGlobalPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(eq(principal), eq(1L))).thenReturn("tok-1");

        LoginResponse r = authService.login(new LoginRequest("admin", "admin123"));

        assertNotNull(r.token());
        assertEquals("tok-1", r.token());
        assertEquals(3_600_000L, r.expiresInMs());
        assertTrue(r.roles().containsAll(List.of("ADMIN", "RECEPCION")));
        // unión de permisos de ambos roles, ordenada y sin duplicados
        assertEquals(List.of("CAJA_WRITE", "CITAS_READ", "PACIENTES_READ"), r.permisos());
    }

    @Test
    void refreshTambienDevuelveLosPermisos() {
        when(jwtService.extractUsername("tok-viejo")).thenReturn("admin");
        Usuario usuario = usuarioCon(rol("ODONTOLOGO", "ODONTOGRAMA_WRITE", "HISTORIA_CLINICA_READ"));
        when(usuarioRepository.buscarGlobalPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(UserDetails.class), eq(1L))).thenReturn("tok-nuevo");

        LoginResponse r = authService.refresh(new RefreshRequest("tok-viejo"));

        assertEquals("tok-nuevo", r.token());
        assertEquals(List.of("HISTORIA_CLINICA_READ", "ODONTOGRAMA_WRITE"), r.permisos());
    }

    @Test
    void usuarioSinPermisosDevuelveListaVacia() {
        UserDetails principal = new User("sin", "x", List.of());
        autenticarComo(principal);
        Usuario usuario = usuarioCon(rol("INVITADO"));
        when(usuarioRepository.buscarGlobalPorUsername("sin")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(UserDetails.class), eq(1L))).thenReturn("tok-2");

        LoginResponse r = authService.login(new LoginRequest("sin", "x"));

        assertEquals(List.of(), r.permisos());
        assertEquals(List.of("INVITADO"), r.roles());
    }
}

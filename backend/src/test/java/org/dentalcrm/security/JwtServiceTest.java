package org.dentalcrm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("clave-de-prueba-suficientemente-larga-y-segura-para-jwt", 3600000);
    }

    @Test
    void generaYValidaToken() {
        UserDetails user = new User("admin", "password", true, true, true, true, List.of());
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
        assertTrue(jwtService.isValid(token, user));
        assertFalse(jwtService.extractExpiration(token).before(new java.util.Date()));
    }

    @Test
    void tokenIncluyeTenantId() {
        UserDetails user = new User("admin", "password", true, true, true, true, List.of());
        String token = jwtService.generateToken(user, 42L);

        assertEquals(42L, jwtService.extractTenantId(token));
    }

    @Test
    void tenantPorDefectoCuandoNoHayClaim() {
        UserDetails user = new User("admin", "password", true, true, true, true, List.of());
        String token = jwtService.generateToken(user);

        assertEquals(1L, jwtService.extractTenantId(token));
    }

    @Test
    void expiraToken() {
        JwtService shortLived = new JwtService("clave-de-prueba-suficientemente-larga-y-segura-para-jwt", -1000);
        UserDetails user = new User("admin", "password", true, true, true, true, List.of());
        String token = shortLived.generateToken(user);
        assertFalse(shortLived.isValid(token, user));
    }

    @Test
    void rechazaTokenConOtraClave() {
        JwtService other = new JwtService("otra-clave-distinta-suficientemente-larga-para-firmar", 3600000);
        UserDetails user = new User("admin", "password", true, true, true, true, List.of());
        String token = jwtService.generateToken(user);
        assertThrows(Exception.class, () -> other.extractUsername(token));
    }
}
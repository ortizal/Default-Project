package org.dentalcrm.web.configuracion;

import org.dentalcrm.domain.configuracion.IntegracionConfiguracion;
import org.dentalcrm.domain.configuracion.IntegracionConfiguracionRepository;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.IntegracionSecretCipher;
import org.dentalcrm.web.configuracion.dto.CorreoConfigRequest;
import org.dentalcrm.web.configuracion.dto.MetaConfigRequest;
import org.dentalcrm.web.configuracion.dto.OpenWaConfigRequest;
import org.dentalcrm.web.configuracion.dto.TikTokConfigRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegracionesConfiguracionServiceTest {

    private static final String KEY = "integration-test-key-with-at-least-32-characters";

    @Mock
    private IntegracionConfiguracionRepository repository;
    @Mock
    private AuditService auditService;

    private AtomicReference<IntegracionConfiguracion> stored;
    private IntegracionSecretCipher cipher;
    private IntegracionesConfiguracionService service;

    @BeforeEach
    void setUp() {
        stored = new AtomicReference<>();
        cipher = new IntegracionSecretCipher(KEY);
        service = new IntegracionesConfiguracionService(repository, auditService, cipher,
                "http://openwa-env:2785", "env-api-key", "http://backend/webhook", "env-webhook-secret",
                "smtp.env", 587, "mailer", "env-password", "clinic@example.com",
                "meta-client-id", "meta-env-secret", "https://crm.example.com/api/v1/social/callback/meta",
                "tiktok-client-key", "tiktok-env-secret", "https://crm.example.com/api/v1/social/callback/tiktok");
        when(repository.findByTenantId(1L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
    }

    @AfterEach
    void limpiarTenant() {
        org.dentalcrm.multitenant.TenantContext.clear();
    }

    @Test
    void obtenerUsaValoresDeEntornoCuandoNoHayOverride() {
        var response = service.obtener();

        assertEquals("http://openwa-env:2785", response.openWa().url());
        assertTrue(response.openWa().apiKeyConfigurada());
        assertTrue(response.openWa().usaValoresEntorno());
        assertEquals("smtp.env", response.correo().host());
        assertTrue(response.correo().passwordConfigurada());
        assertEquals("meta-client-id", response.meta().clientId());
        assertTrue(response.meta().configurado());
        assertTrue(response.meta().clientSecretConfigurado());
        assertEquals("tiktok-client-key", response.tikTok().clientKey());
        assertTrue(response.tikTok().configurado());
    }

    @Test
    void guardarOpenWaCifraSecretosYLosAplicaSinDevolverlos() {
        configurarGuardado();
        var response = service.actualizarOpenWa(new OpenWaConfigRequest(
                "https://openwa.clinica.ec", "mi-api-secreta", "https://api.clinica.ec/webhook", "mi-webhook-secreto"));

        String storedKey = stored.get().getOpenwaApiKey();
        assertTrue(storedKey.startsWith("enc:v1:"));
        assertEquals("mi-api-secreta", cipher.decrypt(storedKey.substring("enc:v1:".length())));
        assertEquals("https://openwa.clinica.ec", service.openWa().url());
        assertTrue(response.openWa().apiKeyConfigurada());
        assertFalse(response.openWa().usaValoresEntorno());
    }

    @Test
    void guardarCorreoCifraPasswordYRestablecerRecuperaEntorno() {
        configurarGuardado();
        service.actualizarCorreo(new CorreoConfigRequest(
                "smtp.clinica.ec", 465, "cuenta", "password-nuevo", "avisos@clinica.ec"));

        String storedPassword = stored.get().getMailPassword();
        assertTrue(storedPassword.startsWith("enc:v1:"));
        assertEquals("password-nuevo", cipher.decrypt(storedPassword.substring("enc:v1:".length())));
        assertEquals("smtp.clinica.ec", service.correo().host());

        var response = service.restablecerCorreo();
        assertEquals("smtp.env", response.correo().host());
        assertTrue(response.correo().usaValoresEntorno());
    }

    @Test
    void guardarMetaYTikTokCifraSecretosYUsaValoresEnCaliente() {
        configurarGuardado();
        var meta = service.actualizarMeta(new MetaConfigRequest(
                "meta-app", "meta-secret-new", "https://meta.example.com/callback"));
        assertTrue(stored.get().getMetaClientSecret().startsWith("enc:v1:"));
        assertEquals("meta-secret-new", service.redesSociales().metaClientSecret());
        assertTrue(meta.meta().configurado());

        var tikTok = service.actualizarTikTok(new TikTokConfigRequest(
                "tiktok-key", "tiktok-secret-new", "https://tiktok.example.com/callback"));
        assertTrue(stored.get().getTiktokClientSecret().startsWith("enc:v1:"));
        assertEquals("tiktok-secret-new", service.redesSociales().tikTokClientSecret());
        assertTrue(tikTok.tikTok().configurado());

        var restored = service.restablecerMeta();
        assertEquals("meta-client-id", restored.meta().clientId());
        assertTrue(restored.meta().usaValoresEntorno());
    }

    private void configurarGuardado() {
        when(repository.save(any(IntegracionConfiguracion.class))).thenAnswer(invocation -> {
            IntegracionConfiguracion config = invocation.getArgument(0);
            stored.set(config);
            return config;
        });
    }
}
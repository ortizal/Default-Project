package org.dentalcrm.web.google;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.domain.google.GoogleCredential;
import org.dentalcrm.domain.google.GoogleCredentialRepository;
import org.dentalcrm.service.SocialTokenCipher;
import org.dentalcrm.web.google.dto.GoogleCalendarioResponse;
import org.dentalcrm.web.google.dto.GoogleConnectResponse;
import org.dentalcrm.web.google.dto.GoogleCredentialRequest;
import org.dentalcrm.web.google.dto.GoogleCredentialResponse;
import org.dentalcrm.web.google.dto.GoogleStatusResponse;
import org.dentalcrm.web.google.dto.GoogleSyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/google")
@Tag(name = "Google Calendar", description = "Integración OAuth 2.0 y sincronización de citas con Google Calendar")
public class GoogleController {

    private static final Logger log = LoggerFactory.getLogger(GoogleController.class);

    private final GoogleCalendarService googleCalendarService;
    private final GoogleCredentialRepository credentialRepository;
    private final GoogleService googleService;
    private final SocialTokenCipher secretCipher;

    public GoogleController(GoogleCalendarService googleCalendarService,
                             GoogleCredentialRepository credentialRepository,
                             GoogleService googleService,
                             SocialTokenCipher secretCipher) {
        this.googleCalendarService = googleCalendarService;
        this.credentialRepository = credentialRepository;
        this.googleService = googleService;
        this.secretCipher = secretCipher;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Estado de la conexión y sincronización")
    public ResponseEntity<GoogleStatusResponse> status() {
        return ResponseEntity.ok(googleCalendarService.status());
    }

    @GetMapping("/connect")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Iniciar conexión OAuth (devuelve la URL de autorización de Google)")
    public ResponseEntity<GoogleConnectResponse> conectar() {
        return ResponseEntity.ok(new GoogleConnectResponse(googleCalendarService.iniciarConexion()));
    }

    @GetMapping("/callback")
    @Operation(summary = "Callback OAuth de Google (intercambia el código por tokens y guarda la cuenta)")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String error) {
        boolean conectado = false;
        if (error == null || error.isBlank()) {
            try {
                googleCalendarService.completarConexion(code);
                conectado = true;
            } catch (RuntimeException e) {
                log.warn("Google OAuth: no se pudo completar la conexión: {}", e.getMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(googleCalendarService.urlRetorno(conectado)))
                .build();
    }

    @PostMapping("/disconnect")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Desconectar cuenta de Google")
    public ResponseEntity<Void> desconectar() {
        googleCalendarService.desconectar();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/calendars")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Listar calendarios de la cuenta conectada")
    public ResponseEntity<List<GoogleCalendarioResponse>> calendarios() {
        return ResponseEntity.ok(googleCalendarService.listarCalendarios());
    }

    @PostMapping("/calendars/{id}/select")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Seleccionar un calendario")
    public ResponseEntity<Void> seleccionarCalendario(@PathVariable Long id) {
        googleCalendarService.seleccionarCalendario(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Sincronizar citas con Google Calendar")
    public ResponseEntity<GoogleSyncResponse> sincronizar() {
        return ResponseEntity.ok(googleCalendarService.sincronizar());
    }

    @PostMapping("/sync/inbound")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Importar eventos de Google como bloqueos de agenda")
    public ResponseEntity<Void> sincronizarEntrante() {
        googleCalendarService.sincronizarEntrante();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/credentials")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Obtener credenciales OAuth de Google")
    public ResponseEntity<GoogleCredentialResponse> getCredentials() {
        GoogleCredential cred = credentialRepository.findTopByOrderByIdAsc().orElse(null);
        if (cred == null) {
            return ResponseEntity.ok(new GoogleCredentialResponse(null, googleService.clientIdConfigurado(), "",
                googleService.redirectUriConfigurado(), googleService.authBaseUrl(), googleService.oauthBaseUrl(),
                googleService.apiBaseUrl(), googleService.estaConfigurado(), googleService.clientSecretConfigurado()));
        }
        return ResponseEntity.ok(new GoogleCredentialResponse(
                cred.getId(),
            valor(cred.getClientId(), googleService.clientIdConfigurado()),
            "",
            valor(cred.getRedirectUri(), googleService.redirectUriConfigurado()),
            valor(cred.getAuthBaseUrl(), googleService.authBaseUrl()),
            valor(cred.getOauthBaseUrl(), googleService.oauthBaseUrl()),
            valor(cred.getApiBaseUrl(), googleService.apiBaseUrl()),
            googleService.estaConfigurado(), googleService.clientSecretConfigurado()
        ));
    }

    @PutMapping("/credentials")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Actualizar credenciales OAuth de Google")
    public ResponseEntity<GoogleCredentialResponse> updateCredentials(@Valid @RequestBody GoogleCredentialRequest request) {
        GoogleCredential cred = credentialRepository.findTopByOrderByIdAsc().orElseGet(GoogleCredential::new);
        cred.setClientId(request.clientId());
        if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
            cred.setClientSecret("enc:v1:" + secretCipher.encrypt(request.clientSecret().trim()));
        } else if (cred.getClientSecret() != null && !cred.getClientSecret().isBlank()
                && !cred.getClientSecret().startsWith("enc:v1:")) {
            cred.setClientSecret("enc:v1:" + secretCipher.encrypt(cred.getClientSecret()));
        }
        cred.setRedirectUri(request.redirectUri());
        cred.setAuthBaseUrl(request.authBaseUrl());
        cred.setOauthBaseUrl(request.oauthBaseUrl());
        cred.setApiBaseUrl(request.apiBaseUrl());
        cred = credentialRepository.save(cred);
        return ResponseEntity.ok(new GoogleCredentialResponse(
            cred.getId(), valor(cred.getClientId(), googleService.clientIdConfigurado()), "",
            valor(cred.getRedirectUri(), googleService.redirectUriConfigurado()),
            valor(cred.getAuthBaseUrl(), googleService.authBaseUrl()),
            valor(cred.getOauthBaseUrl(), googleService.oauthBaseUrl()),
            valor(cred.getApiBaseUrl(), googleService.apiBaseUrl()), googleService.estaConfigurado(),
            googleService.clientSecretConfigurado()));
        }

        private String valor(String valor, String fallback) {
        return valor == null || valor.isBlank() ? fallback : valor;
    }
}

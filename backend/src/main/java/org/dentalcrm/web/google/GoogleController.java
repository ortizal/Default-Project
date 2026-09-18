package org.dentalcrm.web.google;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.domain.google.GoogleCredential;
import org.dentalcrm.domain.google.GoogleCredentialRepository;
import org.dentalcrm.web.google.dto.GoogleCalendarioResponse;
import org.dentalcrm.web.google.dto.GoogleConnectResponse;
import org.dentalcrm.web.google.dto.GoogleCredentialRequest;
import org.dentalcrm.web.google.dto.GoogleCredentialResponse;
import org.dentalcrm.web.google.dto.GoogleStatusResponse;
import org.dentalcrm.web.google.dto.GoogleSyncResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/google")
@Tag(name = "Google Calendar", description = "Integración OAuth 2.0 y sincronización de citas con Google Calendar")
public class GoogleController {

    private final GoogleCalendarService googleCalendarService;
    private final GoogleCredentialRepository credentialRepository;

    public GoogleController(GoogleCalendarService googleCalendarService,
                             GoogleCredentialRepository credentialRepository) {
        this.googleCalendarService = googleCalendarService;
        this.credentialRepository = credentialRepository;
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

    @GetMapping("/credentials")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Obtener credenciales OAuth de Google")
    public ResponseEntity<GoogleCredentialResponse> getCredentials() {
        GoogleCredential cred = credentialRepository.findTopByOrderByIdAsc().orElse(null);
        if (cred == null) {
            return ResponseEntity.ok(new GoogleCredentialResponse(null, "", "", "", "https://accounts.google.com", "https://oauth2.googleapis.com", "https://www.googleapis.com", false));
        }
        boolean configurada = cred.getClientId() != null && !cred.getClientId().isBlank()
                && cred.getClientSecret() != null && !cred.getClientSecret().isBlank();
        return ResponseEntity.ok(new GoogleCredentialResponse(
                cred.getId(),
                cred.getClientId(),
                cred.getClientSecret(),
                cred.getRedirectUri(),
                cred.getAuthBaseUrl(),
                cred.getOauthBaseUrl(),
                cred.getApiBaseUrl(),
                configurada
        ));
    }

    @PutMapping("/credentials")
    @PreAuthorize("hasAuthority('PERMISO_INTEGRACIONES')")
    @Operation(summary = "Actualizar credenciales OAuth de Google")
    public ResponseEntity<GoogleCredentialResponse> updateCredentials(@Valid @RequestBody GoogleCredentialRequest request) {
        GoogleCredential cred = credentialRepository.findTopByOrderByIdAsc().orElseGet(GoogleCredential::new);
        cred.setClientId(request.clientId());
        cred.setClientSecret(request.clientSecret());
        cred.setRedirectUri(request.redirectUri());
        cred.setAuthBaseUrl(request.authBaseUrl());
        cred.setOauthBaseUrl(request.oauthBaseUrl());
        cred.setApiBaseUrl(request.apiBaseUrl());
        cred = credentialRepository.save(cred);
        boolean configurada = cred.getClientId() != null && !cred.getClientId().isBlank()
                && cred.getClientSecret() != null && !cred.getClientSecret().isBlank();
        return ResponseEntity.ok(new GoogleCredentialResponse(
                cred.getId(), cred.getClientId(), cred.getClientSecret(), cred.getRedirectUri(),
                cred.getAuthBaseUrl(), cred.getOauthBaseUrl(), cred.getApiBaseUrl(), configurada));
    }
}

package org.dentalcrm.web.social;

import org.dentalcrm.domain.social.SocialProvider;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.web.social.dto.SocialConnectResponse;
import org.dentalcrm.web.social.dto.SocialPublishResponse;
import org.dentalcrm.web.social.dto.SocialStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialService socialService;
    private final SocialPublishService socialPublishService;

    public SocialController(SocialService socialService, SocialPublishService socialPublishService) {
        this.socialService = socialService;
        this.socialPublishService = socialPublishService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERMISO_REDES_SOCIALES')")
    public ResponseEntity<SocialStatusResponse> status() {
        return ResponseEntity.ok(socialService.status());
    }

    @PostMapping(value = "/publish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERMISO_REDES_SOCIALES')")
    public ResponseEntity<SocialPublishResponse> publicar(@RequestParam("cuentaId") Long cuentaId,
                                                          @RequestParam("texto") String texto,
                                                          @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(socialPublishService.publicar(cuentaId, texto, imagen));
    }

    @GetMapping("/{provider}/connect")
    @PreAuthorize("hasAuthority('PERMISO_REDES_SOCIALES')")
    public ResponseEntity<SocialConnectResponse> conectar(@PathVariable String provider) {
        return ResponseEntity.ok(new SocialConnectResponse(socialService.iniciarConexion(parseProvider(provider))));
    }

    @DeleteMapping("/{provider}/disconnect")
    @PreAuthorize("hasAuthority('PERMISO_REDES_SOCIALES')")
    public ResponseEntity<Void> desconectar(@PathVariable String provider) {
        socialService.desconectar(parseProvider(provider));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        boolean connected = false;
        try {
            if (error == null || error.isBlank()) {
                SocialProvider parsedProvider = parseProvider(provider);
                Long previousTenant = TenantContext.tenantId();
                TenantContext.set(socialService.tenantDeSolicitud(parsedProvider, state));
                try {
                    socialService.completarConexion(parsedProvider, code, state);
                    connected = true;
                } finally {
                    if (previousTenant == null) TenantContext.clear();
                    else TenantContext.set(previousTenant);
                }
            }
        } catch (RuntimeException ignored) {
            connected = false;
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(socialService.urlRetorno(connected)))
                .build();
    }

    private SocialProvider parseProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "meta" -> SocialProvider.META;
            case "tiktok" -> SocialProvider.TIKTOK;
            default -> throw new IllegalArgumentException("Proveedor social no válido");
        };
    }
}
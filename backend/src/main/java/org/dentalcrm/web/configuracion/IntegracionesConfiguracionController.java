package org.dentalcrm.web.configuracion;

import jakarta.validation.Valid;
import org.dentalcrm.web.configuracion.dto.CorreoConfigRequest;
import org.dentalcrm.web.configuracion.dto.IntegracionesConfigResponse;
import org.dentalcrm.web.configuracion.dto.MetaConfigRequest;
import org.dentalcrm.web.configuracion.dto.OpenWaConfigRequest;
import org.dentalcrm.web.configuracion.dto.TikTokConfigRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion/integraciones")
@PreAuthorize("hasAuthority('PERMISO_CONFIGURACION')")
public class IntegracionesConfiguracionController {

    private final IntegracionesConfiguracionService service;

    public IntegracionesConfiguracionController(IntegracionesConfiguracionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<IntegracionesConfigResponse> obtener() {
        return ResponseEntity.ok(service.obtener());
    }

    @PutMapping("/openwa")
    public ResponseEntity<IntegracionesConfigResponse> actualizarOpenWa(@Valid @RequestBody OpenWaConfigRequest request) {
        return ResponseEntity.ok(service.actualizarOpenWa(request));
    }

    @DeleteMapping("/openwa")
    public ResponseEntity<IntegracionesConfigResponse> restablecerOpenWa() {
        return ResponseEntity.ok(service.restablecerOpenWa());
    }

    @PutMapping("/correo")
    public ResponseEntity<IntegracionesConfigResponse> actualizarCorreo(@Valid @RequestBody CorreoConfigRequest request) {
        return ResponseEntity.ok(service.actualizarCorreo(request));
    }

    @DeleteMapping("/correo")
    public ResponseEntity<IntegracionesConfigResponse> restablecerCorreo() {
        return ResponseEntity.ok(service.restablecerCorreo());
    }

    @PutMapping("/meta")
    public ResponseEntity<IntegracionesConfigResponse> actualizarMeta(@Valid @RequestBody MetaConfigRequest request) {
        return ResponseEntity.ok(service.actualizarMeta(request));
    }

    @DeleteMapping("/meta")
    public ResponseEntity<IntegracionesConfigResponse> restablecerMeta() {
        return ResponseEntity.ok(service.restablecerMeta());
    }

    @PutMapping("/tiktok")
    public ResponseEntity<IntegracionesConfigResponse> actualizarTikTok(@Valid @RequestBody TikTokConfigRequest request) {
        return ResponseEntity.ok(service.actualizarTikTok(request));
    }

    @DeleteMapping("/tiktok")
    public ResponseEntity<IntegracionesConfigResponse> restablecerTikTok() {
        return ResponseEntity.ok(service.restablecerTikTok());
    }
}
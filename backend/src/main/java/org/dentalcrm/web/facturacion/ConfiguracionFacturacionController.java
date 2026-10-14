package org.dentalcrm.web.facturacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.facturacion.dto.ConfiguracionFacturacionRequest;
import org.dentalcrm.web.facturacion.dto.ConfiguracionFacturacionResponse;
import org.dentalcrm.web.facturacion.dto.FirmaPasswordRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Configuración tributaria y credenciales SRI — Fase G (plan §3.5/§3.16).
 * Requiere {@code CONFIGURACION_FACTURACION} (SÓLO SUPER_ADMIN/ADMIN, V27):
 * la recepción consulta comprobantes pero no toca la configuración.
 */
@RestController
@RequestMapping("/api/v1/facturacion/configuracion")
@Tag(name = "Facturación — configuración SRI",
        description = "Ambiente, secuenciales, credenciales y firma digital "
                + "(Fase G, plan §3.5)")
@PreAuthorize("hasAuthority('PERMISO_CONFIGURACION_FACTURACION')")
public class ConfiguracionFacturacionController {

    private final ConfiguracionFacturacionService servicio;

    public ConfiguracionFacturacionController(ConfiguracionFacturacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Leer la configuración tributaria (credenciales sólo como \"configuradas\")")
    public ResponseEntity<ConfiguracionFacturacionResponse> obtener() {
        return ResponseEntity.ok(servicio.obtener());
    }

    @PutMapping
    @Operation(summary = "Actualizar ambiente, secuenciales, banderas y credenciales del SRI")
    public ResponseEntity<ConfiguracionFacturacionResponse> actualizar(
            @Valid @RequestBody ConfiguracionFacturacionRequest request) {
        return ResponseEntity.ok(servicio.actualizar(request));
    }

    @PutMapping("/firma")
    @Operation(summary = "Definir la contraseña del certificado .p12 (se guarda cifrada)")
    public ResponseEntity<ConfiguracionFacturacionResponse> definirPasswordFirma(
            @Valid @RequestBody FirmaPasswordRequest request) {
        return ResponseEntity.ok(servicio.definirPasswordFirma(request));
    }

    @DeleteMapping("/firma")
    @Operation(summary = "Olvidar la contraseña del .p12 y volver a firma simulada")
    public ResponseEntity<ConfiguracionFacturacionResponse> limpiarPasswordFirma() {
        return ResponseEntity.ok(servicio.limpiarPasswordFirma());
    }
}

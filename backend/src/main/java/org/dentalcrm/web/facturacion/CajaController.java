package org.dentalcrm.web.facturacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.facturacion.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Caja — Fase F (plan §3.4): apertura, ingresos y egresos por forma de
 * pago, cierre con monto declarado y conciliación. Abrir y mover dinero
 * exige permiso de escritura; cerrar exige además {@code CAJA_CERRAR}.
 */
@RestController
@RequestMapping("/api/v1/facturacion/cajas")
@Tag(name = "Facturación — caja",
        description = "Sesiones de caja: apertura, movimientos por forma de pago, "
                + "cierre y conciliación (Fase F, plan §3.4)")
public class CajaController {

    private final CajaService servicio;

    public CajaController(CajaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/actual")
    @PreAuthorize("hasAuthority('PERMISO_CAJA_READ')")
    @Operation(summary = "Sesión de caja abierta con su corte y movimientos")
    public ResponseEntity<CajaResponse> actual() {
        return ResponseEntity.ok(servicio.actual());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_CAJA_WRITE')")
    @Operation(summary = "Abrir una sesión de caja (sólo puede haber una abierta)")
    public ResponseEntity<CajaResponse> abrir(@Valid @RequestBody AbrirCajaRequest request) {
        return ResponseEntity.ok(servicio.abrir(request));
    }

    @PutMapping("/actual/cerrar")
    @PreAuthorize("hasAuthority('PERMISO_CAJA_CERRAR')")
    @Operation(summary = "Cerrar la sesión abierta declarando el efectivo contado")
    public ResponseEntity<CajaResponse> cerrar(
            @Valid @RequestBody CerrarCajaRequest request) {
        return ResponseEntity.ok(servicio.cerrar(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_CAJA_READ')")
    @Operation(summary = "Historial de sesiones de caja por rango")
    public ResponseEntity<List<CajaResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.listar(desde, hasta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_CAJA_READ')")
    @Operation(summary = "Detalle de una sesión de caja")
    public ResponseEntity<CajaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('PERMISO_CAJA_WRITE')")
    @Operation(summary = "Registrar un ingreso o egreso manual en la sesión")
    public ResponseEntity<CajaResponse> registrarMovimiento(
            @PathVariable Long id,
            @Valid @RequestBody NuevoMovimientoCajaRequest request) {
        return ResponseEntity.ok(servicio.registrarMovimiento(id, request));
    }

    @PostMapping("/{id}/movimientos/{movimientoId}/anular")
    @PreAuthorize("hasAuthority('PERMISO_CAJA_WRITE')")
    @Operation(summary = "Anular un movimiento de la sesión (queda con su motivo)")
    public ResponseEntity<CajaResponse> anularMovimiento(
            @PathVariable Long id,
            @PathVariable Long movimientoId,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(servicio.anularMovimiento(id, movimientoId, motivo));
    }
}

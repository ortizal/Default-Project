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
 * Cuenta del paciente — Fase F (plan §3.3): capa financiera previa al
 * comprobante con cargos, descuentos, anticipos, abonos y devoluciones.
 * Todo lo que toca dinero exige permiso de escritura; la consulta del
 * saldo es de sólo lectura.
 */
@RestController
@RequestMapping("/api/v1/facturacion")
@Tag(name = "Facturación — cuenta del paciente",
        description = "Cargos, descuentos, anticipos, abonos y devoluciones previos "
                + "al comprobante electrónico (Fase F, plan §3.3)")
public class CuentaPacienteController {

    private final CuentaPacienteService servicio;

    public CuentaPacienteController(CuentaPacienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cuentas")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Listar cuentas con su saldo (filtro por estado y texto libre)")
    public ResponseEntity<List<CuentaResumenResponse>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String buscar) {
        return ResponseEntity.ok(servicio.listar(estado, buscar));
    }

    @GetMapping("/cuentas/{pacienteId}")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Cuenta del paciente con todos sus movimientos")
    public ResponseEntity<CuentaPacienteResponse> cuenta(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.cuenta(pacienteId));
    }

    @PostMapping("/cuentas/{pacienteId}/movimientos")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_WRITE')")
    @Operation(summary = "Agregar un cargo, descuento, anticipo, abono o devolución")
    public ResponseEntity<CuentaPacienteResponse> agregarMovimiento(
            @PathVariable Long pacienteId,
            @Valid @RequestBody NuevoMovimientoCuentaRequest request) {
        return ResponseEntity.ok(servicio.agregarMovimiento(pacienteId, request));
    }

    @PostMapping("/cuentas/{pacienteId}/movimientos/{movimientoId}/anular")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_WRITE')")
    @Operation(summary = "Anular un movimiento (queda en el libro con su motivo)")
    public ResponseEntity<CuentaPacienteResponse> anularMovimiento(
            @PathVariable Long pacienteId,
            @PathVariable Long movimientoId,
            @RequestBody(required = false) AnularMovimientoRequest request) {
        return ResponseEntity.ok(servicio.anularMovimiento(pacienteId, movimientoId, request));
    }

    @PutMapping("/cuentas/{pacienteId}/estado")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_WRITE')")
    @Operation(summary = "Cerrar, anular o reabrir la cuenta del paciente")
    public ResponseEntity<CuentaPacienteResponse> cambiarEstado(
            @PathVariable Long pacienteId,
            @Valid @RequestBody EstadoCuentaRequest request) {
        return ResponseEntity.ok(servicio.cambiarEstado(pacienteId, request));
    }

    @GetMapping("/pagos")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Abonos, anticipos y devoluciones por rango (por defecto: último año)")
    public ResponseEntity<List<MovimientoCuentaResponse>> pagos(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String formaPago) {
        return ResponseEntity.ok(servicio.pagos(pacienteId, desde, hasta, formaPago));
    }
}

package org.dentalcrm.web.facturacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.facturacion.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Dashboard y reportes financieros — Fase F (plan §3.12 y §3.13). Todo
 * es de sólo lectura y requiere {@code REPORTES_FINANCIEROS}; los datos
 * del SRI (comprobantes, impuestos, secuenciales) llegan en la Fase G.
 */
@RestController
@RequestMapping("/api/v1/facturacion")
@Tag(name = "Facturación — reportes",
        description = "Dashboard financiero, ventas, producción, saldos y caja "
                + "(Fase F, plan §3.12-§3.13)")
public class ReporteFinancieroController {

    private final ReporteFinancieroService servicio;

    public ReporteFinancieroController(ReporteFinancieroService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Dashboard financiero: facturado, cobrado, por cobrar y anticipos")
    public ResponseEntity<DashboardFinancieroResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.dashboard(desde, hasta));
    }

    @GetMapping("/reportes/ventas")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES_FINANCIEROS')")
    @Operation(summary = "Ventas por rango: día a día, forma de pago y servicios")
    public ResponseEntity<ReporteVentasResponse> ventas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.ventas(desde, hasta));
    }

    @GetMapping("/reportes/produccion")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES_FINANCIEROS')")
    @Operation(summary = "Producción facturada por odontólogo")
    public ResponseEntity<ReporteProduccionResponse> produccion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.produccion(desde, hasta));
    }

    @GetMapping("/reportes/saldos")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES_FINANCIEROS')")
    @Operation(summary = "Cuentas por cobrar con el detalle de cada saldo")
    public ResponseEntity<ReporteSaldosResponse> saldos() {
        return ResponseEntity.ok(servicio.saldos());
    }

    @GetMapping("/reportes/caja")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES_FINANCIEROS')")
    @Operation(summary = "Corte de caja por rango de sesiones")
    public ResponseEntity<ReporteCajaResponse> reporteCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.reporteCaja(desde, hasta));
    }
}

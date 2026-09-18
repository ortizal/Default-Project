package org.dentalcrm.web.reporte;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.reporte.dto.DashboardResponse;
import org.dentalcrm.web.reporte.dto.EstadisticasResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes", description = "Dashboard y estadísticas del consultorio")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES')")
    @Operation(summary = "Dashboard: conteos del día y del mes")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(reporteService.dashboard());
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES')")
    @Operation(summary = "Estadísticas por rango de fechas")
    public ResponseEntity<EstadisticasResponse> estadisticas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(reporteService.estadisticas(desde, hasta, limite));
    }
}
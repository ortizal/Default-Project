package org.dentalcrm.web.historia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse;
import org.dentalcrm.web.historia.dto.ReporteClinicoResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Reportes clínicos (plan §4.17): resumen global del consultorio
 * (diagnósticos frecuentes, tratamientos pendientes, continuidad y
 * producción clínica) y descarga de la historia completa en PDF.
 */
@RestController
@RequestMapping("/api/v1/historia-clinica")
@Tag(name = "Reportes clínicos", description = "Reportes globales y PDF de historia completa")
public class ReporteClinicoController {

    private final ReporteClinicoService reporteService;
    private final HistoriaCompletaService historiaService;
    private final HistoriaCompletaPdfService pdfService;

    public ReporteClinicoController(ReporteClinicoService reporteService,
                                    HistoriaCompletaService historiaService,
                                    HistoriaCompletaPdfService pdfService) {
        this.reporteService = reporteService;
        this.historiaService = historiaService;
        this.pdfService = pdfService;
    }

    @GetMapping("/reportes")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES_CLINICOS')")
    @Operation(summary = "Reportes clínicos globales por rango de fechas")
    public ResponseEntity<ReporteClinicoResponse> reportes(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "10") int limite) {
        return ResponseEntity.ok(reporteService.reporte(desde, hasta, limite));
    }

    @GetMapping("/pacientes/{pacienteId}/reportes/historia-completa.pdf")
    @PreAuthorize("hasAuthority('PERMISO_REPORTES_CLINICOS')")
    @Operation(summary = "Descargar la historia clínica completa del paciente en PDF")
    public ResponseEntity<byte[]> historiaCompleta(@PathVariable Long pacienteId) {
        HistoriaCompletaResponse datos = historiaService.construir(pacienteId);
        byte[] contenido = pdfService.generar(datos);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + pdfService.nombreArchivo(pacienteId) + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}

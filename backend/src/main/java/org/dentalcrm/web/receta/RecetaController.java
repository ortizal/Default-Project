package org.dentalcrm.web.receta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.receta.dto.CambioEstadoRecetaRequest;
import org.dentalcrm.web.receta.dto.RecetaRequest;
import org.dentalcrm.web.receta.dto.RecetaResumenResponse;
import org.dentalcrm.web.receta.dto.RecetaResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Recetas — Fase E (plan §4.12): historial por paciente, alta con pauta
 * completa por medicamento, edición, anulación motivada y PDF.
 */
@RestController
@RequestMapping("/api/v1/historia-clinica")
@Tag(name = "Recetas", description = "Recetas médicas por paciente con medicamentos, pauta, "
        + "historial, anulación y reporte PDF")
public class RecetaController {

    private final RecetaService servicio;

    public RecetaController(RecetaService servicio) {
        this.servicio = servicio;
    }

    // ---------------------------- Consultas ---------------------------

    @GetMapping("/pacientes/{pacienteId}/recetas")
    @PreAuthorize("hasAuthority('PERMISO_RECETAS_READ')")
    @Operation(summary = "Historial de recetas del paciente (más recientes primero)")
    public ResponseEntity<List<RecetaResumenResponse>> historial(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.historial(pacienteId));
    }

    @GetMapping("/recetas/{id}")
    @PreAuthorize("hasAuthority('PERMISO_RECETAS_READ')")
    @Operation(summary = "Detalle de una receta con sus líneas")
    public ResponseEntity<RecetaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    // ----------------------------- Alta y edición ---------------------

    @PostMapping("/pacientes/{pacienteId}/recetas")
    @PreAuthorize("hasAuthority('PERMISO_RECETAS_WRITE')")
    @Operation(summary = "Crear una receta con sus medicamentos")
    public ResponseEntity<RecetaResponse> crear(
            @PathVariable Long pacienteId,
            @Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(pacienteId, request));
    }

    @PutMapping("/recetas/{id}")
    @PreAuthorize("hasAuthority('PERMISO_RECETAS_WRITE')")
    @Operation(summary = "Editar cabecera y reemplazar las líneas de la receta")
    public ResponseEntity<RecetaResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.ok(servicio.editar(id, request));
    }

    @PutMapping("/recetas/{id}/estado")
    @PreAuthorize("hasAuthority('PERMISO_RECETAS_WRITE')")
    @Operation(summary = "Anular una receta indicando el motivo")
    public ResponseEntity<RecetaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRecetaRequest request) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, request));
    }

    // -------------------------------- PDF --------------------------------

    @GetMapping("/recetas/{id}/pdf")
    @PreAuthorize("hasAuthority('PERMISO_RECETAS_READ')")
    @Operation(summary = "Descargar la receta en PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] contenido = servicio.pdf(id);
        String nombre = servicio.nombreArchivoPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}

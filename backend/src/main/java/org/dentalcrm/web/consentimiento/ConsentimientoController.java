package org.dentalcrm.web.consentimiento;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.consentimiento.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consentimientos informados — Fase E (plan §4.13): plantillas por
 * categoría, emisión con snapshot del texto, edición sólo en borrador,
 * firma con hash y PDF.
 */
@RestController
@RequestMapping("/api/v1/historia-clinica")
@Tag(name = "Consentimientos", description = "Consentimientos informados: plantillas, emisión, "
        + "firma con hash SHA-256 y PDF inmutable")
public class ConsentimientoController {

    private final ConsentimientoService servicio;

    public ConsentimientoController(ConsentimientoService servicio) {
        this.servicio = servicio;
    }

    // ---------------------------- Plantillas ---------------------------

    @GetMapping("/plantillas-consentimiento")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_READ')")
    @Operation(summary = "Catálogo de plantillas de consentimiento")
    public ResponseEntity<List<PlantillaConsentimientoResponse>> plantillas() {
        return ResponseEntity.ok(servicio.plantillas());
    }

    @PutMapping("/plantillas-consentimiento/{codigo}")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_WRITE')")
    @Operation(summary = "Editar el texto de una plantilla (sube su versión)")
    public ResponseEntity<PlantillaConsentimientoResponse> actualizarPlantilla(
            @PathVariable String codigo,
            @Valid @RequestBody ActualizarPlantillaConsentimientoRequest request) {
        return ResponseEntity.ok(servicio.actualizarPlantilla(codigo, request));
    }

    // ---------------------------- Consultas ---------------------------

    @GetMapping("/pacientes/{pacienteId}/consentimientos")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_READ')")
    @Operation(summary = "Historial de consentimientos del paciente")
    public ResponseEntity<List<ConsentimientoResumenResponse>> historial(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.historial(pacienteId));
    }

    @GetMapping("/consentimientos/{id}")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_READ')")
    @Operation(summary = "Detalle de un consentimiento con su texto y firma")
    public ResponseEntity<ConsentimientoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    // --------------------------- Emitir/editar --------------------------

    @PostMapping("/pacientes/{pacienteId}/consentimientos")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_WRITE')")
    @Operation(summary = "Emitir un consentimiento en BORRADOR desde una plantilla")
    public ResponseEntity<ConsentimientoResponse> crear(
            @PathVariable Long pacienteId,
            @Valid @RequestBody NuevoConsentimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(pacienteId, request));
    }

    @PutMapping("/consentimientos/{id}")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_WRITE')")
    @Operation(summary = "Editar un consentimiento mientras esté en BORRADOR")
    public ResponseEntity<ConsentimientoResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EditarConsentimientoRequest request) {
        return ResponseEntity.ok(servicio.editar(id, request));
    }

    @DeleteMapping("/consentimientos/{id}")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_WRITE')")
    @Operation(summary = "Eliminar un consentimiento en BORRADOR")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/consentimientos/{id}/firmar")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_WRITE')")
    @Operation(summary = "Firmar el consentimiento: lo congela y calcula la huella SHA-256")
    public ResponseEntity<ConsentimientoResponse> firmar(
            @PathVariable Long id,
            @Valid @RequestBody FirmarConsentimientoRequest request) {
        return ResponseEntity.ok(servicio.firmar(id, request));
    }

    // -------------------------------- PDF --------------------------------

    @GetMapping("/consentimientos/{id}/pdf")
    @PreAuthorize("hasAuthority('PERMISO_CONSENTIMIENTOS_READ')")
    @Operation(summary = "Descargar el consentimiento en PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] contenido = servicio.pdf(id);
        String nombre = servicio.nombreArchivoPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}

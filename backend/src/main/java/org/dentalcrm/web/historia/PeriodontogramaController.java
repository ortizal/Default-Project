package org.dentalcrm.web.historia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.historia.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Periodontograma — Fase E (plan §4.11). Datos estructurados por examen
 * (versión) con mediciones por diente y sitio, comparación y PDF.
 */
@RestController
@RequestMapping("/api/v1/historia-clinica")
@Tag(name = "Periodontograma", description = "Exámenes periodontales por paciente: mediciones por "
        + "diente y sitio, movilidad, furcación, comparación y reporte")
public class PeriodontogramaController {

    private final PeriodontogramaService servicio;

    public PeriodontogramaController(PeriodontogramaService servicio) {
        this.servicio = servicio;
    }

    // ---------------------------- Consultas ---------------------------

    @GetMapping("/pacientes/{pacienteId}/periodontograma")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_READ')")
    @Operation(summary = "Examen periodontal vigente (la versión más reciente); null si no existe")
    public ResponseEntity<ExamenPeriodontalResponse> vigente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.vigente(pacienteId));
    }

    @GetMapping("/pacientes/{pacienteId}/periodontogramas")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_READ')")
    @Operation(summary = "Historial de exámenes periodontales")
    public ResponseEntity<List<ExamenPeriodontalResumenResponse>> historial(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.historial(pacienteId));
    }

    @GetMapping("/periodontogramas/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_READ')")
    @Operation(summary = "Detalle de un examen concreto")
    public ResponseEntity<ExamenPeriodontalResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @GetMapping("/pacientes/{pacienteId}/periodontograma/comparar")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_READ')")
    @Operation(summary = "Comparar dos exámenes por versión (nuevos, modificados y eliminados)")
    public ResponseEntity<ComparacionPeriodontalResponse> comparar(@PathVariable Long pacienteId,
                                                                   @RequestParam int desde,
                                                                   @RequestParam int hasta) {
        return ResponseEntity.ok(servicio.comparar(pacienteId, desde, hasta));
    }

    // ---------------------------- Versiones ---------------------------

    @PostMapping("/pacientes/{pacienteId}/periodontogramas")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_WRITE')")
    @Operation(summary = "Crear la siguiente versión (por defecto copia la anterior)")
    public ResponseEntity<ExamenPeriodontalResponse> crearVersion(
            @PathVariable Long pacienteId,
            @RequestBody NuevaVersionPeriodontalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crearVersion(pacienteId, request));
    }

    @PutMapping("/periodontogramas/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_WRITE')")
    @Operation(summary = "Editar fecha y observaciones del examen")
    public ResponseEntity<ExamenPeriodontalResponse> editarCabecera(
            @PathVariable Long id,
            @RequestBody EditarExamenPeriodontalRequest request) {
        return ResponseEntity.ok(servicio.editarCabecera(id, request));
    }

    // ---------------------------- Mediciones --------------------------

    @PutMapping("/periodontogramas/{id}/mediciones")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_WRITE')")
    @Operation(summary = "Guardar mediciones por sitio y movilidad/furcación por pieza (upsert)")
    public ResponseEntity<ExamenPeriodontalResponse> guardarMediciones(
            @PathVariable Long id,
            @Valid @RequestBody MedicionesPeriodontalRequest request) {
        return ResponseEntity.ok(servicio.guardarMediciones(id, request));
    }

    @DeleteMapping("/periodontogramas/{id}/dientes/{diente}")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_WRITE')")
    @Operation(summary = "Eliminar todas las mediciones de una pieza")
    public ResponseEntity<Void> eliminarPieza(@PathVariable Long id, @PathVariable int diente) {
        servicio.eliminarPieza(id, diente);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------- PDF --------------------------------

    @GetMapping("/periodontogramas/{id}/pdf")
    @PreAuthorize("hasAuthority('PERMISO_PERIODONCIA_READ')")
    @Operation(summary = "Descargar el examen periodontal en PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] contenido = servicio.pdf(id);
        String nombre = servicio.nombreArchivoPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}

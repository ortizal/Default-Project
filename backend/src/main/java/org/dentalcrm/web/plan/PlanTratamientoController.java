package org.dentalcrm.web.plan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.plan.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes-tratamiento")
@Tag(name = "Planes de tratamiento",
        description = "Plan clínico con líneas, presupuestos versionados y aceptación firmada")
public class PlanTratamientoController {

    private final PlanTratamientoService servicio;
    private final PlanPdfService pdfService;

    public PlanTratamientoController(PlanTratamientoService servicio, PlanPdfService pdfService) {
        this.servicio = servicio;
        this.pdfService = pdfService;
    }

    // ------------------------------- Plan -------------------------------

    @GetMapping("/pacientes/{pacienteId}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_READ')")
    @Operation(summary = "Planes de un paciente")
    public ResponseEntity<List<PlanResumenResponse>> listar(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.listar(pacienteId));
    }

    @PostMapping("/pacientes/{pacienteId}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Crear un plan en borrador")
    public ResponseEntity<PlanResponse> crear(@PathVariable Long pacienteId,
                                              @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(pacienteId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_READ')")
    @Operation(summary = "Detalle del plan con líneas y presupuestos")
    public ResponseEntity<PlanResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Editar cabecera del plan (borrador, presentado o rechazado)")
    public ResponseEntity<PlanResponse> editar(@PathVariable Long id,
                                               @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(servicio.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Eliminar un plan en borrador")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Cambiar el estado del plan (matriz de transiciones)")
    public ResponseEntity<PlanResponse> cambiarEstado(@PathVariable Long id,
                                                      @Valid @RequestBody EstadoPlanRequest request) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, request));
    }

    // ------------------------------ Líneas ------------------------------

    @PostMapping("/{id}/lineas")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Agregar una línea al plan (sólo borrador)")
    public ResponseEntity<LineaResponse> agregarLinea(@PathVariable Long id,
                                                     @Valid @RequestBody LineaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.agregarLinea(id, request));
    }

    @PutMapping("/{id}/lineas/{lineaId}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Editar una línea del plan (sólo borrador)")
    public ResponseEntity<LineaResponse> editarLinea(@PathVariable Long id,
                                                     @PathVariable Long lineaId,
                                                     @Valid @RequestBody LineaRequest request) {
        return ResponseEntity.ok(servicio.editarLinea(id, lineaId, request));
    }

    @DeleteMapping("/{id}/lineas/{lineaId}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Eliminar una línea del plan (sólo borrador)")
    public ResponseEntity<Void> eliminarLinea(@PathVariable Long id, @PathVariable Long lineaId) {
        servicio.eliminarLinea(id, lineaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/lineas/{lineaId}/estado")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Avanzar el estado de una línea (plan en ejecución)")
    public ResponseEntity<LineaResponse> cambiarEstadoLinea(@PathVariable Long id,
                                                            @PathVariable Long lineaId,
                                                            @Valid @RequestBody EstadoLineaRequest request) {
        return ResponseEntity.ok(servicio.cambiarEstadoLinea(id, lineaId, request));
    }

    // --------------------------- Presupuestos ---------------------------

    @GetMapping("/{id}/presupuestos")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_READ')")
    @Operation(summary = "Historial de presupuestos del plan")
    public ResponseEntity<List<PresupuestoResumenResponse>> presupuestos(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.presupuestos(id));
    }

    @PostMapping("/{id}/presupuestos")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Emitir un presupuesto nuevo (snapshot de las líneas)")
    public ResponseEntity<PresupuestoResponse> emitirPresupuesto(@PathVariable Long id,
                                                                 @RequestBody PresupuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.emitirPresupuesto(id, request));
    }

    @GetMapping("/{id}/presupuestos/{presupuestoId}")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_READ')")
    @Operation(summary = "Detalle de un presupuesto con sus líneas congeladas")
    public ResponseEntity<PresupuestoResponse> obtenerPresupuesto(@PathVariable Long id,
                                                                  @PathVariable Long presupuestoId) {
        return ResponseEntity.ok(servicio.obtenerPresupuesto(id, presupuestoId));
    }

    @PutMapping("/{id}/presupuestos/{presupuestoId}/estado")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_WRITE')")
    @Operation(summary = "Presentar, aceptar (con firmante) o rechazar un presupuesto")
    public ResponseEntity<PresupuestoResponse> cambiarEstadoPresupuesto(
            @PathVariable Long id,
            @PathVariable Long presupuestoId,
            @Valid @RequestBody AceptacionRequest request) {
        return ResponseEntity.ok(servicio.cambiarEstadoPresupuesto(id, presupuestoId, request));
    }

    // -------------------------------- PDF --------------------------------

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('PERMISO_PLAN_TRATAMIENTO_READ')")
    @Operation(summary = "Descargar el plan en PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] contenido = servicio.pdf(id);
        String nombre = servicio.nombreArchivoPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}

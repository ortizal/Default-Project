package org.dentalcrm.web.historia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.historia.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/historia-clinica")
@Tag(name = "Historia clínica", description = "Antecedentes, exámenes, diagnósticos, evoluciones y timeline")
public class HistoriaClinicaController {

    private final HistoriaClinicaService servicio;
    private final TimelineService timelineService;

    public HistoriaClinicaController(HistoriaClinicaService servicio, TimelineService timelineService) {
        this.servicio = servicio;
        this.timelineService = timelineService;
    }

    @GetMapping("/pacientes/{pacienteId}")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_READ')")
    @Operation(summary = "Resumen clínico del paciente")
    public ResponseEntity<ResumenResponse> resumen(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.resumen(pacienteId));
    }

    @GetMapping("/pacientes/{pacienteId}/timeline")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_READ')")
    @Operation(summary = "Línea temporal unificada del paciente (consultas, odontogramas, diagnósticos, "
            + "planes, procedimientos, evoluciones, recetas, archivos, consentimientos y controles)")
    public ResponseEntity<List<TimelineEventoResponse>> timeline(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(timelineService.timeline(pacienteId));
    }

    // ---------------------------- Antecedentes ------------------------

    @GetMapping("/pacientes/{pacienteId}/antecedentes")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_READ')")
    @Operation(summary = "Listar antecedentes estructurados")
    public ResponseEntity<List<AntecedenteResponse>> listarAntecedentes(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.listarAntecedentes(pacienteId));
    }

    @PostMapping("/pacientes/{pacienteId}/antecedentes")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Registrar un antecedente")
    public ResponseEntity<AntecedenteResponse> crearAntecedente(@PathVariable Long pacienteId,
                                                                @Valid @RequestBody AntecedenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearAntecedente(pacienteId, request));
    }

    @PutMapping("/antecedentes/{id}")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Editar un antecedente")
    public ResponseEntity<AntecedenteResponse> actualizarAntecedente(@PathVariable Long id,
                                                                     @Valid @RequestBody AntecedenteRequest request) {
        return ResponseEntity.ok(servicio.actualizarAntecedente(id, request));
    }

    @DeleteMapping("/antecedentes/{id}")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Eliminar un antecedente")
    public ResponseEntity<Void> eliminarAntecedente(@PathVariable Long id) {
        servicio.eliminarAntecedente(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------- Exámenes ---------------------------

    @GetMapping("/pacientes/{pacienteId}/examenes")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_READ')")
    @Operation(summary = "Listar exámenes clínicos")
    public ResponseEntity<List<ExamenResponse>> listarExamenes(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.listarExamenes(pacienteId));
    }

    @PostMapping("/pacientes/{pacienteId}/examenes")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Registrar un examen clínico")
    public ResponseEntity<ExamenResponse> crearExamen(@PathVariable Long pacienteId,
                                                      @Valid @RequestBody ExamenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearExamen(pacienteId, request));
    }

    @PutMapping("/examenes/{id}")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Editar un examen clínico")
    public ResponseEntity<ExamenResponse> actualizarExamen(@PathVariable Long id,
                                                           @Valid @RequestBody ExamenRequest request) {
        return ResponseEntity.ok(servicio.actualizarExamen(id, request));
    }

    // --------------------------- Diagnósticos -------------------------

    @GetMapping("/pacientes/{pacienteId}/diagnosticos")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_READ')")
    @Operation(summary = "Listar diagnósticos (historial conservado)")
    public ResponseEntity<List<DiagnosticoResponse>> listarDiagnosticos(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.listarDiagnosticos(pacienteId));
    }

    @PostMapping("/pacientes/{pacienteId}/diagnosticos")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Registrar un diagnóstico")
    public ResponseEntity<DiagnosticoResponse> crearDiagnostico(@PathVariable Long pacienteId,
                                                                @Valid @RequestBody DiagnosticoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearDiagnostico(pacienteId, request));
    }

    @PutMapping("/diagnosticos/{id}")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Actualizar estado/datos de un diagnóstico")
    public ResponseEntity<DiagnosticoResponse> actualizarDiagnostico(@PathVariable Long id,
                                                                     @Valid @RequestBody DiagnosticoRequest request) {
        return ResponseEntity.ok(servicio.actualizarDiagnostico(id, request));
    }

    // --------------------------- Evoluciones --------------------------

    @GetMapping("/pacientes/{pacienteId}/evoluciones")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_READ')")
    @Operation(summary = "Listar evoluciones con sus adendas")
    public ResponseEntity<List<EvolucionResponse>> listarEvoluciones(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.listarEvoluciones(pacienteId));
    }

    @PostMapping("/pacientes/{pacienteId}/evoluciones")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Registrar una evolución")
    public ResponseEntity<EvolucionResponse> crearEvolucion(@PathVariable Long pacienteId,
                                                            @Valid @RequestBody EvolucionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearEvolucion(pacienteId, request));
    }

    @PutMapping("/evoluciones/{id}")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Editar una evolución (sólo si está abierta)")
    public ResponseEntity<EvolucionResponse> actualizarEvolucion(@PathVariable Long id,
                                                                 @Valid @RequestBody EvolucionRequest request) {
        return ResponseEntity.ok(servicio.actualizarEvolucion(id, request));
    }

    @PostMapping("/evoluciones/{id}/cerrar")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_CLOSE')")
    @Operation(summary = "Cerrar una evolución (la vuelve inmutable)")
    public ResponseEntity<EvolucionResponse> cerrarEvolucion(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.cerrarEvolucion(id));
    }

    @PostMapping("/evoluciones/{id}/adendas")
    @PreAuthorize("hasAuthority('PERMISO_HISTORIA_CLINICA_WRITE')")
    @Operation(summary = "Agregar una adenda a una evolución cerrada")
    public ResponseEntity<AdendaResponse> crearAdenda(@PathVariable Long id,
                                                      @Valid @RequestBody AdendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearAdenda(id, request));
    }
}

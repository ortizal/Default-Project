package org.dentalcrm.web.cita;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/citas")
@Tag(name = "Citas", description = "Gestión de citas y transiciones de estado")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_CITAS_READ')")
    @Operation(summary = "Listar citas con filtros")
    public ResponseEntity<Page<CitaResponse>> listar(
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) String paciente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(citaService.listar(estado, doctorId, pacienteId, paciente, fecha, desde, hasta, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_CITAS_READ')")
    @Operation(summary = "Obtener cita por id")
    public ResponseEntity<CitaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_CITAS_WRITE')")
    @Operation(summary = "Crear cita (valida doble reserva, horario y bloqueos)")
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_CITAS_WRITE')")
    @Operation(summary = "Editar/mover cita")
    public ResponseEntity<CitaResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(citaService.actualizar(id, request));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('PERMISO_CITAS_WRITE')")
    @Operation(summary = "Confirmar cita")
    public ResponseEntity<CitaResponse> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('PERMISO_CITAS_CANCEL')")
    @Operation(summary = "Cancelar cita")
    public ResponseEntity<CitaResponse> cancelar(@PathVariable Long id,
                                                 @RequestBody(required = false) CancelarRequest request) {
        return ResponseEntity.ok(citaService.cancelar(id, request));
    }

    @PostMapping("/{id}/atender")
    @PreAuthorize("hasAuthority('PERMISO_CITAS_WRITE')")
    @Operation(summary = "Marcar cita atendida")
    public ResponseEntity<CitaResponse> atender(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.atender(id));
    }

    @PostMapping("/{id}/no-asistio")
    @PreAuthorize("hasAuthority('PERMISO_CITAS_WRITE')")
    @Operation(summary = "Marcar cita como no asistió")
    public ResponseEntity<CitaResponse> noAsistio(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.noAsistio(id));
    }
}
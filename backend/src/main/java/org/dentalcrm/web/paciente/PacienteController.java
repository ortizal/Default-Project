package org.dentalcrm.web.paciente;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.paciente.dto.PacienteRequest;
import org.dentalcrm.web.paciente.dto.PacienteResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pacientes")
@Tag(name = "Pacientes", description = "Gestión de pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_PACIENTES_READ')")
    @Operation(summary = "Listar pacientes (buscar por nombre, cédula o teléfono)")
    public ResponseEntity<Page<PacienteResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pacienteService.listar(q, estado, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PACIENTES_READ')")
    @Operation(summary = "Obtener paciente por id")
    public ResponseEntity<PacienteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_PACIENTES_WRITE')")
    @Operation(summary = "Crear paciente")
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PACIENTES_WRITE')")
    @Operation(summary = "Editar paciente")
    public ResponseEntity<PacienteResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.ok(pacienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_PACIENTES_DELETE')")
    @Operation(summary = "Desactivar paciente")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        pacienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
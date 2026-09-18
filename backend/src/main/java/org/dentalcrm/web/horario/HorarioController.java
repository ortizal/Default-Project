package org.dentalcrm.web.horario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.horario.dto.HorarioRequest;
import org.dentalcrm.web.horario.dto.HorarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/horarios")
@Tag(name = "Horarios", description = "Horarios de atención por odontólogo")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_READ')")
    @Operation(summary = "Horarios de un odontólogo")
    public ResponseEntity<List<HorarioResponse>> listar(@RequestParam Long odontologoId) {
        return ResponseEntity.ok(horarioService.listarPorOdontologo(odontologoId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_WRITE')")
    @Operation(summary = "Crear horario")
    public ResponseEntity<HorarioResponse> crear(@Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_WRITE')")
    @Operation(summary = "Editar horario")
    public ResponseEntity<HorarioResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(horarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_WRITE')")
    @Operation(summary = "Eliminar horario")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        horarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
package org.dentalcrm.web.odontologo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.odontologo.dto.OdontologoRequest;
import org.dentalcrm.web.odontologo.dto.OdontologoResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/odontologos")
@Tag(name = "Odontólogos", description = "Gestión de odontólogos")
public class OdontologoController {

    private final OdontologoService odontologoService;

    public OdontologoController(OdontologoService odontologoService) {
        this.odontologoService = odontologoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_ODONTOLOGOS_READ')")
    @Operation(summary = "Listar odontólogos")
    public ResponseEntity<Page<OdontologoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(odontologoService.listar(q, estado, page, size));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOLOGOS_READ')")
    @Operation(summary = "Listar odontólogos activos (para selección)")
    public ResponseEntity<List<OdontologoResponse>> activos() {
        return ResponseEntity.ok(odontologoService.activos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOLOGOS_READ')")
    @Operation(summary = "Obtener odontólogo por id")
    public ResponseEntity<OdontologoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(odontologoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_ODONTOLOGOS_WRITE')")
    @Operation(summary = "Crear odontólogo")
    public ResponseEntity<OdontologoResponse> crear(@Valid @RequestBody OdontologoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOLOGOS_WRITE')")
    @Operation(summary = "Editar odontólogo")
    public ResponseEntity<OdontologoResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody OdontologoRequest request) {
        return ResponseEntity.ok(odontologoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOLOGOS_WRITE')")
    @Operation(summary = "Desactivar odontólogo")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        odontologoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
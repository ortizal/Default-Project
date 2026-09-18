package org.dentalcrm.web.servicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.servicio.dto.ServicioRequest;
import org.dentalcrm.web.servicio.dto.ServicioResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios")
@Tag(name = "Servicios", description = "Gestión de servicios odontológicos")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_SERVICIOS_READ')")
    @Operation(summary = "Listar servicios")
    public ResponseEntity<Page<ServicioResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(servicioService.listar(q, estado, page, size));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('PERMISO_SERVICIOS_READ')")
    @Operation(summary = "Listar servicios activos (para selección)")
    public ResponseEntity<List<ServicioResponse>> activos() {
        return ResponseEntity.ok(servicioService.activos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_SERVICIOS_READ')")
    @Operation(summary = "Obtener servicio por id")
    public ResponseEntity<ServicioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_SERVICIOS_WRITE')")
    @Operation(summary = "Crear servicio")
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_SERVICIOS_WRITE')")
    @Operation(summary = "Editar servicio")
    public ResponseEntity<ServicioResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_SERVICIOS_WRITE')")
    @Operation(summary = "Desactivar servicio")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        servicioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
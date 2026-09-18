package org.dentalcrm.web.automatizacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.automatizacion.dto.PlantillaRequest;
import org.dentalcrm.web.automatizacion.dto.PlantillaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plantillas")
@Tag(name = "Plantillas", description = "Plantillas de mensajes con variables")
public class PlantillaController {

    private final PlantillaService plantillaService;

    public PlantillaController(PlantillaService plantillaService) {
        this.plantillaService = plantillaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Listar plantillas")
    public ResponseEntity<List<PlantillaResponse>> listar() {
        return ResponseEntity.ok(plantillaService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Crear plantilla")
    public ResponseEntity<PlantillaResponse> crear(@Valid @RequestBody PlantillaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Actualizar plantilla")
    public ResponseEntity<PlantillaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody PlantillaRequest request) {
        return ResponseEntity.ok(plantillaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Eliminar plantilla")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        plantillaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
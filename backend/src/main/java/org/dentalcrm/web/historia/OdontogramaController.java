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
@Tag(name = "Odontograma", description = "Versiones FDI por paciente: dientes, superficies, condiciones y comparación")
public class OdontogramaController {

    private final OdontogramaService servicio;

    public OdontogramaController(OdontogramaService servicio) {
        this.servicio = servicio;
    }

    // ---------------------------- Consultas ---------------------------

    @GetMapping("/pacientes/{pacienteId}/odontograma")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_READ')")
    @Operation(summary = "Odontograma vigente (la versión más reciente); null si no existe")
    public ResponseEntity<OdontogramaResponse> vigente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.vigente(pacienteId));
    }

    @GetMapping("/pacientes/{pacienteId}/odontogramas")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_READ')")
    @Operation(summary = "Historial de versiones del odontograma")
    public ResponseEntity<List<OdontogramaResumenResponse>> historial(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.historial(pacienteId));
    }

    @GetMapping("/pacientes/{pacienteId}/odontogramas/{version}")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_READ')")
    @Operation(summary = "Detalle de una versión concreta (para comparación)")
    public ResponseEntity<OdontogramaResponse> version(@PathVariable Long pacienteId,
                                                       @PathVariable int version) {
        return ResponseEntity.ok(servicio.version(pacienteId, version));
    }

    @GetMapping("/pacientes/{pacienteId}/odontograma/comparar")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_READ')")
    @Operation(summary = "Comparar dos versiones (nuevos, modificados y eliminados)")
    public ResponseEntity<ComparacionResponse> comparar(@PathVariable Long pacienteId,
                                                        @RequestParam int desde,
                                                        @RequestParam int hasta) {
        return ResponseEntity.ok(servicio.comparar(pacienteId, desde, hasta));
    }

    @GetMapping("/odontogramas/{id}/eventos")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_READ')")
    @Operation(summary = "Log de cambios de una versión")
    public ResponseEntity<List<EventoResponse>> eventos(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.eventos(id));
    }

    // ---------------------------- Versiones ---------------------------

    @PostMapping("/pacientes/{pacienteId}/odontogramas")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_WRITE')")
    @Operation(summary = "Crear la siguiente versión (por defecto copia la anterior)")
    public ResponseEntity<OdontogramaResponse> crearVersion(@PathVariable Long pacienteId,
                                                            @RequestBody NuevaVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearVersion(pacienteId, request));
    }

    // ---------------------------- Dientes -----------------------------

    @PostMapping("/odontogramas/{id}/dientes")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_WRITE')")
    @Operation(summary = "Registrar o actualizar una condición (diente + superficie + condición)")
    public ResponseEntity<DienteResponse> guardarDiente(@PathVariable Long id,
                                                        @Valid @RequestBody DienteRequest request) {
        return ResponseEntity.ok(servicio.guardarDiente(id, request));
    }

    @DeleteMapping("/odontogramas/{id}/dientes/{dienteId}")
    @PreAuthorize("hasAuthority('PERMISO_ODONTOGRAMA_WRITE')")
    @Operation(summary = "Eliminar una condición del odontograma")
    public ResponseEntity<Void> eliminarDiente(@PathVariable Long id, @PathVariable Long dienteId) {
        servicio.eliminarDiente(id, dienteId);
        return ResponseEntity.noContent().build();
    }
}

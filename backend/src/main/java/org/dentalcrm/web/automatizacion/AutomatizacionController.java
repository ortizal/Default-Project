package org.dentalcrm.web.automatizacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.domain.automatizacion.NotificacionRepository;
import org.dentalcrm.web.automatizacion.dto.AutomatizacionRequest;
import org.dentalcrm.web.automatizacion.dto.AutomatizacionResponse;
import org.dentalcrm.web.automatizacion.dto.NotificacionResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/automatizaciones")
@Tag(name = "Automatizaciones", description = "Reglas, plantillas y notificaciones")
public class AutomatizacionController {

    private final AutomatizacionService automatizacionService;
    private final NotificacionRepository notificacionRepository;

    public AutomatizacionController(AutomatizacionService automatizacionService,
                                    NotificacionRepository notificacionRepository) {
        this.automatizacionService = automatizacionService;
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Listar automatizaciones")
    public ResponseEntity<List<AutomatizacionResponse>> listar() {
        return ResponseEntity.ok(automatizacionService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Crear automatización")
    public ResponseEntity<AutomatizacionResponse> crear(@Valid @RequestBody AutomatizacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(automatizacionService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Actualizar automatización")
    public ResponseEntity<AutomatizacionResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody AutomatizacionRequest request) {
        return ResponseEntity.ok(automatizacionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Eliminar automatización")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        automatizacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Activar automatización")
    public ResponseEntity<AutomatizacionResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(automatizacionService.activar(id));
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Desactivar automatización")
    public ResponseEntity<AutomatizacionResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(automatizacionService.desactivar(id));
    }

    @GetMapping("/notificaciones")
    @PreAuthorize("hasAuthority('PERMISO_AUTOMATIZACIONES')")
    @Operation(summary = "Listar últimas notificaciones")
    public ResponseEntity<List<NotificacionResponse>> notificaciones(
            @RequestParam(defaultValue = "50") int limite,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String evento) {
        int limiteSeguro = Math.min(Math.max(limite, 1), 200);
        org.dentalcrm.domain.automatizacion.EstadoNotificacion estadoFiltro = null;
        if (estado != null && !estado.isBlank()) {
            estadoFiltro = org.dentalcrm.domain.automatizacion.EstadoNotificacion.valueOf(estado.trim().toUpperCase());
        }
        org.dentalcrm.domain.automatizacion.EventoAutomatizacion eventoFiltro = null;
        if (evento != null && !evento.isBlank()) {
            eventoFiltro = org.dentalcrm.domain.automatizacion.EventoAutomatizacion.valueOf(evento.trim().toUpperCase());
        }
        final org.dentalcrm.domain.automatizacion.EstadoNotificacion estadoFinal = estadoFiltro;
        final org.dentalcrm.domain.automatizacion.EventoAutomatizacion eventoFinal = eventoFiltro;
        return ResponseEntity.ok(notificacionRepository.ultimas(PageRequest.of(0, limiteSeguro)).stream()
                .filter(n -> estadoFinal == null || n.getEstado() == estadoFinal)
                .filter(n -> eventoFinal == null || (n.getAutomatizacion() != null && n.getAutomatizacion().getEvento() == eventoFinal))
                .map(NotificacionResponse::from).toList());
    }
}
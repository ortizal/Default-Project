package org.dentalcrm.web.whatsapp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.whatsapp.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp (OpenWA)", description = "Sesiones, conversaciones y mensajes")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/sesiones")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_READ')")
    @Operation(summary = "Listar sesiones de WhatsApp (paginado)")
    public ResponseEntity<org.springframework.data.domain.Page<WhatsAppSesionResponse>> listarSesiones(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(whatsAppService.listarSesiones(busca, estado, page, size));
    }

    @PostMapping("/sesiones")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_WRITE')")
    @Operation(summary = "Registrar una sesión de WhatsApp")
    public ResponseEntity<WhatsAppSesionResponse> crearSesion(@Valid @RequestBody WhatsAppSesionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(whatsAppService.crearSesion(request));
    }

    @PostMapping("/sesiones/{id}/conectar")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_WRITE')")
    @Operation(summary = "Conectar sesión en OpenWA")
    public ResponseEntity<WhatsAppSesionResponse> conectar(@PathVariable Long id) {
        return ResponseEntity.ok(whatsAppService.conectar(id));
    }

    @PostMapping("/sesiones/{id}/desconectar")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_WRITE')")
    @Operation(summary = "Desconectar sesión")
    public ResponseEntity<WhatsAppSesionResponse> desconectar(@PathVariable Long id) {
        return ResponseEntity.ok(whatsAppService.desconectar(id));
    }

    @DeleteMapping("/sesiones/{id}")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_DELETE')")
    @Operation(summary = "Eliminar sesión y su QR")
    public ResponseEntity<Void> borrarSesion(@PathVariable Long id) {
        whatsAppService.borrarSesion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversaciones")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_READ')")
    @Operation(summary = "Listar conversaciones (inbox)")
    public ResponseEntity<List<ConversacionResponse>> listarConversaciones(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(whatsAppService.listarConversaciones(q));
    }

    @GetMapping("/conversaciones/{id}")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_READ')")
    @Operation(summary = "Obtener conversación con su historial")
    public ResponseEntity<ConversacionDetalleResponse> obtenerConversacion(@PathVariable Long id) {
        return ResponseEntity.ok(whatsAppService.obtenerConversacion(id));
    }

    @PostMapping("/conversaciones/{id}/mensajes")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_WRITE')")
    @Operation(summary = "Enviar mensaje manual (salida)")
    public ResponseEntity<MensajeResponse> enviarMensaje(@PathVariable Long id,
                                                         @Valid @RequestBody EnviarMensajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(whatsAppService.enviarMensaje(id, request));
    }

    @PostMapping("/conversaciones/{id}/estado")
    @PreAuthorize("hasAuthority('PERMISO_WHATSAPP_WRITE')")
    @Operation(summary = "Cambiar estado de conversación (inbox)")
    public ResponseEntity<ConversacionResponse> cambiarEstado(@PathVariable Long id,
                                                              @Valid @RequestBody CambiarEstadoConversacionRequest request) {
        return ResponseEntity.ok(whatsAppService.cambiarEstado(id, request));
    }
}
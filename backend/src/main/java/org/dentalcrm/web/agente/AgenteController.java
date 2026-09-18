package org.dentalcrm.web.agente;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.agente.dto.ActivarAgenteRequest;
import org.dentalcrm.web.agente.dto.AgenteConversacionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agente")
@Tag(name = "Agente de IA", description = "Supervisión del agente conversacional de WhatsApp")
public class AgenteController {

    private final AgenteConversacionalService agenteService;

    public AgenteController(AgenteConversacionalService agenteService) {
        this.agenteService = agenteService;
    }

    @GetMapping("/conversaciones")
    @PreAuthorize("hasAuthority('PERMISO_AGENTE_IA')")
    @Operation(summary = "Lista las conversaciones con el estado del agente")
    public ResponseEntity<List<AgenteConversacionResponse>> conversaciones(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(agenteService.listarConversacionesAgente(q));
    }

    @PostMapping("/conversaciones/{id}/transferir")
    @PreAuthorize("hasAuthority('PERMISO_AGENTE_IA')")
    @Operation(summary = "Transfiere una conversación a atención humana")
    public ResponseEntity<AgenteConversacionResponse> transferir(@PathVariable Long id) {
        return ResponseEntity.ok(agenteService.transferirAHumano(id));
    }

    @PostMapping("/conversaciones/{id}/agente")
    @PreAuthorize("hasAuthority('PERMISO_AGENTE_IA')")
    @Operation(summary = "Activa o desactiva el agente en una conversación")
    public ResponseEntity<AgenteConversacionResponse> activar(@PathVariable Long id,
                                                              @RequestBody ActivarAgenteRequest request) {
        return ResponseEntity.ok(agenteService.activarAgente(id, request.activo()));
    }
}
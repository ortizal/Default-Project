package org.dentalcrm.web.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.whatsapp.dto.WebhookResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Eventos entrantes de proveedores externos (públicos)")
public class WebhookController {

    private final WhatsAppService whatsAppService;

    public WebhookController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/whatsapp")
    @Operation(summary = "Evento de OpenWA (mensaje entrante, estado de sesión, etc.)")
    public ResponseEntity<WebhookResponse> whatsapp(@RequestBody(required = false) JsonNode body) {
        return ResponseEntity.ok(whatsAppService.recibirWebhook(body));
    }
}
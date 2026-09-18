package org.dentalcrm.web.whatsapp.dto;

import org.dentalcrm.domain.whatsapp.WhatsappSesion;

import java.time.Instant;

public record WhatsAppSesionResponse(
        Long id,
        String sesionId,
        String nombre,
        String estado,
        String estadoDetalle,
        String qr,
        String lastError,
        Instant updatedAt,
        String proveedor
) {
    public static WhatsAppSesionResponse from(WhatsappSesion s, String proveedor) {
        return new WhatsAppSesionResponse(
                s.getId(), s.getSesionId(), s.getNombre(),
                s.getEstado().name(), s.getEstadoDetalle(), s.getQr(),
                s.getLastError(), s.getUpdatedAt(), proveedor);
    }
}
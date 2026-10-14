package org.dentalcrm.web.consentimiento.dto;

import jakarta.validation.constraints.Size;

/**
 * Firma de un consentimiento: nombre del firmante obligatorio y firma
 * manuscrita opcional en data URL (PNG del lienzo).
 */
public record FirmarConsentimientoRequest(
        @Size(max = 150) String firmante,
        @Size(max = 200000) String firmaImagen
) {
}

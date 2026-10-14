package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contraseña del certificado .p12 para firmar el XML (plan §3.6).
 * Se guarda cifrada; nunca se devuelve en claro.
 */
public record FirmaPasswordRequest(
        @NotBlank @Size(max = 200) String password
) {
}

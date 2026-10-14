package org.dentalcrm.web.facturacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;

/**
 * Actualizar la configuración tributaria (plan §3.5). La clave del SRI es
 * opcional: en blanco se conserva la guardada y con {@code limpiarSriClave}
 * se elimina.
 */
public record ConfiguracionFacturacionRequest(
        @NotNull AmbienteSri ambiente,
        @NotNull TipoEmisionSri tipoEmision,
        boolean contribuyenteEspecial,
        boolean agenteRetencion,
        boolean obligadoContabilidad,
        @NotBlank @Pattern(regexp = "^[0-9]{3}$") String establecimiento,
        @NotBlank @Pattern(regexp = "^[0-9]{3}$") String puntoEmision,
        @Min(0) @Max(999999999) int secuencialActual,
        @NotBlank @Size(max = 500) String sriUrlPruebas,
        @NotBlank @Size(max = 500) String sriUrlProduccion,
        @Size(max = 100) String sriUsuario,
        @Size(max = 200) String sriClave,
        boolean limpiarSriClave
) {
}

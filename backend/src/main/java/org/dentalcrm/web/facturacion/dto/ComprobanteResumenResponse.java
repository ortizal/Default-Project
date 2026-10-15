package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.paciente.Paciente;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Fila de la bandeja de comprobantes (plan §3.7): lo que la tabla
 * necesita mostrar sin traer el XML. El detalle completo está en
 * {@link ComprobanteElectronicoResponse}.
 */
public record ComprobanteResumenResponse(
        Long id,
        String tipo,
        String numeroComprobante,
        String claveAcceso,
        Long pacienteId,
        String paciente,
        String identificacion,
        BigDecimal total,
        LocalDate fechaEmision,
        String estadoGeneracion,
        String estadoTributario,
        boolean simulado,
        String errorMensaje,
        Long cuentaId,
        /** Motivo de la nota de crédito / anulación, si aplica (§3.9/§3.10). */
        String motivo,
        /** Factura que modifica esta nota de crédito (§3.9), si la hay. */
        Long comprobanteReferenciaId,
        Instant createdAt
) {

    public static ComprobanteResumenResponse de(ComprobanteElectronico c, Paciente paciente) {
        return new ComprobanteResumenResponse(
                c.getId(),
                c.getTipoComprobante() == null ? null : c.getTipoComprobante().codigo(),
                c.getNumeroComprobante(),
                c.getClaveAcceso(),
                c.getPacienteId(),
                nombre(paciente),
                paciente == null ? null : paciente.getCedula(),
                c.getTotal(),
                c.getFechaEmision(),
                c.getEstadoGeneracion() == null ? null : c.getEstadoGeneracion().name(),
                // En modo simulado el "SRI local" autoriza (ver
                // AuthorizationService); se expone el estado guardado y el
                // badge `simulado` de la UI avisa que no tiene validez
                // tributaria.
                c.getEstadoTributario() == null ? null : c.getEstadoTributario().name(),
                c.isSimulado(),
                c.getErrorMensaje(),
                c.getCuentaId(),
                c.getMotivo(),
                c.getComprobanteReferenciaId(),
                c.getCreatedAt());
    }

    private static String nombre(Paciente paciente) {
        if (paciente == null) {
            return null;
        }
        String nombre = ((paciente.getNombres() == null ? "" : paciente.getNombres().trim())
                + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos().trim())).trim();
        return nombre.isEmpty() ? null : nombre;
    }
}

package org.dentalcrm.web.facturacion.dto;

import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.EstadoGeneracion;
import org.dentalcrm.domain.comprobante.EstadoTributario;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Comprobante electrónico emitido (plan §3.7 bandeja, §3.8 estados).
 *
 * @param simulado        true cuando el envío fue simulado (firma sin
 *                        certificado .p12, sólo permitido en pruebas): la UI
 *                        lo muestra como badge y no tiene validez tributaria
 * @param estadoGeneracion lo que hizo la aplicación (BORRADOR → GENERADO →
 *                        FIRMADO → ENVIADO | ERROR)
 * @param estadoTributario lo que dice el SRI (PENDIENTE → AUTORIZADO |
 *                        DEVUELTO | NO_AUTORIZADO | ANULADO)
 */
public record ComprobanteElectronicoResponse(
        Long id,
        String tipo,
        String numeroComprobante,
        String claveAcceso,
        String ambiente,
        String modoFirma,
        boolean simulado,
        String estadoGeneracion,
        String estadoTributario,
        String sriEstado,
        String sriRespuesta,
        String errorMensaje,
        String autorizacionNumero,
        Instant autorizacionFecha,
        BigDecimal total,
        LocalDate fechaEmision,
        Long cuentaId,
        Long pacienteId,
        /** Factura que modifica esta nota de crédito (plan §3.9), si la hay. */
        Long comprobanteReferenciaId,
        /** Motivo de la nota de crédito o de la anulación (§3.9/§3.10). */
        String motivo,
        /** Detalle leído del XML: vacío salvo en el detalle de la bandeja. */
        List<ComprobanteLineaResponse> detalles,
        Instant createdAt
) {

    public static ComprobanteElectronicoResponse from(ComprobanteElectronico c) {
                boolean simulado = c.isSimulado();
        return new ComprobanteElectronicoResponse(
                c.getId(),
                c.getTipoComprobante() == null ? null : c.getTipoComprobante().codigo(),
                c.getNumeroComprobante(),
                c.getClaveAcceso(),
                c.getAmbiente() == null ? null : c.getAmbiente().name(),
                c.getModoFirma(),
                c.isSimulado(),
                // §3.8: el estado de generación refleja lo que hizo la
                // aplicación (BORRADOR → GENERADO → FIRMADO → ENVIADO). Un
                // documento firmado con firma simulada se queda FIRMADO: la
                // firma existió, sólo que no se contactó al SRI.
                c.getEstadoGeneracion() == null ? null : c.getEstadoGeneracion().name(),
                simulado ? EstadoTributario.PENDIENTE.name()
                        : c.getEstadoTributario() == null ? null : c.getEstadoTributario().name(),
                c.getSriEstado(),
                simulado ? "Prueba local: no existe recepción ni autorización del SRI." : c.getSriRespuesta(),
                c.getErrorMensaje(),
                simulado ? null : c.getAutorizacionNumero(),
                simulado ? null : c.getAutorizacionFecha(),
                c.getTotal(),
                c.getFechaEmision(),
                c.getCuentaId(),
                c.getPacienteId(),
                c.getComprobanteReferenciaId(),
                c.getMotivo(),
                List.of(),
                c.getCreatedAt());
    }

    /** Detalle completo, con las líneas del XML para una nota parcial (§3.9). */
    public static ComprobanteElectronicoResponse detalle(
            ComprobanteElectronico c, List<ComprobanteLineaResponse> lineas) {
        ComprobanteElectronicoResponse base = from(c);
        return new ComprobanteElectronicoResponse(
                base.id(), base.tipo(), base.numeroComprobante(), base.claveAcceso(),
                base.ambiente(), base.modoFirma(), base.simulado(), base.estadoGeneracion(),
                base.estadoTributario(), base.sriEstado(), base.sriRespuesta(),
                base.errorMensaje(), base.autorizacionNumero(), base.autorizacionFecha(),
                base.total(), base.fechaEmision(), base.cuentaId(), base.pacienteId(),
                base.comprobanteReferenciaId(), base.motivo(), lineas, base.createdAt());
    }
}

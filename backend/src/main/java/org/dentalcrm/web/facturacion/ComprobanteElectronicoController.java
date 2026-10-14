package org.dentalcrm.web.facturacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.facturacion.dto.AnularComprobanteRequest;
import org.dentalcrm.web.facturacion.dto.ComprobanteElectronicoResponse;
import org.dentalcrm.web.facturacion.dto.ComprobanteFiltroRequest;
import org.dentalcrm.web.facturacion.dto.ComprobanteResumenResponse;
import org.dentalcrm.web.facturacion.dto.EmitirFacturaRequest;
import org.dentalcrm.web.facturacion.dto.EmitirNotaCreditoRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprobantes electrónicos — Fase G (plan §3.5/§3.6): emisión de la
 * factura desde la cuenta del paciente, bandeja (§3.7) y, con la Fase H,
 * notas de crédito (§3.9) y anulaciones (§3.10).
 */
@RestController
@RequestMapping("/api/v1/facturacion")
@Tag(name = "Facturación — comprobantes electrónicos",
        description = "Emisión de la factura electrónica: clave, XML, XSD, firma, "
                + "envío y autorización del SRI (Fase G, plan §3.5)")
public class ComprobanteElectronicoController {

    private final ComprobanteElectronicoService servicio;

    public ComprobanteElectronicoController(ComprobanteElectronicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/comprobantes")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Bandeja de comprobantes",
            description = "Listado paginado con filtros por fecha, texto libre (número, clave "
                    + "o paciente), estados, establecimiento y usuario (plan §3.7).")
    public ResponseEntity<Page<ComprobanteResumenResponse>> listar(ComprobanteFiltroRequest filtro) {
        return ResponseEntity.ok(servicio.listar(filtro));
    }

    @GetMapping("/comprobantes/{id}")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Detalle de un comprobante",
            description = "Estados, respuesta del SRI y mensaje de error si lo hubo.")
    public ResponseEntity<ComprobanteElectronicoResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.detalle(id));
    }

    @GetMapping("/comprobantes/{id}/xml")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Descargar el XML del comprobante")
    public ResponseEntity<String> xml(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + servicio.nombreXml(id) + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(servicio.xml(id));
    }

    @PostMapping("/comprobantes/{id}/reenviar")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_EMITIR')")
    @Operation(summary = "Reenviar al SRI un comprobante que falló",
            description = "Sólo para comprobantes sin respuesta definitiva (plan §3.7).")
    public ResponseEntity<ComprobanteElectronicoResponse> reenviar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.reenviar(id));
    }

    @PostMapping("/comprobantes/{id}/consultar")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_EMITIR')")
    @Operation(summary = "Consultar al SRI la autorización de un comprobante enviado")
    public ResponseEntity<ComprobanteElectronicoResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(id));
    }

    @PostMapping("/comprobantes")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_EMITIR')")
    @Operation(summary = "Emitir la factura electrónica de una cuenta",
            description = "Genera la clave de acceso, el XML, lo valida contra el XSD, lo firma "
                    + "y lo lleva hasta la autorización del SRI. En ambiente de pruebas sin "
                    + "certificado queda como prueba local pendiente, sin envío ni autorización tributaria.")
    public ResponseEntity<ComprobanteElectronicoResponse> emitir(
            @Valid @RequestBody EmitirFacturaRequest request) {
        return ResponseEntity.ok(servicio.emitir(request));
    }

    @PostMapping("/notas-credito")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_ANULAR')")
    @Operation(summary = "Emitir una nota de crédito electrónica",
            description = "Devolución total o parcial de una factura autorizada (plan §3.9): "
                    + "referencia obligatoria al documento original, motivo, líneas e impuestos; "
                    + "recorre el mismo flujo que la factura (XML, XSD, firma y autorización).")
    public ResponseEntity<ComprobanteElectronicoResponse> emitirNotaCredito(
            @Valid @RequestBody EmitirNotaCreditoRequest request) {
        return ResponseEntity.ok(servicio.emitirNotaCredito(request));
    }

    @PostMapping("/comprobantes/{id}/anular")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_ANULAR')")
    @Operation(summary = "Anular un comprobante autorizado",
            description = "Requiere integración disponible con el SRI; sin ella responde 501 y conserva el comprobante.")
    public ResponseEntity<ComprobanteElectronicoResponse> anular(
            @PathVariable Long id, @Valid @RequestBody AnularComprobanteRequest request) {
        return ResponseEntity.ok(servicio.anular(id, request));
    }

    @GetMapping("/comprobantes/{id}/ride")
    @PreAuthorize("hasAuthority('PERMISO_FACTURACION_READ')")
    @Operation(summary = "Descargar el RIDE en PDF del comprobante",
            description = "Representación impresa con datos fiscales, adquiriente, detalle, "
                    + "impuestos, formas de pago, clave de acceso y autorización (plan §3.11).")
    public ResponseEntity<byte[]> ride(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + servicio.nombreRide(id) + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(servicio.ride(id));
    }
}

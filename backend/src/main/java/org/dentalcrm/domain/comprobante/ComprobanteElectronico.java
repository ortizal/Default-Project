package org.dentalcrm.domain.comprobante;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comprobante electrónico emitido (Fase G, plan §3.5/§3.8).
 *
 * <p>Conserva el XML firmado, la respuesta del SRI y los DOS estados que
 * exige el plan: {@code estadoGeneracion} (lo que hizo la aplicación) y
 * {@code estadoTributario} (lo que dice el SRI). Las filas de comprobantes
 * emitidos nunca se eliminan (§3.8): la anulación es un estado.
 *
 * <p>El desglose tributario (líneas, impuestos, pagos) vive en el XML; aquí
 * sólo se guarda lo necesario para la bandeja (§3.7) y para reenviar/consultar.
 */
@Entity
@Table(name = "comprobantes_electronicos")
@Getter
@Setter
@NoArgsConstructor
public class ComprobanteElectronico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @Convert(converter = TipoComprobanteSriConverter.class)
    @Column(name = "tipo_comprobante", nullable = false, length = 2)
    private TipoComprobanteSri tipoComprobante = TipoComprobanteSri.FACTURA;

    @Column(name = "clave_acceso", nullable = false, length = 49)
    private String claveAcceso;

    @Column(nullable = false, length = 3)
    private String establecimiento;

    @Column(name = "punto_emision", nullable = false, length = 3)
    private String puntoEmision;

    @Column(nullable = false)
    private int secuencial;

    /** Número legible para el usuario, p. ej. {@code 001-001-000000001}. */
    @Column(name = "numero_comprobante", nullable = false, length = 20)
    private String numeroComprobante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbienteSri ambiente;

    /** {@code SIMULADA} o {@code CERTIFICADO} ({@code ElectronicSignatureService}). */
    @Column(name = "modo_firma", nullable = false, length = 20)
    private String modoFirma;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_generacion", nullable = false, length = 20)
    private EstadoGeneracion estadoGeneracion = EstadoGeneracion.BORRADOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_tributario", nullable = false, length = 20)
    private EstadoTributario estadoTributario = EstadoTributario.PENDIENTE;

    /** true si el "envío" fue simulado (firma sin certificado, sólo en pruebas). */
    @Column(nullable = false)
    private boolean simulado;

    /** {@code importeTotal} del XML (IVA incluido), para la bandeja sin parsearlo. */
    @Column(precision = 14, scale = 2)
    private BigDecimal total;

    /** Fecha de emisión de la clave de acceso (ya trasladada al día hábil). */
    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "xml_firmado", columnDefinition = "text")
    private String xmlFirmado;

    @Column(name = "xml_autorizado", columnDefinition = "text")
    private String xmlAutorizado;

    @Column(name = "autorizacion_numero", length = 49)
    private String autorizacionNumero;

    @Column(name = "autorizacion_fecha")
    private Instant autorizacionFecha;

    @Column(name = "sri_estado", length = 30)
    private String sriEstado;

    @Column(name = "sri_respuesta", columnDefinition = "text")
    private String sriRespuesta;

    @Column(name = "error_mensaje", columnDefinition = "text")
    private String errorMensaje;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    @Column(name = "paciente_id")
    private Long pacienteId;

    /**
     * Comprobante al que modifica esta nota de crédito (§3.9): referencia
     * obligatoria al documento original, que nunca se elimina.
     */
    @Column(name = "comprobante_referencia_id")
    private Long comprobanteReferenciaId;

    /**
     * Motivo de la nota de crédito (máx. 300, lo pide el XSD del SRI) o de
     * la anulación (máx. 500) — plan §3.9/§3.10.
     */
    @Column(length = 500)
    private String motivo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** true cuando el SRI ya dio su respuesta definitiva. */
    public boolean tributariamenteDefinitivo() {
        return estadoTributario == EstadoTributario.AUTORIZADO
                || estadoTributario == EstadoTributario.NO_AUTORIZADO
                || estadoTributario == EstadoTributario.ANULADO;
    }

    /**
     * Resumen del comprobante para la auditoría (clave, número y estados);
     * los campos vacíos se omiten para no ensuciar el JSON.
     */
    public Map<String, Object> datosAuditoria() {
        Map<String, Object> datos = new LinkedHashMap<>();
        put(datos, "claveAcceso", claveAcceso);
        put(datos, "numeroComprobante", numeroComprobante);
        put(datos, "cuentaId", cuentaId);
        put(datos, "total", total);
        put(datos, "fechaEmision", fechaEmision);
        put(datos, "estadoGeneracion", estadoGeneracion == null ? null : estadoGeneracion.name());
        put(datos, "estadoTributario", estadoTributario == null ? null : estadoTributario.name());
        put(datos, "sriEstado", sriEstado);
        put(datos, "simulado", simulado);
        put(datos, "error", errorMensaje);
        return datos;
    }

    private static void put(Map<String, Object> datos, String clave, Object valor) {
        if (valor != null) {
            datos.put(clave, valor);
        }
    }
}

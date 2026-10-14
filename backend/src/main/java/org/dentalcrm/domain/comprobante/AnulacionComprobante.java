package org.dentalcrm.domain.comprobante;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Solicitud de anulación de un comprobante electrónico — plan §3.10.
 *
 * <p>Registra un trámite tributario remoto con motivo, solicitante, fechas,
 * estado y respuesta verificable. <b>Nunca</b> se elimina la fila del
 * comprobante emitido: la anulación es un estado más de su vida.
 */
@Entity
@Table(name = "anulaciones_comprobante")
@Getter
@Setter
@NoArgsConstructor
public class AnulacionComprobante {

    /** El trámite se registró y todavía no tiene resultado del SRI. */
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    /** El SRI aceptó la anulación. */
    public static final String ESTADO_APLICADA = "APLICADA";
    /** El SRI rechazó la anulación: el comprobante sigue vigente. */
    public static final String ESTADO_RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "comprobante_id", nullable = false)
    private Long comprobanteId;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false, length = 20)
    private String estado = ESTADO_PENDIENTE;

    /** {@code APLICADA} o {@code RECHAZADA}; nulo mientras está pendiente. */
    @Column(length = 20)
    private String resultado;

    @Column(columnDefinition = "text")
    private String mensaje;

    @Column(name = "solicitado_por")
    private Long solicitadoPor;

    @Column(name = "solicitado_en", nullable = false, updatable = false)
    private Instant solicitadoEn;

    @Column(name = "aplicado_en")
    private Instant aplicadoEn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (solicitadoEn == null) {
            solicitadoEn = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Resumen para la auditoría (§3.10): solicitud, resultado y evidencia. */
    public java.util.Map<String, Object> datosAuditoria() {
        java.util.Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("comprobanteId", comprobanteId);
        datos.put("motivo", motivo);
        datos.put("estado", estado);
        if (resultado != null) {
            datos.put("resultado", resultado);
        }
        if (mensaje != null) {
            datos.put("mensaje", mensaje);
        }
        return datos;
    }
}

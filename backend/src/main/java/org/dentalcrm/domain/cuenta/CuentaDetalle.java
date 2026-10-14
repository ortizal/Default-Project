package org.dentalcrm.domain.cuenta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Movimiento del libro mayor de la cuenta (plan §3.3): cargo, descuento,
 * abono, anticipo o devolución. {@code total} es siempre positivo y el
 * signo lo aporta {@link TipoMovimientoCuenta#signo()}.
 */
@Entity
@Table(name = "cuenta_detalles")
@Getter
@Setter
@NoArgsConstructor
public class CuentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaPaciente cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoCuenta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenMovimientoCuenta origen = OrigenMovimientoCuenta.MANUAL;

    /** Dimensión de reportes: producción por odontólogo (§3.12/§3.13). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private org.dentalcrm.domain.odontologo.Odontologo odontologo;

    /** Dimensión de reportes: ingresos por servicio (§3.12/§3.13). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private org.dentalcrm.domain.servicio.Servicio servicio;

    /**
     * Identidad del hecho que originó el movimiento (id de la línea del
     * plan cuando el origen es PROCEDIMIENTO). La unicidad
     * {@code (cuenta_id, origen, referencia_id)} garantiza que un mismo
     * procedimiento sólo facture una vez.
     */
    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(nullable = false, length = 255)
    private String concepto;

    private Integer diente;

    @Column(length = 20)
    private String superficie;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "descuento_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPct = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", length = 20)
    private FormaPago formaPago;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    /** Referencia de transferencia, número de recibo, etc. */
    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false)
    private boolean anulado = false;

    @Column(name = "anulado_motivo", length = 255)
    private String anuladoMotivo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    /** Efecto firmado de este movimiento sobre el saldo de la cuenta. */
    public BigDecimal efecto() {
        if (anulado) {
            return BigDecimal.ZERO;
        }
        return total.multiply(BigDecimal.valueOf(tipo.signo()));
    }
}

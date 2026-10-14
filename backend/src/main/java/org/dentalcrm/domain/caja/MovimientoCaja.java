package org.dentalcrm.domain.caja;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Movimiento de una sesión de caja (plan §3.4). Cuando nace de un
 * cobro, anticipo o devolución de la cuenta del paciente conserva el
 * vínculo en {@code cuentaDetalleId} para poder anularlo en conjunto.
 */
@Entity
@Table(name = "movimientos_caja")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoCaja tipo;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 20)
    private FormaPago formaPago;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(name = "cuenta_detalle_id")
    private Long cuentaDetalleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

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

    /** Efecto firmado sobre el neto de la caja. */
    public BigDecimal efecto() {
        if (anulado) {
            return BigDecimal.ZERO;
        }
        BigDecimal valor = monto == null ? BigDecimal.ZERO : monto;
        return tipo == TipoMovimientoCaja.EGRESO ? valor.negate() : valor;
    }
}

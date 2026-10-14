package org.dentalcrm.domain.cuenta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cuenta financiera del paciente (plan §3.3): contenedor de los cargos,
 * descuentos, anticipos, abonos y devoluciones previos al comprobante.
 *
 * <p>Los totales y el saldo son DENORMALIZADOS: se recalculan en cada
 * movimiento a partir del libro {@link CuentaDetalle}, nunca se aceptan
 * desde un request.
 */
@Entity
@Table(name = "cuentas_paciente",
        uniqueConstraints = @UniqueConstraint(name = "uq_cuenta_paciente",
                columnNames = {"tenant_id", "paciente_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CuentaPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado = EstadoCuenta.ABIERTA;

    /** Cuanto falta por cobrar (puede quedar negativo: pago a favor). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "total_cargos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCargos = BigDecimal.ZERO;

    @Column(name = "total_descuentos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    /** Crédito acumulado del paciente: abonos + anticipos − devoluciones. */
    @Column(name = "total_abonos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAbonos = BigDecimal.ZERO;

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

    /** true si la cuenta no admite nuevos movimientos manuales. */
    public boolean bloqueada() {
        return estado == EstadoCuenta.CERRADA || estado == EstadoCuenta.ANULADA;
    }
}

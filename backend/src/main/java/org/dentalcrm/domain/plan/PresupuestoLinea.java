package org.dentalcrm.domain.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Línea congelada de un presupuesto (snapshot del plan al emitir).
 */
@Entity
@Table(name = "presupuesto_lineas")
@Getter
@Setter
@NoArgsConstructor
public class PresupuestoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private Presupuesto presupuesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_linea_id")
    private PlanLinea planLinea;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private Integer fase = 1;

    private Integer diente;

    @Column(length = 20)
    private String superficie;

    @Column(length = 150)
    private String diagnostico;

    @Column(nullable = false, length = 150)
    private String procedimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadLinea prioridad = PrioridadLinea.MEDIA;

    @Column(nullable = false)
    private Integer sesiones = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "descuento_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPct = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

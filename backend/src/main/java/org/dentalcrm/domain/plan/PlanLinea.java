package org.dentalcrm.domain.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.dentalcrm.domain.servicio.Servicio;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Procedimiento planificado dentro de un plan: diente/superficie,
 * diagnóstico, prioridad, sesiones, precio, descuento y estado.
 */
@Entity
@Table(name = "plan_lineas",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_linea_numero",
                columnNames = {"plan_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanTratamiento plan;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLineaPlan estado = EstadoLineaPlan.PENDIENTE;

    @Column(length = 255)
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        recalcularSubtotal();
    }

    @PreUpdate
    void onUpdate() {
        recalcularSubtotal();
        updatedAt = Instant.now();
    }

    /** subtotal = precio × sesiones × (1 − descuento/100), redondeado a 2 decimales. */
    public void recalcularSubtotal() {
        BigDecimal precio = precioUnitario == null ? BigDecimal.ZERO : precioUnitario;
        int ses = sesiones == null || sesiones < 1 ? 1 : sesiones;
        BigDecimal bruto = precio.multiply(BigDecimal.valueOf(ses));
        BigDecimal dto = descuentoPct == null ? BigDecimal.ZERO : descuentoPct;
        BigDecimal factor = BigDecimal.ONE.subtract(dto.movePointLeft(2));
        subtotal = bruto.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

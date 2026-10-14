package org.dentalcrm.domain.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Versión de presupuesto derivada de un plan: totales, aceptación y
 * firma (hash SHA-256 de lo que se firmó). Sólo uno queda `vigente`.
 */
@Entity
@Table(name = "presupuestos",
        uniqueConstraints = @UniqueConstraint(name = "uq_presupuesto_version",
                columnNames = {"plan_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanTratamiento plan;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPresupuesto estado = EstadoPresupuesto.BORRADOR;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Boolean vigente = false;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 255)
    private String observaciones;

    @Column(length = 255)
    private String comentario;

    @Column(length = 120)
    private String firmante;

    @Column(name = "firma_hash", length = 64)
    private String firmaHash;

    @Column(name = "aceptado_en")
    private Instant aceptadoEn;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<PresupuestoLinea> lineas = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (fecha == null) {
            fecha = LocalDate.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

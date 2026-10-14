package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

/** Medición periodontal de una pieza en un sitio concreto (plan §4.11). */
@Entity
@Table(name = "periodontograma_sitios")
@Getter
@Setter
@NoArgsConstructor
public class PeriodontogramaSitio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id", nullable = false)
    private ExamenPeriodontal examen;

    @Column(nullable = false)
    private int diente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SitioPeriodontal sitio;

    /** Profundidad de sondaje en mm (0–20). */
    @Column(name = "sondaje_mm", precision = 4, scale = 1)
    private BigDecimal sondajeMm;

    /** Margen gingival en mm (-5 a 10; negativo = encía recesiva). */
    @Column(name = "margen_mm", precision = 4, scale = 1)
    private BigDecimal margenMm;

    /** Nivel de inserción clínico en mm (0–20). */
    @Column(name = "insercion_mm", precision = 4, scale = 1)
    private BigDecimal insercionMm;

    @Column(nullable = false)
    private boolean sangrado;

    @Column(nullable = false)
    private boolean placa;

    @Column(nullable = false)
    private boolean supuracion;

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
}

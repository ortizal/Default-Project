package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/** Datos periodontales por pieza: movilidad (0–3), furcación (0–3) y notas. */
@Entity
@Table(name = "periodontograma_dientes")
@Getter
@Setter
@NoArgsConstructor
public class PeriodontogramaDiente {

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

    private Integer movilidad;

    private Integer furcacion;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

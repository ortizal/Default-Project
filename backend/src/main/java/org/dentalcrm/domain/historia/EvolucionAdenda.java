package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Adenda de una evolución ya cerrada: la nota original queda intacta y
 * la corrección queda auditada (plan §4.10).
 */
@Entity
@Table(name = "evolucion_adendas")
@Getter
@Setter
@NoArgsConstructor
public class EvolucionAdenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evolucion_id", nullable = false)
    private Evolucion evolucion;

    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    @Column(name = "autor_id")
    private Long autorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

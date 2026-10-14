package org.dentalcrm.domain.consentimiento;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/** Texto base de un tipo de consentimiento, editable por la clínica. */
@Entity
@Table(name = "consentimiento_plantillas",
        uniqueConstraints = @UniqueConstraint(name = "uq_consentimiento_plantilla",
                columnNames = {"tenant_id", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class ConsentimientoPlantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 60)
    private String categoria;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(nullable = false)
    private int version = 1;

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

package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Versión fechada del odontograma de una historia clínica (plan §4.5):
 * cada versión conserva su propio estado de dientes para poder comparar
 * en el tiempo. Nunca se almacena una imagen.
 */
@Entity
@Table(name = "odontogramas")
@Getter
@Setter
@NoArgsConstructor
public class Odontograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_id", nullable = false)
    private HistoriaClinica historia;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Denticion denticion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(columnDefinition = "text")
    private String observaciones;

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
        if (denticion == null) {
            denticion = Denticion.PERMANENTE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

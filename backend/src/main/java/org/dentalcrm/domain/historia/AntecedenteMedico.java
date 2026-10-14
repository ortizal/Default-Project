package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Antecedente estructurado (una fila por dato, plan §4.3): enfermedades,
 * cirugías, hospitalizaciones, alergias, medicación, antecedentes
 * odontológicos y hábitos.
 */
@Entity
@Table(name = "antecedentes_medicos")
@Getter
@Setter
@NoArgsConstructor
public class AntecedenteMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_id", nullable = false)
    private HistoriaClinica historia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAntecedente tipo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String detalle;

    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAntecedente estado;

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
        if (estado == null) {
            estado = EstadoAntecedente.ACTIVO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

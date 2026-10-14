package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Historia clínica de un paciente: una por paciente dentro del tenant.
 * Agrupa antecedentes, exámenes, diagnósticos y evoluciones.
 */
@Entity
@Table(name = "historias_clinicas",
        uniqueConstraints = @UniqueConstraint(name = "uq_historias_tenant_paciente",
                columnNames = {"tenant_id", "paciente_id"}))
@Getter
@Setter
@NoArgsConstructor
public class HistoriaClinica {

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
    private EstadoHistoria estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(name = "motivo_cierre", length = 255)
    private String motivoCierre;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Column(name = "cerrada_por")
    private Long cerradaPor;

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
            estado = EstadoHistoria.ABIERTA;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

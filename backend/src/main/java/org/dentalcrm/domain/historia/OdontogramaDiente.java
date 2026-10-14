package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Condición registrada sobre una pieza (FDI) y, opcionalmente, una de
 * sus superficies dentro de una versión de odontograma.
 */
@Entity
@Table(name = "odontograma_dientes")
@Getter
@Setter
@NoArgsConstructor
public class OdontogramaDiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontograma_id", nullable = false)
    private Odontograma odontograma;

    @Column(nullable = false)
    private int diente;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SuperficieDental superficie;

    @Column(nullable = false, length = 40)
    private String condicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOdontograma estado;

    @Column(length = 255)
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

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
            estado = EstadoOdontograma.EXISTENTE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

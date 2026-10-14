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
 * Diagnóstico clínico. El historial se conserva cambiando el estado
 * (sospecha → confirmado → resuelto/descartado), plan §4.7.
 */
@Entity
@Table(name = "diagnosticos_clinicos")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_id", nullable = false)
    private HistoriaClinica historia;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Región: "FDI 26", "Cuadrante sup. derecho", "General"... */
    @Column(length = 60)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeveridadDiagnostico severidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDiagnostico estado;

    @Column(nullable = false)
    private LocalDate fecha;

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
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        if (severidad == null) {
            severidad = SeveridadDiagnostico.MODERADA;
        }
        if (estado == null) {
            estado = EstadoDiagnostico.CONFIRMADO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

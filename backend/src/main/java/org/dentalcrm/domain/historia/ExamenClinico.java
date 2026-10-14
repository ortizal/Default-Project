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
 * Examen clínico estructurado por secciones: general, extraoral e
 * intraoral (plan §4.4). Sin campos libres mezclados: cada hallazgo
 * vive en su propia columna.
 */
@Entity
@Table(name = "examenes_clinicos")
@Getter
@Setter
@NoArgsConstructor
public class ExamenClinico {

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
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(columnDefinition = "text")
    private String general;

    @Column(name = "extraoral_atm", length = 255)
    private String extraoralAtm;

    @Column(name = "extraoral_simetria", length = 255)
    private String extraoralSimetria;

    @Column(name = "extraoral_ganglios", length = 255)
    private String extraoralGanglios;

    @Column(name = "extraoral_apertura", length = 255)
    private String extraoralApertura;

    @Column(name = "intraoral_mucosas", length = 255)
    private String intraoralMucosas;

    @Column(name = "intraoral_lengua", length = 255)
    private String intraoralLengua;

    @Column(name = "intraoral_paladar", length = 255)
    private String intraoralPaladar;

    @Column(name = "intraoral_encias", length = 255)
    private String intraoralEncias;

    @Column(name = "intraoral_higiene", length = 255)
    private String intraoralHigiene;

    @Column(name = "intraoral_oclusion", length = 255)
    private String intraoralOclusion;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

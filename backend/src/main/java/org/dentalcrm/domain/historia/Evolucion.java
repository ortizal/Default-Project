package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Evolución (nota clínica) con campos SOAP opcionales (plan §4.10).
 * Una nota cerrada no se edita: las correcciones van en adendas.
 */
@Entity
@Table(name = "evoluciones")
@Getter
@Setter
@NoArgsConstructor
public class Evolucion {

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
    private Instant fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(columnDefinition = "text")
    private String hallazgos;

    @Column(columnDefinition = "text")
    private String diagnostico;

    @Column(columnDefinition = "text")
    private String procedimiento;

    @Column(length = 155)
    private String anestesia;

    @Column(length = 255)
    private String materiales;

    @Column(columnDefinition = "text")
    private String indicaciones;

    @Column(length = 255)
    private String conducta;

    @Column(name = "soap_subjetivo", columnDefinition = "text")
    private String soapSubjetivo;

    @Column(name = "soap_objetivo", columnDefinition = "text")
    private String soapObjetivo;

    @Column(name = "soap_analisis", columnDefinition = "text")
    private String soapAnalisis;

    @Column(name = "soap_plan", columnDefinition = "text")
    private String soapPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvolucion estado;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Column(name = "cerrada_por")
    private Long cerradaPor;

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
            fecha = now;
        }
        if (estado == null) {
            estado = EstadoEvolucion.ABIERTA;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

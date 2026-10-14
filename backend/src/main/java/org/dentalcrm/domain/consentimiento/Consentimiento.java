package org.dentalcrm.domain.consentimiento;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Consentimiento informado de un paciente (plan §4.13): conserva el
 * snapshot del texto, la firma del paciente/tutor, el hash SHA-256 y el
 * PDF. Una vez FIRMADO ya no admite cambios.
 */
@Entity
@Table(name = "consentimientos")
@Getter
@Setter
@NoArgsConstructor
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "plantilla_codigo", nullable = false, length = 40)
    private String plantillaCodigo;

    @Column(name = "plantilla_version", nullable = false)
    private int plantillaVersion;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 60)
    private String categoria;

    /** Texto congelado al crear el consentimiento. */
    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "tutor_nombre", length = 150)
    private String tutorNombre;

    /** Referencia débil al tutor del paciente (V35): el nombre, el parentesco
     *  y el teléfono se copian aquí porque el paciente puede reemplazarlos. */
    @Column(name = "tutor_id")
    private Long tutorId;

    @Column(name = "tutor_parentesco", length = 20)
    private String tutorParentesco;

    @Column(name = "tutor_telefono", length = 30)
    private String tutorTelefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConsentimiento estado = EstadoConsentimiento.BORRADOR;

    @Column(length = 150)
    private String firmante;

    /** Firma manuscrita como data URL (opcional). */
    @Column(name = "firma_imagen", columnDefinition = "text")
    private String firmaImagen;

    @Column(name = "firma_hash", length = 64)
    private String firmaHash;

    @Column(name = "firmado_en")
    private Instant firmadoEn;

    @Column(length = 500)
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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

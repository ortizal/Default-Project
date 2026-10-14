package org.dentalcrm.domain.receta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Receta médica de un paciente (plan §4.12), con historial y anulación motivada. */
@Entity
@Table(name = "recetas")
@Getter
@Setter
@NoArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 255)
    private String diagnostico;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReceta estado = EstadoReceta.VIGENTE;

    @Column(name = "motivo_anulacion", length = 255)
    private String motivoAnulacion;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("numero ASC")
    private List<RecetaLinea> lineas = new ArrayList<>();

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

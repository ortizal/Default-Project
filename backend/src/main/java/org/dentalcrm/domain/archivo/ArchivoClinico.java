package org.dentalcrm.domain.archivo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Archivo clínico del paciente (plan §4.15). La metadata vive en
 * PostgreSQL y el contenido en el {@code FileStorageProvider}: aquí sólo
 * se guarda la {@code clave} relativa, nunca bytes ni rutas físicas.
 */
@Entity
@Table(name = "archivos_clinicos")
@Getter
@Setter
@NoArgsConstructor
public class ArchivoClinico {

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
    @Column(nullable = false, length = 30)
    private CategoriaArchivo categoria;

    /** Nombre original del archivo (como lo eligió quien lo subió). */
    @Column(nullable = false, length = 255)
    private String nombre;

    /** Clave relativa dentro del almacenamiento (nunca una ruta física). */
    @Column(nullable = false, length = 500)
    private String clave;

    @Column(nullable = false, length = 100)
    private String mime;

    @Column(nullable = false)
    private Long tamano;

    @Column(length = 64)
    private String sha256;

    /** Fecha del estudio (puede diferir del día en que se subió). */
    private LocalDate fecha;

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

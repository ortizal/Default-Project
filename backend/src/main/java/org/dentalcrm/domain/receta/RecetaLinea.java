package org.dentalcrm.domain.receta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/** Una línea de la receta: medicamento y su pauta completa (§4.12). */
@Entity
@Table(name = "receta_lineas")
@Getter
@Setter
@NoArgsConstructor
public class RecetaLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, length = 150)
    private String medicamento;

    @Column(length = 100)
    private String concentracion;

    @Column(length = 100)
    private String presentacion;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ViaAdministracion via;

    @Column(length = 100)
    private String frecuencia;

    @Column(length = 100)
    private String duracion;

    @Column(length = 500)
    private String instrucciones;

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

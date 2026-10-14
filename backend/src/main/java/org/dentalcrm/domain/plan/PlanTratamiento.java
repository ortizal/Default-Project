package org.dentalcrm.domain.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan de tratamiento de un paciente: cabecera con estado y totales;
 * las líneas viven en {@link PlanLinea} y los presupuestos en
 * {@link Presupuesto}.
 */
@Entity
@Table(name = "planes_tratamiento")
@Getter
@Setter
@NoArgsConstructor
public class PlanTratamiento {

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

    @Column(nullable = false, length = 150)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPlanTratamiento estado;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 255)
    private String comentario;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<PlanLinea> lineas = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (estado == null) {
            estado = EstadoPlanTratamiento.BORRADOR;
        }
        if (fecha == null) {
            fecha = LocalDate.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

package org.dentalcrm.domain.historia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Evento de cambio dentro de una versión de odontograma: quién agregó,
 * modificó o eliminó una condición y cuándo (historial por fecha).
 */
@Entity
@Table(name = "odontograma_eventos")
@Getter
@Setter
@NoArgsConstructor
public class OdontogramaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontograma_id", nullable = false)
    private Odontograma odontograma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionOdontograma accion;

    @Column
    private Integer diente;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SuperficieDental superficie;

    @Column(length = 40)
    private String condicion;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 20)
    private String estadoNuevo;

    @Column(length = 255)
    private String detalle;

    @Column(name = "autor_id")
    private Long autorId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (fecha == null) {
            fecha = createdAt;
        }
    }
}

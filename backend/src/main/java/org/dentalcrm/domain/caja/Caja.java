package org.dentalcrm.domain.caja;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Sesión de caja (plan §3.4): apertura con fondo inicial, movimientos
 * de ingreso/egreso por forma de pago y cierre con monto declarado y
 * conciliación. Sólo hay una sesión ABIERTA por tenant.
 */
@Entity
@Table(name = "cajas")
@Getter
@Setter
@NoArgsConstructor
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 80)
    private String nombre = "Caja";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    /** Fondo inicial (siempre en efectivo). */
    @Column(name = "monto_apertura", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoApertura = BigDecimal.ZERO;

    /** Efectivo contado al cerrar. */
    @Column(name = "monto_declarado", precision = 14, scale = 2)
    private BigDecimal montoDeclarado;

    /** declarado − esperado (efectivo). */
    @Column(precision = 14, scale = 2)
    private BigDecimal diferencia;

    /** Usuario responsable de la sesión. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private org.dentalcrm.domain.usuario.Usuario usuario;

    @Column(name = "abierto_por")
    private Long abiertoPor;

    @Column(name = "cerrado_por")
    private Long cerradoPor;

    @Column(name = "abierto_en")
    private Instant abiertoEn;

    @Column(name = "cerrado_en")
    private Instant cerradoEn;

    @Column(name = "observaciones_apertura", length = 500)
    private String observacionesApertura;

    @Column(name = "observaciones_cierre", length = 500)
    private String observacionesCierre;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (abiertoEn == null) {
            abiertoEn = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

package org.dentalcrm.domain.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Enlace entre la clave de idempotencia de una reserva web (calendario
 * público) y la cita que ya se creó con esa clave — plan §15.
 *
 * <p>La segunda entrega de la misma clave reentrega la cita existente
 * (estado {@code REPETIDA}) en lugar de crear otra. El índice único
 * {@code (tenant_id, idempotency_key)} es la garantía de que dos envíos
 * concurrentes no dupliquen el cupo: sólo un INSERT gana y el otro cae en
 * el camino de repetición.
 */
@Entity
@Table(name = "reserva_web_idempotencia",
        uniqueConstraints = @UniqueConstraint(name = "uq_reserva_web_idem",
                columnNames = {"tenant_id", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class ReservaWebIdempotencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    /** Clave opaca reutilizada por el navegador ante un reintento. */
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /** La cita ya creada (o que existía) para esta clave. */
    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

package org.dentalcrm.domain.horario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Día en que un odontólogo no sigue su horario semanal: la clínica está
 * cerrada ({@code CERRADO}) o atiende en una ventana propia
 * ({@code HORARIO_ESPECIAL}).
 */
@Entity
@Table(name = "excepciones_horario",
        uniqueConstraints = @UniqueConstraint(name = "uq_excepcion_odontologo_fecha",
                columnNames = {"odontologo_id", "fecha"}))
@Getter
@Setter
@NoArgsConstructor
public class ExcepcionHorario {

    public static final String TIPO_CERRADO = "CERRADO";
    public static final String TIPO_HORARIO_ESPECIAL = "HORARIO_ESPECIAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(length = 255)
    private String motivo;

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

    public boolean esCerrado() {
        return TIPO_CERRADO.equals(tipo);
    }

    /** Cerrado no admite nada; en ventana especial el lapso debe caber entero. */
    public boolean admite(LocalTime horaInicio, LocalTime horaFin) {
        if (esCerrado()) {
            return false;
        }
        return !this.horaInicio.isAfter(horaInicio) && !this.horaFin.isBefore(horaFin);
    }

    /** Una cita queda afectada si no cabe entera dentro de la ventana especial. */
    public boolean afectaA(LocalTime horaInicio, LocalTime horaFin) {
        return !admite(horaInicio, horaFin);
    }
}

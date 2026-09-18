package org.dentalcrm.domain.cita;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.servicio.Servicio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Odontologo doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Column(nullable = false)
    private Boolean confirmada;

    @Column(name = "confirmada_at")
    private Instant confirmadaAt;

    @Column(name = "confirmation_source", length = 20)
    private String confirmationSource;

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Column(name = "google_calendar_id", length = 255)
    private String googleCalendarId;

    @Column(name = "google_event_id_doctor", length = 255)
    private String googleEventIdDoctor;

    @Column(name = "google_calendar_id_doctor", length = 255)
    private String googleCalendarIdDoctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncEstado syncStatus = SyncEstado.NO_SYNC;

    @Column(name = "sync_error", length = 500)
    private String syncError;

    @Column(name = "sync_attempts", nullable = false)
    private Integer syncAttempts = 0;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "cancelada_at")
    private Instant canceladaAt;

    @Column(name = "cancelada_motivo", length = 255)
    private String canceladaMotivo;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "created_by")
    private Long createdBy;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (estado == null) {
            estado = EstadoCita.PENDIENTE;
        }
        if (confirmada == null) {
            confirmada = false;
        }
        if (syncStatus == null) {
            syncStatus = SyncEstado.NO_SYNC;
        }
        if (syncAttempts == null) {
            syncAttempts = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
package org.dentalcrm.domain.google;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "google_calendars")
@Getter
@Setter
@NoArgsConstructor
public class GoogleCalendario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "google_account_id")
    private GoogleAccount cuenta;

    @Column(name = "calendar_id", nullable = false, length = 255)
    private String calendarId;

    @Column(length = 255)
    private String summary;

    @Column(name = "time_zone", length = 64)
    private String timeZone;

    @Column(name = "es_seleccionado", nullable = false)
    private Boolean seleccionado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (seleccionado == null) {
            seleccionado = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
package org.dentalcrm.domain.whatsapp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "whatsapp_sesiones")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "sesion_id", nullable = false, unique = true, length = 100)
    private String sesionId;

    @Column(length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesionWhatsapp estado = EstadoSesionWhatsapp.DESCONECTADA;

    @Column(name = "estado_detalle", columnDefinition = "text")
    private String estadoDetalle;

    @Column(columnDefinition = "text")
    private String qr;

    @Column(name = "last_error", length = 500)
    private String lastError;

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
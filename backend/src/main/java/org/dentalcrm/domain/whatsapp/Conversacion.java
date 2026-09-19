package org.dentalcrm.domain.whatsapp;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.paciente.Paciente;
import org.hibernate.annotations.Type;

import java.time.Instant;

@Entity
@Table(name = "conversaciones")
@Getter
@Setter
@NoArgsConstructor
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = true)
    private WhatsappSesion sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(name = "nombre_contacto", length = 150)
    private String nombreContacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConversacion estado = EstadoConversacion.BOT;

    @Column(name = "ultimo_mensaje_at")
    private Instant ultimoMensajeAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Intencion intencion;

    @Type(JsonBinaryType.class)
    @Column(name = "contexto_agente", columnDefinition = "jsonb")
    private String contextoAgente;

    @Column(name = "agente_activo", nullable = false)
    private Boolean agenteActivo = true;

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
package org.dentalcrm.domain.configuracion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

@Entity
@Table(name = "configuracion_integraciones")
@Getter
@Setter
@NoArgsConstructor
public class IntegracionConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "openwa_url", length = 500)
    private String openwaUrl;

    @Column(name = "openwa_api_key", columnDefinition = "text")
    private String openwaApiKey;

    @Column(name = "openwa_webhook_url", length = 1000)
    private String openwaWebhookUrl;

    @Column(name = "openwa_webhook_secret", columnDefinition = "text")
    private String openwaWebhookSecret;

    @Column(name = "mail_host", length = 255)
    private String mailHost;

    @Column(name = "mail_port")
    private Integer mailPort;

    @Column(name = "mail_username", length = 255)
    private String mailUsername;

    @Column(name = "mail_password", columnDefinition = "text")
    private String mailPassword;

    @Column(name = "mail_from", length = 255)
    private String mailFrom;

    @Column(name = "meta_client_id", length = 255)
    private String metaClientId;

    @Column(name = "meta_client_secret", columnDefinition = "text")
    private String metaClientSecret;

    @Column(name = "meta_redirect_uri", length = 1000)
    private String metaRedirectUri;

    @Column(name = "tiktok_client_key", length = 255)
    private String tiktokClientKey;

    @Column(name = "tiktok_client_secret", columnDefinition = "text")
    private String tiktokClientSecret;

    @Column(name = "tiktok_redirect_uri", length = 1000)
    private String tiktokRedirectUri;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        updatedAt = Instant.now();
    }
}
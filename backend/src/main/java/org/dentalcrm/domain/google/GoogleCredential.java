package org.dentalcrm.domain.google;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "google_credentials")
@Getter
@Setter
@NoArgsConstructor
public class GoogleCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    @Column(name = "client_secret", nullable = false, columnDefinition = "text")
    private String clientSecret;

    @Column(name = "redirect_uri", length = 255)
    private String redirectUri;

    @Column(name = "auth_base_url", length = 255)
    private String authBaseUrl;

    @Column(name = "oauth_base_url", length = 255)
    private String oauthBaseUrl;

    @Column(name = "api_base_url", length = 255)
    private String apiBaseUrl;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

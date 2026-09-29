package org.dentalcrm.domain.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "social_oauth_requests")
@Getter
@Setter
@NoArgsConstructor
public class SocialOAuthRequest {

    @Id
    @Column(length = 64)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
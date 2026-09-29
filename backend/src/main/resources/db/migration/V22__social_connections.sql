CREATE TABLE social_accounts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    platform VARCHAR(20) NOT NULL,
    external_account_id VARCHAR(255) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMPTZ,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_social_account_tenant_platform_external UNIQUE (tenant_id, platform, external_account_id)
);

CREATE INDEX idx_social_accounts_tenant_platform ON social_accounts (tenant_id, platform);

CREATE TABLE social_oauth_requests (
    state VARCHAR(64) PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    tenant_id BIGINT NOT NULL REFERENCES tenants (id),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_social_oauth_requests_expires_at ON social_oauth_requests (expires_at);

INSERT INTO permisos (codigo, nombre)
VALUES ('REDES_SOCIALES', 'Administrar redes sociales')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'REDES_SOCIALES'
WHERE r.codigo IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
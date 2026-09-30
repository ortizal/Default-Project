CREATE TABLE configuracion_integraciones (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants (id) ON DELETE CASCADE,
    openwa_url VARCHAR(500),
    openwa_api_key TEXT,
    openwa_webhook_url VARCHAR(1000),
    openwa_webhook_secret TEXT,
    mail_host VARCHAR(255),
    mail_port INTEGER,
    mail_username VARCHAR(255),
    mail_password TEXT,
    mail_from VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
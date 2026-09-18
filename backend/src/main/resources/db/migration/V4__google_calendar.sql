-- =====================================================================
-- Fase 4 - Integración Google Calendar (OAuth 2.0)
-- =====================================================================

CREATE TABLE google_accounts (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    access_token  TEXT         NOT NULL,
    refresh_token TEXT,
    expires_at    TIMESTAMPTZ,
    scope         TEXT,
    connected_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE google_accounts IS 'Cuenta Google conectada (tokens OAuth, nunca contraseñas)';

CREATE TABLE google_calendars (
    id                 BIGSERIAL PRIMARY KEY,
    google_account_id  BIGINT NOT NULL REFERENCES google_accounts(id) ON DELETE CASCADE,
    calendar_id        VARCHAR(255) NOT NULL,
    summary            VARCHAR(255),
    time_zone          VARCHAR(64),
    es_seleccionado    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (google_account_id, calendar_id)
);

CREATE INDEX idx_google_calendars_cuenta ON google_calendars (google_account_id);

-- Solo un calendario seleccionado por cuenta
CREATE UNIQUE INDEX uq_google_calendars_seleccionado
    ON google_calendars (google_account_id) WHERE es_seleccionado;

-- Estado de sincronización de la cita en Google Calendar
ALTER TABLE citas
    ADD COLUMN sync_status      VARCHAR(20) NOT NULL DEFAULT 'NO_SYNC',
    ADD COLUMN sync_error       VARCHAR(500),
    ADD COLUMN sync_attempts    INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_at     TIMESTAMPTZ;

COMMENT ON COLUMN citas.sync_status IS 'NO_SYNC|PENDING|SYNCED|ERROR';

-- ADMIN gestiona integraciones (SUPER_ADMIN ya tiene todos)
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'INTEGRACIONES'
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;
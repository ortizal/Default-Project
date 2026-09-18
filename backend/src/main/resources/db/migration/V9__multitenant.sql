-- =====================================================================
-- Fase 9 - Multi-tenant: aislamiento de datos por clínica (tenant)
-- =====================================================================

-- Catálogo de tenants. La fila 'principal' (id = 1) es el arrendatario
-- por defecto; el resto de filas existentes se asignan a él en el ALTER.
CREATE TABLE IF NOT EXISTS tenants (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(150) NOT NULL UNIQUE,
    estado     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO tenants (id, nombre, estado)
VALUES (1, 'principal', 'ACTIVO')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('tenants', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM tenants), 1));

-- tenant_id en todas las tablas de negocio. El DEFAULT 1 reasigna las filas
-- existentes al tenant 'principal'; los INSERTs vía JPA lo fijan explícitamente.
ALTER TABLE usuarios            ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE pacientes           ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE odontologos         ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE servicios           ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE horarios_odontologos ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE bloqueos_agenda     ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE citas               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE auditoria           ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE google_accounts     ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE google_calendars    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE whatsapp_sesiones   ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE conversaciones      ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE mensajes            ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE plantillas_mensajes ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE automatizaciones    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);
ALTER TABLE notificaciones      ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id);

-- Índices de apoyo para el aislamiento por tenant en las consultas más usadas
CREATE INDEX IF NOT EXISTS idx_usuarios_tenant             ON usuarios (tenant_id);
CREATE INDEX IF NOT EXISTS idx_pacientes_tenant            ON pacientes (tenant_id);
CREATE INDEX IF NOT EXISTS idx_odontologos_tenant          ON odontologos (tenant_id);
CREATE INDEX IF NOT EXISTS idx_servicios_tenant            ON servicios (tenant_id);
CREATE INDEX IF NOT EXISTS idx_citas_tenant_fecha          ON citas (tenant_id, fecha);
CREATE INDEX IF NOT EXISTS idx_conversaciones_tenant       ON conversaciones (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mensajes_tenant             ON mensajes (tenant_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_sesiones_tenant    ON whatsapp_sesiones (tenant_id);
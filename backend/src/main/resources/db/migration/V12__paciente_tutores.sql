-- =====================================================================
-- Tutores de pacientes (padre/madre) obligatorios para menores de edad
-- =====================================================================

CREATE TABLE paciente_tutores (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id),
    paciente_id BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    parentesco  VARCHAR(20) NOT NULL CHECK (parentesco IN ('PADRE','MADRE')),
    nombres     VARCHAR(150) NOT NULL,
    apellidos   VARCHAR(150),
    telefono    VARCHAR(30),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_paciente_tutores_paciente ON paciente_tutores (paciente_id, tenant_id);
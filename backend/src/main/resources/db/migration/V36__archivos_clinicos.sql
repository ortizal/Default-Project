-- =====================================================================
-- V36: Archivos clínicos del paciente — Fase E
-- ---------------------------------------------------------------------
-- Fase E del plan de ampliación (§4.15): radiografías, fotografías,
-- panorámicas, tomografías, resultados y documentos del paciente.
-- Sólo se persiste la METADATA en PostgreSQL; los bytes viven fuera de la
-- BD en el FileStorageProvider (local hoy, S3 mañana) para no inflar los
-- respaldos. El nombre de archivo original se conserva para la descarga
-- y se guarda la huella SHA-256 del contenido para detectar alteraciones.
-- =====================================================================

CREATE TABLE archivos_clinicos (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    paciente_id    BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    categoria      VARCHAR(30) NOT NULL
                   CHECK (categoria IN ('RADIOGRAFIA', 'FOTOGRAFIA', 'PANORAMICA',
                                        'TOMOGRAFIA', 'RESULTADO', 'DOCUMENTO')),
    nombre         VARCHAR(255) NOT NULL,
    clave          VARCHAR(500) NOT NULL,
    mime           VARCHAR(100) NOT NULL,
    tamano         BIGINT NOT NULL CHECK (tamano > 0),
    sha256         VARCHAR(64),
    fecha          DATE,
    observaciones  VARCHAR(500),
    created_by     BIGINT REFERENCES usuarios (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_archivos_clinicos_paciente
    ON archivos_clinicos (paciente_id, fecha DESC NULLS LAST, id DESC);
CREATE INDEX idx_archivos_clinicos_tenant ON archivos_clinicos (tenant_id);

-- =====================================================================
-- V29: Odontograma interactivo — Fase C
-- ---------------------------------------------------------------------
-- Fase C del plan de ampliación (§4.5): dentición permanente/temporal/
-- mixta, numeración FDI, condiciones por diente y superficie con estados
-- (existente, diagnóstico, planificado, en proceso, completado,
-- referido), versionado por fecha para comparación temporal y un log de
-- eventos de cambio. **No se guarda ninguna imagen**: todo son datos
-- estructurados y el dibujo SVG se genera en el cliente.
-- =====================================================================

CREATE TABLE odontogramas (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenants (id),
    historia_id   BIGINT NOT NULL REFERENCES historias_clinicas (id) ON DELETE CASCADE,
    version       INT NOT NULL,
    fecha         DATE NOT NULL DEFAULT CURRENT_DATE,
    denticion     VARCHAR(20) NOT NULL DEFAULT 'PERMANENTE'
                  CHECK (denticion IN ('PERMANENTE', 'TEMPORAL', 'MIXTA')),
    odontologo_id BIGINT REFERENCES odontologos (id),
    observaciones TEXT,
    created_by    BIGINT REFERENCES usuarios (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_odontograma_version UNIQUE (historia_id, version)
);

CREATE INDEX idx_odontogramas_historia ON odontogramas (historia_id, version DESC);

-- Una fila por (diente FDI, superficie, condición). Superficie NULL =
-- diente completo (p. ej. "ausente"). Las condiciones son catálogo
-- configurable: aquí sólo se guarda su clave.
CREATE TABLE odontograma_dientes (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenants (id),
    odontograma_id BIGINT NOT NULL REFERENCES odontogramas (id) ON DELETE CASCADE,
    diente        INT NOT NULL CHECK (diente >= 11 AND diente <= 85),
    superficie    VARCHAR(20)
                  CHECK (superficie IS NULL OR superficie IN
                         ('OCLUSAL', 'MESIAL', 'DISTAL', 'VESTIBULAR', 'LINGUAL', 'INCISAL')),
    condicion     VARCHAR(40) NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'EXISTENTE'
                  CHECK (estado IN ('EXISTENTE', 'DIAGNOSTICO', 'PLANIFICADO',
                                    'PROCESO', 'COMPLETADO', 'REFERIDO')),
    notas         VARCHAR(255),
    odontologo_id BIGINT REFERENCES odontologos (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Misma clave lógica que la de negocio: nada de duplicados en una versión.
CREATE UNIQUE INDEX uq_odontograma_diente
    ON odontograma_dientes (odontograma_id, diente, COALESCE(superficie, '~'), condicion);
CREATE INDEX idx_odontograma_dientes
    ON odontograma_dientes (odontograma_id, diente);

-- Quién cambió qué y cuándo dentro de una versión (auditoría clínica).
CREATE TABLE odontograma_eventos (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    odontograma_id BIGINT NOT NULL REFERENCES odontogramas (id) ON DELETE CASCADE,
    accion         VARCHAR(20) NOT NULL
                   CHECK (accion IN ('CREAR_VERSION', 'AGREGAR', 'MODIFICAR', 'ELIMINAR')),
    diente         INT,
    superficie     VARCHAR(20),
    condicion      VARCHAR(40),
    estado_anterior VARCHAR(20),
    estado_nuevo   VARCHAR(20),
    detalle        VARCHAR(255),
    autor_id       BIGINT REFERENCES usuarios (id),
    fecha          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_odontograma_eventos ON odontograma_eventos (odontograma_id, fecha DESC);

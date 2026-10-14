-- =====================================================================
-- V31: Periodontograma — Fase E
-- ---------------------------------------------------------------------
-- Fase E del plan de ampliación (§4.11): examen periodontal por versión,
-- mediciones por diente y sitio (sondaje, margen, inserción, sangrado,
-- placa y supuración), movilidad y furcación por pieza, comparación
-- temporal entre exámenes y reporte periodontal. Todo son datos
-- estructurados; el reporte se genera en el servidor con OpenPDF.
-- =====================================================================

CREATE TABLE examenes_periodontales (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenants (id),
    historia_id   BIGINT NOT NULL REFERENCES historias_clinicas (id) ON DELETE CASCADE,
    version       INT NOT NULL,
    fecha         DATE NOT NULL DEFAULT CURRENT_DATE,
    odontologo_id BIGINT REFERENCES odontologos (id),
    observaciones TEXT,
    created_by    BIGINT REFERENCES usuarios (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_examen_periodontal_version UNIQUE (historia_id, version)
);

CREATE INDEX idx_examenes_periodontales_historia
    ON examenes_periodontales (historia_id, version DESC);

-- Una fila por (diente FDI, sitio). Los valores son milímetros y los
-- signos clínicos (sangrado, placa, supuración) son binarios.
CREATE TABLE periodontograma_sitios (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants (id),
    examen_id   BIGINT NOT NULL REFERENCES examenes_periodontales (id) ON DELETE CASCADE,
    diente      INT NOT NULL CHECK (diente >= 11 AND diente <= 85),
    sitio       VARCHAR(20) NOT NULL
                CHECK (sitio IN ('VESTIBULAR', 'MESIAL', 'DISTAL', 'LINGUAL')),
    sondaje_mm  NUMERIC(4, 1)
                CHECK (sondaje_mm IS NULL OR (sondaje_mm >= 0 AND sondaje_mm <= 20)),
    margen_mm   NUMERIC(4, 1)
                CHECK (margen_mm IS NULL OR (margen_mm >= -5 AND margen_mm <= 10)),
    insercion_mm NUMERIC(4, 1)
                CHECK (insercion_mm IS NULL OR (insercion_mm >= 0 AND insercion_mm <= 20)),
    sangrado    BOOLEAN NOT NULL DEFAULT FALSE,
    placa       BOOLEAN NOT NULL DEFAULT FALSE,
    supuracion  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodonto_sitio UNIQUE (examen_id, diente, sitio)
);

CREATE INDEX idx_periodonto_sitios_examen
    ON periodontograma_sitios (examen_id, diente);

-- Movilidad y furcación se registran por pieza (no por sitio).
CREATE TABLE periodontograma_dientes (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    examen_id  BIGINT NOT NULL REFERENCES examenes_periodontales (id) ON DELETE CASCADE,
    diente     INT NOT NULL CHECK (diente >= 11 AND diente <= 85),
    movilidad  INT CHECK (movilidad IS NULL OR movilidad IN (0, 1, 2, 3)),
    furcacion  INT CHECK (furcacion IS NULL OR furcacion IN (0, 1, 2, 3)),
    notas      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodonto_diente UNIQUE (examen_id, diente)
);

CREATE INDEX idx_periodonto_dientes_examen
    ON periodontograma_dientes (examen_id, diente);

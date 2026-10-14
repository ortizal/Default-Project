-- =====================================================================
-- V33: Recetas médicas — Fase E
-- ---------------------------------------------------------------------
-- Fase E del plan de ampliación (§4.12): receta por paciente con
-- profesional, diagnóstico e historial; cada línea concentra medicamento,
-- concentración, presentación, dosis, vía, frecuencia, duración e
-- instrucciones. La anulación queda motivada y auditada en servidor.
-- =====================================================================

CREATE TABLE recetas (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL REFERENCES tenants (id),
    paciente_id       BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    odontologo_id     BIGINT REFERENCES odontologos (id),
    fecha             DATE NOT NULL DEFAULT CURRENT_DATE,
    diagnostico       VARCHAR(255),
    observaciones     TEXT,
    estado            VARCHAR(20) NOT NULL DEFAULT 'VIGENTE'
                      CHECK (estado IN ('VIGENTE', 'ANULADA')),
    motivo_anulacion  VARCHAR(255),
    created_by        BIGINT REFERENCES usuarios (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recetas_paciente ON recetas (paciente_id, fecha DESC);

CREATE TABLE receta_lineas (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    receta_id      BIGINT NOT NULL REFERENCES recetas (id) ON DELETE CASCADE,
    numero         INT NOT NULL,
    medicamento    VARCHAR(150) NOT NULL,
    concentracion  VARCHAR(100),
    presentacion   VARCHAR(100),
    dosis          VARCHAR(100) NOT NULL,
    via            VARCHAR(30)
                   CHECK (via IS NULL OR via IN ('ORAL', 'SUBLINGUAL', 'TOPICA', 'INTRAMUSCULAR',
                                                 'INTRAVENOSA', 'RECTAL', 'OFTALMICA', 'OTICA',
                                                 'INHALATORIA')),
    frecuencia     VARCHAR(100),
    duracion       VARCHAR(100),
    instrucciones  VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_receta_linea_numero UNIQUE (receta_id, numero)
);

CREATE INDEX idx_receta_lineas_receta ON receta_lineas (receta_id, numero);

-- ---------------------------------------------------------------------
-- RECETAS_READ para RECEPCION (Fase E, §4.12)
-- ---------------------------------------------------------------------
-- Coherente con HISTORIA_CLINICA_READ y ODONTOGRAMA_READ que ya usa la
-- recepción (V27): puede consultar e imprimir una receta en el mostrador,
-- pero no emite ni anula (no recibe RECETAS_WRITE).
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'RECETAS_READ'
WHERE r.codigo = 'RECEPCION'
ON CONFLICT DO NOTHING;

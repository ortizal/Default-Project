-- =====================================================================
-- V28: Historia clínica odontológica — base (Fase B)
-- ---------------------------------------------------------------------
-- Fase B del plan de ampliación (FACTURACIÓN ELECTRÓNICA + HISTORIA
-- CLÍNICA ODONTOLÓGICA). Una historia por paciente con antecedentes
-- estructurados (no texto libre), exámenes clínicos por secciones,
-- diagnósticos con historial de estados y evoluciones cerrables con
-- adendas auditadas. Odontograma, planes, recetas y periodoncia
-- llegan en las fases C-E sobre estas tablas.
-- =====================================================================

-- Una (y sólo una) historia por paciente dentro del tenant.
CREATE TABLE historias_clinicas (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    paciente_id    BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA'
                   CHECK (estado IN ('ABIERTA', 'CERRADA')),
    odontologo_id  BIGINT REFERENCES odontologos (id),
    motivo_cierre  VARCHAR(255),
    cerrada_en     TIMESTAMPTZ,
    cerrada_por    BIGINT REFERENCES usuarios (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_historias_tenant_paciente UNIQUE (tenant_id, paciente_id)
);

CREATE INDEX idx_historias_paciente ON historias_clinicas (tenant_id, paciente_id);

-- Antecedentes estructurados: cada dato en su fila y su tipo, en lugar
-- de un párrafo libre (plan §4.3).
CREATE TABLE antecedentes_medicos (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenants (id),
    historia_id   BIGINT NOT NULL REFERENCES historias_clinicas (id) ON DELETE CASCADE,
    tipo          VARCHAR(30) NOT NULL
                  CHECK (tipo IN ('ENFERMEDAD', 'CIRUGIA', 'HOSPITALIZACION',
                                  'ALERGIA', 'MEDICACION', 'ANTECEDENTE_ODONTOLOGICO', 'HABITO')),
    nombre        VARCHAR(150) NOT NULL,
    detalle       VARCHAR(255),
    fecha_desde   DATE,
    estado        VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
                  CHECK (estado IN ('ACTIVO', 'CONTROLADO', 'RESUELTO', 'INACTIVO')),
    observaciones TEXT,
    created_by    BIGINT REFERENCES usuarios (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_antecedentes_historia ON antecedentes_medicos (historia_id, tipo);

-- Examen clínico: general / extraoral / intraoral en campos propios
-- (plan §4.4); la vista se compone desde el cliente.
CREATE TABLE examenes_clinicos (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT NOT NULL REFERENCES tenants (id),
    historia_id          BIGINT NOT NULL REFERENCES historias_clinicas (id) ON DELETE CASCADE,
    fecha                DATE NOT NULL DEFAULT CURRENT_DATE,
    odontologo_id        BIGINT REFERENCES odontologos (id),
    general              TEXT,
    extraoral_atm        VARCHAR(255),
    extraoral_simetria   VARCHAR(255),
    extraoral_ganglios   VARCHAR(255),
    extraoral_apertura   VARCHAR(255),
    intraoral_mucosas    VARCHAR(255),
    intraoral_lengua     VARCHAR(255),
    intraoral_paladar    VARCHAR(255),
    intraoral_encias     VARCHAR(255),
    intraoral_higiene    VARCHAR(255),
    intraoral_oclusion   VARCHAR(255),
    observaciones        TEXT,
    created_by           BIGINT REFERENCES usuarios (id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_examenes_historia ON examenes_clinicos (historia_id, fecha DESC);

-- Diagnósticos: se conserva el historial cambiando `estado`
-- (sospecha → confirmado → resuelto/descartado), plan §4.7.
CREATE TABLE diagnosticos_clinicos (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenants (id),
    historia_id   BIGINT NOT NULL REFERENCES historias_clinicas (id) ON DELETE CASCADE,
    nombre        VARCHAR(150) NOT NULL,
    region        VARCHAR(60),
    severidad     VARCHAR(20) NOT NULL DEFAULT 'MODERADA'
                  CHECK (severidad IN ('LEVE', 'MODERADA', 'SEVERA')),
    estado        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO'
                  CHECK (estado IN ('SOSPECHA', 'CONFIRMADO', 'RESUELTO', 'DESCARTADO')),
    fecha         DATE NOT NULL DEFAULT CURRENT_DATE,
    odontologo_id BIGINT REFERENCES odontologos (id),
    observaciones TEXT,
    created_by    BIGINT REFERENCES usuarios (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_diagnosticos_historia ON diagnosticos_clinicos (historia_id, estado, fecha DESC);

-- Evoluciones (notas clínicas). Una vez cerrada sólo admite adendas.
CREATE TABLE evoluciones (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    historia_id    BIGINT NOT NULL REFERENCES historias_clinicas (id) ON DELETE CASCADE,
    fecha          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    odontologo_id  BIGINT REFERENCES odontologos (id),
    cita_id        BIGINT REFERENCES citas (id),
    motivo         VARCHAR(255) NOT NULL,
    hallazgos      TEXT,
    diagnostico    TEXT,
    procedimiento  TEXT,
    anestesia      VARCHAR(155),
    materiales     VARCHAR(255),
    indicaciones   TEXT,
    conducta       VARCHAR(255),
    soap_subjetivo TEXT,
    soap_objetivo  TEXT,
    soap_analisis  TEXT,
    soap_plan      TEXT,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                   CHECK (estado IN ('ABIERTA', 'CERRADA')),
    cerrada_en     TIMESTAMPTZ,
    cerrada_por    BIGINT REFERENCES usuarios (id),
    created_by     BIGINT REFERENCES usuarios (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evoluciones_historia_fecha ON evoluciones (historia_id, fecha DESC);

-- Corrección de una nota ya cerrada: nunca se edita el original.
CREATE TABLE evolucion_adendas (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL REFERENCES tenants (id),
    evolucion_id BIGINT NOT NULL REFERENCES evoluciones (id) ON DELETE CASCADE,
    texto        TEXT NOT NULL,
    autor_id     BIGINT REFERENCES usuarios (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_adendas_evolucion ON evolucion_adendas (evolucion_id, created_at DESC);

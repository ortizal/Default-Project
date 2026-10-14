-- =====================================================================
-- V30: Planes de tratamiento y presupuestos — Fase D
-- ---------------------------------------------------------------------
-- Fase D del plan de ampliación (§4.8 y §4.9): plan clínico con líneas
-- por procedimiento (diente, superficie, diagnóstico, prioridad,
-- sesiones, precio y descuento), fases, estados del ciclo de vida
-- (borrador → presentado → aceptado/rechazado → en ejecución →
-- completado/cancelado) y presupuestos versionados con snapshot de las
-- líneas, aceptación y firma (hash SHA-256). El presupuesto es un
-- documento separado del plan: Plan → Presupuesto → Aceptación.
-- =====================================================================

CREATE TABLE planes_tratamiento (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants (id),
    paciente_id     BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    odontologo_id   BIGINT REFERENCES odontologos (id),
    titulo          VARCHAR(150) NOT NULL,
    estado          VARCHAR(30) NOT NULL DEFAULT 'BORRADOR'
                    CHECK (estado IN ('BORRADOR', 'PRESENTADO', 'ACEPTADO',
                                      'PARCIALMENTE_ACEPTADO', 'RECHAZADO',
                                      'EN_EJECUCION', 'COMPLETADO', 'CANCELADO')),
    observaciones   TEXT,
    fecha           DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal        NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    total           NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    comentario      VARCHAR(255),
    creado_por      BIGINT REFERENCES usuarios (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_planes_paciente ON planes_tratamiento (tenant_id, paciente_id, id DESC);
CREATE INDEX idx_planes_estado ON planes_tratamiento (tenant_id, estado);

-- Una fila por procedimiento planificado. `numero` da el orden estable
-- dentro del plan; `fase` agrupa (ortodoncia en varias fases, p. ej.).
CREATE TABLE plan_lineas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants (id),
    plan_id         BIGINT NOT NULL REFERENCES planes_tratamiento (id) ON DELETE CASCADE,
    numero          INT NOT NULL,
    fase            INT NOT NULL DEFAULT 1 CHECK (fase >= 1),
    diente          INT CHECK (diente IS NULL OR (diente >= 11 AND diente <= 85)),
    superficie      VARCHAR(20)
                    CHECK (superficie IS NULL OR superficie IN
                           ('OCLUSAL', 'MESIAL', 'DISTAL', 'VESTIBULAR',
                            'LINGUAL', 'INCISAL')),
    diagnostico     VARCHAR(150),
    procedimiento   VARCHAR(150) NOT NULL,
    servicio_id     BIGINT REFERENCES servicios (id),
    prioridad       VARCHAR(10) NOT NULL DEFAULT 'MEDIA'
                    CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'URGENTE')),
    sesiones        INT NOT NULL DEFAULT 1 CHECK (sesiones >= 1),
    precio_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (precio_unitario >= 0),
    descuento_pct   NUMERIC(5, 2) NOT NULL DEFAULT 0
                    CHECK (descuento_pct >= 0 AND descuento_pct <= 100),
    subtotal        NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                    CHECK (estado IN ('PENDIENTE', 'EN_EJECUCION', 'COMPLETADO',
                                      'CANCELADO', 'REFERIDO')),
    notas           VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_plan_linea_numero UNIQUE (plan_id, numero)
);

CREATE INDEX idx_plan_lineas ON plan_lineas (plan_id, numero);
CREATE INDEX idx_plan_lineas_estado ON plan_lineas (plan_id, estado);

-- Presupuestos del plan: cada emisión crea una versión nueva con las
-- líneas copiadas (snapshot inmutable) para poder auditar qué se
-- ofreció y qué se firmó. Sólo uno queda `vigente` por plan.
CREATE TABLE presupuestos (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants (id),
    plan_id         BIGINT NOT NULL REFERENCES planes_tratamiento (id) ON DELETE CASCADE,
    numero          INT NOT NULL,
    estado          VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                    CHECK (estado IN ('BORRADOR', 'PRESENTADO', 'ACEPTADO',
                                      'RECHAZADO', 'ANULADO')),
    fecha           DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente         BOOLEAN NOT NULL DEFAULT FALSE,
    subtotal        NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento       NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    total           NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    observaciones   VARCHAR(255),
    comentario      VARCHAR(255),
    firmante        VARCHAR(120),
    firma_hash      VARCHAR(64),
    aceptado_en     TIMESTAMPTZ,
    creado_por      BIGINT REFERENCES usuarios (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_presupuesto_version UNIQUE (plan_id, numero)
);

CREATE INDEX idx_presupuestos_plan ON presupuestos (plan_id, numero DESC);

-- Snapshot de las líneas del plan al momento de emitir. Si la línea
-- original se elimina (sólo en borrador) el presupuesto conserva sus
-- propios valores.
CREATE TABLE presupuesto_lineas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants (id),
    presupuesto_id  BIGINT NOT NULL REFERENCES presupuestos (id) ON DELETE CASCADE,
    plan_linea_id   BIGINT REFERENCES plan_lineas (id) ON DELETE SET NULL,
    numero          INT NOT NULL,
    fase            INT NOT NULL DEFAULT 1,
    diente          INT,
    superficie      VARCHAR(20),
    diagnostico     VARCHAR(150),
    procedimiento   VARCHAR(150) NOT NULL,
    prioridad       VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    sesiones        INT NOT NULL DEFAULT 1,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    descuento_pct   NUMERIC(5, 2) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_presupuesto_lineas ON presupuesto_lineas (presupuesto_id, numero);

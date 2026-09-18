-- =====================================================================
-- Fase 2 - CRM: pacientes, odontólogos, servicios
-- =====================================================================

CREATE TABLE pacientes (
    id               BIGSERIAL PRIMARY KEY,
    cedula           VARCHAR(20),
    nombres          VARCHAR(150) NOT NULL,
    apellidos        VARCHAR(150) NOT NULL,
    telefono         VARCHAR(30),
    email            VARCHAR(190),
    fecha_nacimiento DATE,
    direccion        VARCHAR(255),
    observaciones    TEXT,
    estado           VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Evitar duplicados por cédula (permitiendo nulos/vacíos)
CREATE UNIQUE INDEX idx_pacientes_cedula_unique
    ON pacientes (cedula) WHERE cedula IS NOT NULL AND cedula <> '';

CREATE INDEX idx_pacientes_nombre    ON pacientes (nombres, apellidos);
CREATE INDEX idx_pacientes_telefono  ON pacientes (telefono);
CREATE INDEX idx_pacientes_estado    ON pacientes (estado);

CREATE TABLE odontologos (
    id            BIGSERIAL PRIMARY KEY,
    nombres       VARCHAR(150) NOT NULL,
    apellidos     VARCHAR(150) NOT NULL,
    especialidad  VARCHAR(100),
    telefono      VARCHAR(30),
    email         VARCHAR(190),
    estado        VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_odontologos_nombre  ON odontologos (nombres, apellidos);
CREATE INDEX idx_odontologos_estado  ON odontologos (estado);

CREATE TABLE servicios (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(150) NOT NULL UNIQUE,
    descripcion      VARCHAR(500),
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos > 0),
    precio           NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (precio >= 0),
    estado           VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_servicios_estado ON servicios (estado);
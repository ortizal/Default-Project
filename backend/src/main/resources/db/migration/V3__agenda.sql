-- =====================================================================
-- Fase 3 - Agenda: horarios, bloqueos, citas
-- =====================================================================

CREATE TABLE horarios_odontologos (
    id                 BIGSERIAL PRIMARY KEY,
    odontologo_id      BIGINT NOT NULL REFERENCES odontologos(id) ON DELETE CASCADE,
    dia_semana         INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1=Lunes ... 7=Domingo
    hora_inicio        TIME NOT NULL,
    hora_fin           TIME NOT NULL,
    intervalo_minutos  INTEGER NOT NULL DEFAULT 30 CHECK (intervalo_minutos >= 5),
    estado             VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_horarios_odontologo ON horarios_odontologos (odontologo_id, dia_semana);

CREATE TABLE bloqueos_agenda (
    id              BIGSERIAL PRIMARY KEY,
    odontologo_id   BIGINT NOT NULL REFERENCES odontologos(id) ON DELETE CASCADE,
    fecha           DATE NOT NULL,
    hora_inicio     TIME,
    hora_fin        TIME,
    motivo          VARCHAR(255),
    created_by      BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bloqueos_odontologo_fecha ON bloqueos_agenda (odontologo_id, fecha);

CREATE TABLE citas (
    id                 BIGSERIAL PRIMARY KEY,
    paciente_id        BIGINT NOT NULL REFERENCES pacientes(id),
    doctor_id          BIGINT NOT NULL REFERENCES odontologos(id),
    servicio_id        BIGINT NOT NULL REFERENCES servicios(id),
    fecha              DATE NOT NULL,
    hora_inicio        TIME NOT NULL,
    hora_fin           TIME NOT NULL,
    estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE/CONFIRMADA/ATENDIDA/CANCELADA/NO_ASISTIO
    confirmada         BOOLEAN NOT NULL DEFAULT FALSE,
    confirmada_at      TIMESTAMPTZ,
    google_event_id    VARCHAR(255),
    google_calendar_id VARCHAR(255),
    cancelada_at       TIMESTAMPTZ,
    cancelada_motivo   VARCHAR(255),
    observaciones      TEXT,
    created_by         BIGINT,
    version            BIGINT  NOT NULL DEFAULT 0, -- bloqueo optimista
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_citas_fecha        ON citas (fecha, doctor_id);
CREATE INDEX idx_citas_doctor       ON citas (doctor_id, fecha);
CREATE INDEX idx_citas_paciente     ON citas (paciente_id, fecha);
CREATE INDEX idx_citas_estado       ON citas (estado, fecha);
CREATE INDEX idx_citas_google_event ON citas (google_event_id);
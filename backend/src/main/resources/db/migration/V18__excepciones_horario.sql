-- Días de excepción de atención por odontólogo: la clínica cierra un día
-- concreto o abre con un horario distinto al semanal. Al crearla se cancelan
-- (y se notifican a los pacientes) las citas activas que queden fuera.
CREATE TABLE excepciones_horario (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL DEFAULT 1 REFERENCES tenants (id),
    odontologo_id  BIGINT NOT NULL REFERENCES odontologos(id) ON DELETE CASCADE,
    fecha          DATE NOT NULL,
    tipo           VARCHAR(20) NOT NULL,
    hora_inicio    TIME,
    hora_fin       TIME,
    motivo         VARCHAR(255),
    created_by     BIGINT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_excepcion_odontologo_fecha UNIQUE (odontologo_id, fecha),
    CONSTRAINT ck_excepcion_tipo CHECK (tipo IN ('CERRADO', 'HORARIO_ESPECIAL')),
    CONSTRAINT ck_excepcion_horas CHECK (
        (tipo = 'CERRADO' AND hora_inicio IS NULL AND hora_fin IS NULL)
        OR
        (tipo = 'HORARIO_ESPECIAL' AND hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_inicio < hora_fin)
    )
);

CREATE INDEX idx_excepciones_fecha ON excepciones_horario (fecha);

COMMENT ON TABLE excepciones_horario IS 'Días de excepción de atención: CERRADO o HORARIO_ESPECIAL';
COMMENT ON COLUMN excepciones_horario.tipo IS 'CERRADO (todo el día) | HORARIO_ESPECIAL (ventana propia)';

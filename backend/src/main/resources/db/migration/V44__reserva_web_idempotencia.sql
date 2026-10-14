-- =====================================================================
-- V44: idempotencia explícita de la reserva web (calendario público, §15)
-- ---------------------------------------------------------------------
-- Hasta ahora la repetición de una reserva se detectaba por la clave
-- natural (paciente + odontólogo + fecha + hora). Con un token compartido
-- o un doble envío del navegador (doble clic, reintento de red), dos
-- peticiones concurrentes pueden pasar la validación a la vez y crear
-- dos citas para el mismo cupo.
--
-- Esta tabla fija la semántica §15: el navegador/envío del paciente
-- lleva un idempotencyKey opaco; la segunda entrega con la misma clave
-- devuelve la cita ya creada (estado REPETIDA) sin crear otra. El índice
-- único por (tenant_id, idempotency_key) garantiza que, aunque dos
-- envíos concurrentes corran a la vez, sólo uno gana el INSERT y el otro
-- cae en el camino de repetición.
--
-- No se toca la tabla `citas`: la cita sigue siendo la fuente de verdad
-- del cupo; esta tabla sólo enlaza una clave de reintento con la cita que
-- ya se creó por esa clave.
-- =====================================================================

CREATE TABLE reserva_web_idempotencia (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT      NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    -- Clave opaca que el navegador/envío reutiliza ante un reintento de la
    -- misma reserva; única dentro del tenant.
    idempotency_key VARCHAR(64) NOT NULL,
    -- La cita que se creó (o ya existía) para esa clave; es el resultado
    -- que se reentrega si vuelve a llegar la misma clave.
    cita_id         BIGINT      NOT NULL REFERENCES citas (id) ON DELETE CASCADE,
    odontologo_id   BIGINT      NOT NULL REFERENCES odontologos (id) ON DELETE CASCADE,
    paciente_id     BIGINT      NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    fecha           DATE        NOT NULL,
    hora_inicio     TIME        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reserva_web_idem UNIQUE (tenant_id, idempotency_key)
);

-- Las búsquedas de "¿ya se reservó con esta clave?" se hacen con el índice
-- único; además queda trazable qué cupo cubrió cada clave.
CREATE INDEX ix_reserva_web_idem_cita ON reserva_web_idempotencia (cita_id);
CREATE INDEX ix_reserva_web_idem_paciente ON reserva_web_idempotencia (paciente_id, fecha);

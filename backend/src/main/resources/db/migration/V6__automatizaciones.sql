-- =====================================================================
-- Fase 6 - Automatizaciones: plantillas, reglas, notificaciones
-- =====================================================================

CREATE TABLE plantillas_mensajes (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    contenido   TEXT NOT NULL,
    activa      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE automatizaciones (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    evento         VARCHAR(30)  NOT NULL,
    minutos_antes  INTEGER      NOT NULL DEFAULT 0,
    plantilla_id   BIGINT NOT NULL REFERENCES plantillas_mensajes(id) ON DELETE RESTRICT,
    condicion      VARCHAR(255),
    activa         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN automatizaciones.evento IS 'CITA_CREADA|CITA_PROXIMA|CITA_CONFIRMADA|CITA_CANCELADA|CITA_ATENDIDA|NO_ASISTIO';
COMMENT ON COLUMN automatizaciones.minutos_antes IS '0 = inmediato; >0 = minutos antes de la cita';

CREATE TABLE notificaciones (
    id                BIGSERIAL PRIMARY KEY,
    cita_id           BIGINT REFERENCES citas(id) ON DELETE CASCADE,
    automatizacion_id BIGINT REFERENCES automatizaciones(id) ON DELETE SET NULL,
    paciente_id       BIGINT REFERENCES pacientes(id) ON DELETE CASCADE,
    telefono          VARCHAR(30),
    plantilla_id      BIGINT REFERENCES plantillas_mensajes(id) ON DELETE SET NULL,
    mensaje_generado  TEXT,
    programada_at     TIMESTAMPTZ NOT NULL,
    enviada_at        TIMESTAMPTZ,
    estado            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos          INTEGER NOT NULL DEFAULT 0,
    error             VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN notificaciones.estado IS 'PENDIENTE|ENVIANDO|ENVIADA|ERROR|CANCELADA';

CREATE INDEX idx_notificaciones_estado_programada ON notificaciones (estado, programada_at);
CREATE INDEX idx_notificaciones_cita ON notificaciones (cita_id);
CREATE INDEX idx_notificaciones_paciente ON notificaciones (paciente_id);

-- Origen de la confirmación de la cita (null = panel, WHATSAPP = WhatsApp)
ALTER TABLE citas
    ADD COLUMN confirmation_source VARCHAR(20);

-- ---------------------------------------------------------------------
-- Plantillas por defecto
-- ---------------------------------------------------------------------
INSERT INTO plantillas_mensajes (nombre, contenido) VALUES
    ('cita_creada', E'Hola {{nombre}} 👋\n\nTu cita quedó agendada:\n🦷 Servicio: {{servicio}}\n📅 Fecha: {{fecha}}\n🕐 Hora: {{hora}}\n👨‍⚕️ Odontólogo: {{doctor}}\n\nSi necesitas cambiar o cancelar, escríbenos por este chat.'),
    ('recordatorio_24h', E'Hola {{nombre}} 👋\n\nTe recordamos tu cita:\n🦷 Servicio: {{servicio}}\n📅 Fecha: {{fecha}}\n🕐 Hora: {{hora}}\n👨‍⚕️ Odontólogo: {{doctor}}\n\n¿Confirmas tu asistencia?\n\n1. Confirmar\n3. Cancelar'),
    ('cita_confirmada', E'¡Gracias {{nombre}}! ✅\n\nTu cita fue confirmada:\n🦷 Servicio: {{servicio}}\n📅 Fecha: {{fecha}}\n🕐 Hora: {{hora}}\n\nTe esperamos en {{clinica}}.'),
    ('cita_cancelada', E'Hola {{nombre}},\n\ntu cita del {{fecha}} a las {{hora}} fue cancelada.\n\nSi deseas reagendar, escríbenos y con gusto te ayudamos.'),
    ('cita_empezar', E'Hola {{nombre}}, te recordamos que en unos minutos tienes tu cita:\n🦷 {{servicio}}\n📅 {{fecha}}\n🕐 {{hora}}')
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- Automatizaciones por defecto
-- ---------------------------------------------------------------------
INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id)
SELECT 'Aviso de cita creada', 'CITA_CREADA', 0, p.id
FROM plantillas_mensajes p
WHERE p.nombre = 'cita_creada'
  AND NOT EXISTS (SELECT 1 FROM automatizaciones a WHERE a.evento = 'CITA_CREADA' AND a.minutos_antes = 0);

INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id)
SELECT 'Recordatorio 24 horas antes', 'CITA_PROXIMA', 1440, p.id
FROM plantillas_mensajes p
WHERE p.nombre = 'recordatorio_24h'
  AND NOT EXISTS (SELECT 1 FROM automatizaciones a WHERE a.evento = 'CITA_PROXIMA' AND a.minutos_antes = 1440);
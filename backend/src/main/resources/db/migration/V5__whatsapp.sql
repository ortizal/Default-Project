-- =====================================================================
-- Fase 5 - WhatsApp / OpenWA (sesiones, conversaciones, mensajes)
-- =====================================================================

-- Permisos WHATSAPP_READ y WHATSAPP_WRITE ya existen en V1
-- y están otorgados a SUPER_ADMIN, ADMIN y RECEPCION.

CREATE TABLE whatsapp_sesiones (
    id              BIGSERIAL PRIMARY KEY,
    sesion_id       VARCHAR(100) NOT NULL UNIQUE,
    nombre          VARCHAR(150),
    estado          VARCHAR(20)  NOT NULL DEFAULT 'DESCONECTADA',
    estado_detalle  TEXT,
    qr              TEXT,
    last_error      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE whatsapp_sesiones IS 'Sesión de WhatsApp gestionada por OpenWA';
COMMENT ON COLUMN whatsapp_sesiones.estado IS 'DESCONECTADA|CONECTANDO|CONECTADA|ERROR';

CREATE TABLE conversaciones (
    id                BIGSERIAL PRIMARY KEY,
    sesion_id         BIGINT NOT NULL REFERENCES whatsapp_sesiones(id) ON DELETE CASCADE,
    paciente_id       BIGINT REFERENCES pacientes(id) ON DELETE SET NULL,
    telefono          VARCHAR(30) NOT NULL,
    nombre_contacto   VARCHAR(150),
    estado            VARCHAR(20) NOT NULL DEFAULT 'BOT',
    ultimo_mensaje_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (sesion_id, telefono)
);

COMMENT ON COLUMN conversaciones.estado IS 'BOT|ATENCION_HUMANA|ATENDIDA|CERRADA';

CREATE INDEX idx_conversaciones_ultimo ON conversaciones (ultimo_mensaje_at DESC);
CREATE INDEX idx_conversaciones_sesion ON conversaciones (sesion_id);

CREATE TABLE mensajes (
    id              BIGSERIAL PRIMARY KEY,
    conversacion_id BIGINT NOT NULL REFERENCES conversaciones(id) ON DELETE CASCADE,
    direccion       VARCHAR(10) NOT NULL,
    texto           TEXT NOT NULL,
    remitente       VARCHAR(30),
    estado          VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO',
    error           VARCHAR(500),
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN mensajes.direccion IS 'ENTRADA|SALIDA';
COMMENT ON COLUMN mensajes.estado IS 'RECIBIDO|PENDIENTE|ENVIADO|ERROR';

CREATE INDEX idx_mensajes_conversacion ON mensajes (conversacion_id, received_at);
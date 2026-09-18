-- =====================================================================
-- Fase 1 - Esquema base de autenticación y auditoría
-- =====================================================================

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(50)  NOT NULL UNIQUE,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE permisos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(100) NOT NULL UNIQUE,
    nombre      VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE rol_permisos (
    rol_id      BIGINT NOT NULL REFERENCES roles(id)      ON DELETE CASCADE,
    permiso_id  BIGINT NOT NULL REFERENCES permisos(id)   ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuarios (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    email        VARCHAR(190) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    nombres      VARCHAR(150) NOT NULL,
    apellidos    VARCHAR(150) NOT NULL,
    telefono     VARCHAR(30),
    estado       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    ultimo_login TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario_roles (
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id      BIGINT NOT NULL REFERENCES roles(id)    ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE auditoria (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT,
    accion          VARCHAR(100) NOT NULL,
    modulo          VARCHAR(100) NOT NULL,
    entidad         VARCHAR(100),
    entidad_id      BIGINT,
    datos_anteriores JSONB,
    datos_nuevos     JSONB,
    ip              VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_entidad   ON auditoria (entidad, entidad_id);
CREATE INDEX idx_auditoria_created   ON auditoria (created_at);
CREATE INDEX idx_usuarios_estado     ON usuarios (estado);
CREATE INDEX idx_roles_codigo        ON roles (codigo);

-- =====================================================================
-- Datos base
-- =====================================================================

INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('SUPER_ADMIN', 'Super Administrador', 'Acceso total al sistema'),
    ('ADMIN',       'Administrador',       'Gestión operativa del CRM'),
    ('RECEPCION',   'Recepción',           'Atención de pacientes y agenda'),
    ('ODONTOLOGO',  'Odontólogo',          'Consulta de agenda y atención')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO permisos (codigo, nombre) VALUES
    ('PACIENTES_READ',    'Leer pacientes'),
    ('PACIENTES_WRITE',   'Crear/editar pacientes'),
    ('PACIENTES_DELETE',  'Desactivar pacientes'),
    ('CITAS_READ',        'Leer citas'),
    ('CITAS_WRITE',       'Crear/editar citas'),
    ('CITAS_CANCEL',      'Cancelar citas'),
    ('ODONTOLOGOS_READ',  'Leer odontólogos'),
    ('ODONTOLOGOS_WRITE', 'Crear/editar odontólogos'),
    ('SERVICIOS_READ',    'Leer servicios'),
    ('SERVICIOS_WRITE',   'Crear/editar servicios'),
    ('AGENDA_READ',       'Leer agenda'),
    ('AGENDA_WRITE',      'Administrar agenda'),
    ('WHATSAPP_READ',     'Leer WhatsApp'),
    ('WHATSAPP_WRITE',    'Enviar mensajes WhatsApp'),
    ('AUTOMATIZACIONES',  'Administrar automatizaciones'),
    ('REPORTES',          'Ver reportes'),
    ('USUARIOS_READ',     'Leer usuarios'),
    ('USUARIOS_WRITE',    'Administrar usuarios'),
    ('INTEGRACIONES',     'Administrar integraciones'),
    ('AUDITORIA_READ',    'Ver auditoría'),
    ('CONFIGURACION',     'Administrar configuración')
ON CONFLICT (codigo) DO NOTHING;

-- SUPER_ADMIN tiene todos los permisos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN: permisos operativos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'PACIENTES_READ','PACIENTES_WRITE','PACIENTES_DELETE',
    'CITAS_READ','CITAS_WRITE','CITAS_CANCEL',
    'ODONTOLOGOS_READ','ODONTOLOGOS_WRITE',
    'SERVICIOS_READ','SERVICIOS_WRITE',
    'AGENDA_READ','AGENDA_WRITE',
    'WHATSAPP_READ','WHATSAPP_WRITE',
    'AUTOMATIZACIONES','REPORTES',
    'USUARIOS_READ','AUDITORIA_READ'
)
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;

-- RECEPCION: agenda, pacientes, WhatsApp
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'PACIENTES_READ','PACIENTES_WRITE',
    'CITAS_READ','CITAS_WRITE','CITAS_CANCEL',
    'AGENDA_READ','AGENDA_WRITE',
    'WHATSAPP_READ','WHATSAPP_WRITE'
)
WHERE r.codigo = 'RECEPCION'
ON CONFLICT DO NOTHING;

-- ODONTOLOGO: lectura de agenda, citas y pacientes
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'PACIENTES_READ',
    'CITAS_READ',
    'ODONTOLOGOS_READ',
    'SERVICIOS_READ',
    'AGENDA_READ'
)
WHERE r.codigo = 'ODONTOLOGO'
ON CONFLICT DO NOTHING;

-- Usuario inicial SUPER_ADMIN (password: admin123, cambiar en producción)
INSERT INTO usuarios (username, email, password, nombres, apellidos)
VALUES (
    'admin',
    'admin@dentalcrm.local',
    '$2b$12$0/d9M3McbBPZS/x3OVjh/erdk68zuaqSEy0fqJ6GO84fY0E6sc0CK',
    'Super',
    'Admin'
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = 'SUPER_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;
CREATE TABLE configuracion_consultorio (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants (id),
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(500),
    telefono VARCHAR(30),
    enlace_ubicacion VARCHAR(1000),
    horario_atencion VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO permisos (codigo, nombre)
VALUES ('CONFIGURACION', 'Administrar configuración')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'CONFIGURACION'
WHERE r.codigo IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

UPDATE plantillas_mensajes
SET contenido = E'¡Gracias {{nombre}}! ✅\n\nTu cita fue confirmada:\n🦷 Servicio: {{servicio}}\n📅 Fecha: {{fecha}}\n🕐 Hora: {{hora}}\n\nTe esperamos en {{clinica}}.\n{{direccion_consultorio}}\n{{horario}}\n{{ubicacion}}'
WHERE nombre = 'cita_confirmada';

INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id, activa, tenant_id)
SELECT 'Confirmación de cita', 'CITA_CONFIRMADA', 0, p.id, TRUE, p.tenant_id
FROM plantillas_mensajes p
WHERE p.nombre = 'cita_confirmada'
  AND NOT EXISTS (
      SELECT 1 FROM automatizaciones a
      WHERE a.evento = 'CITA_CONFIRMADA' AND a.tenant_id = p.tenant_id
  );
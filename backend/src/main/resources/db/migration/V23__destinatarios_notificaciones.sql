ALTER TABLE automatizaciones
    ADD COLUMN destinatario VARCHAR(20) NOT NULL DEFAULT 'PACIENTE';

ALTER TABLE notificaciones
    ADD COLUMN destinatario VARCHAR(20) NOT NULL DEFAULT 'PACIENTE';

ALTER TABLE automatizaciones
    ADD CONSTRAINT chk_automatizaciones_destinatario
    CHECK (destinatario IN ('PACIENTE', 'ODONTOLOGO'));

ALTER TABLE notificaciones
    ADD CONSTRAINT chk_notificaciones_destinatario
    CHECK (destinatario IN ('PACIENTE', 'ODONTOLOGO'));

INSERT INTO plantillas_mensajes (nombre, contenido, tenant_id)
SELECT 'Aviso odontologo - cita agendada - ' || t.id,
       E'Nueva cita agendada\nPaciente: {{nombre_completo}}\nServicio: {{servicio}}\nFecha: {{fecha}}\nHora: {{hora}}',
       t.id
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM plantillas_mensajes p
    WHERE p.nombre = 'Aviso odontologo - cita agendada - ' || t.id
);

INSERT INTO plantillas_mensajes (nombre, contenido, tenant_id)
SELECT 'Aviso odontologo - cita confirmada - ' || t.id,
       E'Cita confirmada por el paciente\nPaciente: {{nombre_completo}}\nServicio: {{servicio}}\nFecha: {{fecha}}\nHora: {{hora}}',
       t.id
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM plantillas_mensajes p
    WHERE p.nombre = 'Aviso odontologo - cita confirmada - ' || t.id
);

INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id, activa, tenant_id, destinatario)
SELECT 'Aviso al odontólogo: cita agendada', 'CITA_CREADA', 0, p.id, TRUE, t.id, 'ODONTOLOGO'
FROM tenants t
JOIN plantillas_mensajes p ON p.nombre = 'Aviso odontologo - cita agendada - ' || t.id
WHERE NOT EXISTS (
    SELECT 1 FROM automatizaciones a
    WHERE a.evento = 'CITA_CREADA' AND a.tenant_id = t.id AND a.destinatario = 'ODONTOLOGO'
);

INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id, activa, tenant_id, destinatario)
SELECT 'Aviso al odontólogo: cita confirmada', 'CITA_CONFIRMADA', 0, p.id, TRUE, t.id, 'ODONTOLOGO'
FROM tenants t
JOIN plantillas_mensajes p ON p.nombre = 'Aviso odontologo - cita confirmada - ' || t.id
WHERE NOT EXISTS (
    SELECT 1 FROM automatizaciones a
    WHERE a.evento = 'CITA_CONFIRMADA' AND a.tenant_id = t.id AND a.destinatario = 'ODONTOLOGO'
);
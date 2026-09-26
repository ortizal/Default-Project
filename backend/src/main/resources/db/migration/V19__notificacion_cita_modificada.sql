-- Aviso al paciente cuando su cita cambia de día u hora: la plantilla muestra
-- el horario nuevo y el anterior, y la automatización va con minutos_antes=0
-- para que salga en el siguiente lote (inmediato).
INSERT INTO plantillas_mensajes (nombre, contenido) VALUES
    ('cita_modificada', E'Hola {{nombre}} 👋\n\nTu cita fue reprogramada:\n🦷 Servicio: {{servicio}}\n📅 Nueva fecha: {{fecha}} (antes {{fecha_anterior}})\n🕐 Nueva hora: {{hora}} (antes {{hora_anterior}})\n👨‍⚕️ Odontólogo: {{doctor}}\n\nSi no te queda bien, escríbenos y buscamos otra hora.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id, activa)
SELECT 'Aviso de cambio de hora', 'CITA_MODIFICADA', 0, p.id, TRUE
FROM plantillas_mensajes p
WHERE p.nombre = 'cita_modificada'
  AND NOT EXISTS (SELECT 1 FROM automatizaciones a WHERE a.evento = 'CITA_MODIFICADA');

COMMENT ON COLUMN automatizaciones.evento IS 'CITA_CREADA|CITA_PROXIMA|CITA_CONFIRMADA|CITA_CANCELADA|CITA_ATENDIDA|NO_ASISTIO|CITA_MODIFICADA';

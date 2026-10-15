-- =====================================================================
-- V45 - Notificación post-cobro (openwa + correo)
-- El aviso de un abono/anticipo recibido (evento COBRO_REALIZADO) se
-- encola como cualquier otro y sale por WhatsApp; si hay SMTP configurado
-- también por correo (asunto "Pago recibido").
-- =====================================================================

COMMENT ON COLUMN automatizaciones.evento IS
    'CITA_CREADA|CITA_PROXIMA|CITA_CONFIRMADA|CITA_CANCELADA|CITA_ATENDIDA|NO_ASISTIO|CITA_MODIFICADA|COBRO_REALIZADO';

-- ---------------------------------------------------------------------
-- Plantilla por tenant (nombre único global: se sufija con el tenant)
-- ---------------------------------------------------------------------
INSERT INTO plantillas_mensajes (nombre, contenido, tenant_id)
SELECT 'cobro_registrado - ' || t.id,
       E'Hola {{nombre}} 👋\n\nRecibimos tu pago:\n💰 Monto: {{monto}}\n💳 Forma de pago: {{forma_pago}}\n🧾 Concepto: {{concepto}}\n📅 Fecha: {{fecha}}\n\n📊 Saldo actual: {{saldo}}\n\nGracias por tu confianza en {{clinica}}.',
       t.id
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM plantillas_mensajes p
    WHERE p.nombre = 'cobro_registrado - ' || t.id
);

-- ---------------------------------------------------------------------
-- Automatización por defecto: inmediata y dirigida al paciente
-- ---------------------------------------------------------------------
INSERT INTO automatizaciones (nombre, evento, minutos_antes, plantilla_id, activa, tenant_id, destinatario)
SELECT 'Aviso de cobro recibido', 'COBRO_REALIZADO', 0, p.id, TRUE, t.id, 'PACIENTE'
FROM tenants t
JOIN plantillas_mensajes p ON p.nombre = 'cobro_registrado - ' || t.id
WHERE NOT EXISTS (
    SELECT 1 FROM automatizaciones a
    WHERE a.evento = 'COBRO_REALIZADO'
      AND a.tenant_id = t.id
      AND a.destinatario = 'PACIENTE'
);

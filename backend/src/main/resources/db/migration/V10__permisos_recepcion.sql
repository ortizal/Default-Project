-- =====================================================================
-- V10: conceder ODONTOLOGOS_READ y SERVICIOS_READ al rol RECEPCION
-- ---------------------------------------------------------------------
-- Motivo: el flujo de agenda/citas (RECEPCION) carga los odontólogos
-- activos (GET /api/v1/odontologos/activos, exige ODONTOLOGOS_READ) y
-- el desplegable de servicios (GET /api/v1/servicios?activos=true,
-- exige SERVICIOS_READ) para agendar. Sin estos permisos RECEPCION
-- recibe 403 al abrir el formulario de cita.
-- Solo se conceden los permisos de LECTURA (conserva el resto del rol).
-- =====================================================================

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('ODONTOLOGOS_READ', 'SERVICIOS_READ')
WHERE r.codigo = 'RECEPCION'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

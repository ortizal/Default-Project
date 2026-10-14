-- =====================================================================
-- V32: PERIODONCIA_READ para RECEPCION
-- ---------------------------------------------------------------------
-- Fase E del plan (§4.11). Recepción ya consulta el odontograma y los
-- planes en lectura (V27); con este permiso ve también la pestaña
-- Periodoncia sin poder registrar mediciones (no recibe PERIODONCIA_WRITE).
-- =====================================================================

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'PERIODONCIA_READ'
WHERE r.codigo = 'RECEPCION'
ON CONFLICT DO NOTHING;

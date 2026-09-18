-- =====================================================================
-- Fase 7 - Reportes
-- =====================================================================

-- RECEPCION puede ver el dashboard y los reportes (además de ADMIN/SUPER_ADMIN)
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'REPORTES'
WHERE r.codigo = 'RECEPCION'
ON CONFLICT DO NOTHING;

-- Índices de apoyo para las consultas agregadas de reportes
CREATE INDEX IF NOT EXISTS idx_citas_reporte_fecha_estado ON citas (fecha, estado);
CREATE INDEX IF NOT EXISTS idx_citas_reporte_confirmada   ON citas (fecha, confirmada);
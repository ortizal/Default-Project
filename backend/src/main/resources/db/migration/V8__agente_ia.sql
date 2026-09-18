-- =====================================================================
-- Fase 8 - IA: Agente conversacional
-- =====================================================================

-- Supervisión del agente de IA (respuestas automáticas por WhatsApp)
INSERT INTO permisos (codigo, nombre)
VALUES ('AGENTE_IA', 'Supervisar agente de IA')
ON CONFLICT (codigo) DO NOTHING;

-- SUPER_ADMIN tiene todos los permisos (incluido AGENTE_IA)
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'AGENTE_IA'
WHERE r.codigo = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN y RECEPCION pueden supervisar el agente
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'AGENTE_IA'
WHERE r.codigo IN ('ADMIN', 'RECEPCION')
ON CONFLICT DO NOTHING;

-- Estado conversacional del agente por conversación
ALTER TABLE conversaciones
    ADD COLUMN IF NOT EXISTS intencion       VARCHAR(40),
    ADD COLUMN IF NOT EXISTS contexto_agente JSONB,
    ADD COLUMN IF NOT EXISTS agente_activo   BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_conversaciones_intencion ON conversaciones (intencion);
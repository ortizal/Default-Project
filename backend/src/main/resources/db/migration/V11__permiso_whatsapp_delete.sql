-- Permiso para eliminar sesiones de WhatsApp
INSERT INTO permisos (codigo, nombre) VALUES
    ('WHATSAPP_DELETE', 'Eliminar sesiones WhatsApp')
ON CONFLICT (codigo) DO NOTHING;

-- SUPER_ADMIN tiene todos los permisos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'WHATSAPP_DELETE'
WHERE r.codigo IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;
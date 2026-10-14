-- =====================================================================
-- V27: permisos de Historia Clínica y Facturación
-- ---------------------------------------------------------------------
-- Fase A del plan de ampliación (FACTURACIÓN ELECTRÓNICA +
-- HISTORIA CLÍNICA ODONTOLÓGICA). Sólo se siembran los códigos y las
-- concesiones por rol; los endpoints y el UI llegarán en las fases
-- siguientes (B-G). Mismo patrón que V11/V22.
--
-- 25 códigos nuevos:
--   Historia clínica (16) · Facturación y caja (9)
-- =====================================================================

INSERT INTO permisos (codigo, nombre, descripcion) VALUES
    -- ---- Historia clínica (fases B-E) ----
    ('HISTORIA_CLINICA_READ',     'Leer historia clínica',        'Acceso de lectura a la historia clínica del paciente'),
    ('HISTORIA_CLINICA_WRITE',    'Editar historia clínica',      'Alta y edición de antecedentes, exámenes y evoluciones'),
    ('HISTORIA_CLINICA_CLOSE',    'Cerrar historia clínica',      'Cerrar/certificar registros clínicos (adendas auditadas)'),
    ('ODONTOGRAMA_READ',          'Leer odontograma',             'Visualizar odontograma y su historial'),
    ('ODONTOGRAMA_WRITE',         'Editar odontograma',           'Registrar condiciones y tratamientos por diente/superficie'),
    ('PERIODONCIA_READ',          'Leer periodontograma',         'Visualizar mediciones periodontales'),
    ('PERIODONCIA_WRITE',         'Editar periodontograma',       'Registrar sondaje, margen, inserción y movilidad'),
    ('PLAN_TRATAMIENTO_READ',     'Leer planes de tratamiento',   'Visualizar planes, presupuestos y su evolución'),
    ('PLAN_TRATAMIENTO_WRITE',    'Editar planes de tratamiento', 'Crear y modificar planes, líneas y presupuestos'),
    ('RECETAS_READ',              'Leer recetas',                 'Visualizar recetas emitidas'),
    ('RECETAS_WRITE',             'Emitir recetas',               'Crear y anular recetas médicas'),
    ('CONSENTIMIENTOS_READ',      'Leer consentimientos',         'Visualizar consentimientos informados'),
    ('CONSENTIMIENTOS_WRITE',     'Firmar consentimientos',       'Generar, firmar y archivar consentimientos'),
    ('ARCHIVOS_CLINICOS_READ',    'Leer archivos clínicos',       'Descargar radiografías, fotos y documentos del paciente'),
    ('ARCHIVOS_CLINICOS_WRITE',   'Subir archivos clínicos',      'Cargar y clasificar archivos clínicos'),
    ('REPORTES_CLINICOS',         'Reportes clínicos',            'Historias, evolución, planes pendientes y producción clínica'),
    -- ---- Facturación y caja (fases F-H) ----
    ('FACTURACION_READ',          'Leer facturación',             'Cuentas, comprobantes y reportes financieros en lectura'),
    ('FACTURACION_WRITE',         'Editar facturación',           'Cargos, descuentos, anticipos y abonos en la cuenta'),
    ('FACTURACION_EMITIR',        'Emitir comprobantes',          'Generar facturas y documentos electrónicos'),
    ('FACTURACION_ANULAR',        'Anular comprobantes',          'Solicitar anulación y emitir notas de crédito'),
    ('CAJA_READ',                 'Leer caja',                    'Ver aperturas, movimientos y cierres de caja'),
    ('CAJA_WRITE',                'Mover caja',                   'Registrar ingresos, cobros, anticipos y devoluciones'),
    ('CAJA_CERRAR',               'Cerrar caja',                  'Cerrar y conciliar cajas'),
    ('REPORTES_FINANCIEROS',      'Reportes financieros',         'Dashboard financiero y reportes de ventas/saldos'),
    ('CONFIGURACION_FACTURACION', 'Configurar facturación',       'RUC, establecimientos, secuenciales y firma SRI')
ON CONFLICT (codigo) DO NOTHING;

-- SUPER_ADMIN: todos los permisos (mismo criterio que V1)
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN: los 25 nuevos (gestión completa de clínica y finanzas)
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'HISTORIA_CLINICA_READ','HISTORIA_CLINICA_WRITE','HISTORIA_CLINICA_CLOSE',
    'ODONTOGRAMA_READ','ODONTOGRAMA_WRITE',
    'PERIODONCIA_READ','PERIODONCIA_WRITE',
    'PLAN_TRATAMIENTO_READ','PLAN_TRATAMIENTO_WRITE',
    'RECETAS_READ','RECETAS_WRITE',
    'CONSENTIMIENTOS_READ','CONSENTIMIENTOS_WRITE',
    'ARCHIVOS_CLINICOS_READ','ARCHIVOS_CLINICOS_WRITE',
    'REPORTES_CLINICOS',
    'FACTURACION_READ','FACTURACION_WRITE','FACTURACION_EMITIR','FACTURACION_ANULAR',
    'CAJA_READ','CAJA_WRITE','CAJA_CERRAR',
    'REPORTES_FINANCIEROS','CONFIGURACION_FACTURACION'
)
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;

-- ODONTOLOGO: clínica completa, sin ámbito financiero (no factura ni cobra)
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'HISTORIA_CLINICA_READ','HISTORIA_CLINICA_WRITE','HISTORIA_CLINICA_CLOSE',
    'ODONTOGRAMA_READ','ODONTOGRAMA_WRITE',
    'PERIODONCIA_READ','PERIODONCIA_WRITE',
    'PLAN_TRATAMIENTO_READ','PLAN_TRATAMIENTO_WRITE',
    'RECETAS_READ','RECETAS_WRITE',
    'CONSENTIMIENTOS_READ','CONSENTIMIENTOS_WRITE',
    'ARCHIVOS_CLINICOS_READ','ARCHIVOS_CLINICOS_WRITE',
    'REPORTES_CLINICOS'
)
WHERE r.codigo = 'ODONTOLOGO'
ON CONFLICT DO NOTHING;

-- RECEPCION: registra lo clínico básico y cobra; no cierra historia,
-- no emite ni anula comprobantes, no cierra caja ni toca configuración
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'HISTORIA_CLINICA_READ','HISTORIA_CLINICA_WRITE',
    'ODONTOGRAMA_READ',
    'PLAN_TRATAMIENTO_READ',
    'ARCHIVOS_CLINICOS_READ',
    'FACTURACION_READ','FACTURACION_WRITE',
    'CAJA_READ','CAJA_WRITE'
)
WHERE r.codigo = 'RECEPCION'
ON CONFLICT DO NOTHING;

-- =====================================================================
-- V41: comprobantes electrónicos y sus estados (Fase G, plan §3.5/§3.8)
-- ---------------------------------------------------------------------
-- Una fila por comprobante emitido. Se conservan DOS estados por
-- separado, como pide el plan §3.8:
--   * estado_generacion  → lo que hizo la aplicación
--                           (BORRADOR → GENERADO → FIRMADO → ENVIADO | ERROR)
--   * estado_tributario  → lo que dice el SRI
--                           (PENDIENTE → AUTORIZADO | DEVUELTO |
--                            NO_AUTORIZADO | ANULADO)
-- Nunca se borran filas emitidas (§3.8): la anulación es un estado.
--
-- Corrige además la URL de pruebas sembrada en V39: los servicios
-- offline del SRI (Recepcion/AutorizacionComprobantesOffline) viven en
-- celcer.sri.gob.ec para pruebas y cel.sri.gob.ec para producción
-- (ficha técnica de comprobantes electrónicos, §7.2).
-- =====================================================================

ALTER TABLE configuracion_facturacion
    ALTER COLUMN sri_url_pruebas SET DEFAULT 'https://celcer.sri.gob.ec';

UPDATE configuracion_facturacion
SET sri_url_pruebas = 'https://celcer.sri.gob.ec'
WHERE sri_url_pruebas = 'https://celpruebas.sri.gob.ec';

CREATE TABLE comprobantes_electronicos (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    tipo_comprobante      VARCHAR(2)   NOT NULL DEFAULT '01',
    clave_acceso          VARCHAR(49)  NOT NULL,
    establecimiento       VARCHAR(3)   NOT NULL,
    punto_emision         VARCHAR(3)   NOT NULL,
    secuencial            INTEGER      NOT NULL,
    numero_comprobante    VARCHAR(20)  NOT NULL,
    ambiente              VARCHAR(20)  NOT NULL,
    modo_firma            VARCHAR(20)  NOT NULL,
    estado_generacion     VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    estado_tributario     VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    -- true cuando el "envío" fue simulado (firma sin certificado, sólo
    -- permitido en ambiente de pruebas); la UI lo muestra como badge.
    simulado              BOOLEAN      NOT NULL DEFAULT FALSE,
    xml_firmado           TEXT,
    xml_autorizado        TEXT,
    autorizacion_numero   VARCHAR(49),
    autorizacion_fecha    TIMESTAMPTZ,
    -- último estado crudo devuelto por el SRI (RECIBIDA, DEVUELTA,
    -- EN_PROCESO, AUTORIZADO, NO_AUTORIZADO, SIMULADA…)
    sri_estado            VARCHAR(30),
    -- detalle legible de la última respuesta / mensajes del SRI
    sri_respuesta         TEXT,
    error_mensaje         TEXT,
    cuenta_id             BIGINT REFERENCES cuentas_paciente (id) ON DELETE SET NULL,
    paciente_id           BIGINT REFERENCES pacientes (id) ON DELETE SET NULL,
    created_by            BIGINT REFERENCES usuarios (id),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by            BIGINT REFERENCES usuarios (id),
    CONSTRAINT chk_comp_tipo CHECK (tipo_comprobante IN ('01', '04', '05', '06', '07')),
    CONSTRAINT chk_comp_clave CHECK (clave_acceso ~ '^[0-9]{49}$'),
    CONSTRAINT chk_comp_ambiente CHECK (ambiente IN ('PRUEBAS', 'PRODUCCION')),
    CONSTRAINT chk_comp_modo_firma CHECK (modo_firma IN ('SIMULADA', 'CERTIFICADO')),
    CONSTRAINT chk_comp_generacion CHECK (estado_generacion IN
        ('BORRADOR', 'GENERADO', 'FIRMADO', 'ENVIADO', 'ERROR')),
    CONSTRAINT chk_comp_tributario CHECK (estado_tributario IN
        ('PENDIENTE', 'AUTORIZADO', 'DEVUELTO', 'NO_AUTORIZADO', 'ANULADO')),
    CONSTRAINT chk_comp_secuencial CHECK (secuencial >= 1 AND secuencial <= 999999999),
    CONSTRAINT uq_comp_secuencial UNIQUE (tenant_id, establecimiento, punto_emision, secuencial)
);

CREATE UNIQUE INDEX uq_comp_clave_acceso ON comprobantes_electronicos (clave_acceso);
CREATE INDEX idx_comp_tributario ON comprobantes_electronicos (estado_tributario);
CREATE INDEX idx_comp_generacion ON comprobantes_electronicos (estado_generacion);
CREATE INDEX idx_comp_cuenta ON comprobantes_electronicos (cuenta_id);
CREATE INDEX idx_comp_paciente ON comprobantes_electronicos (paciente_id);
CREATE INDEX idx_comp_creado ON comprobantes_electronicos (created_at DESC);

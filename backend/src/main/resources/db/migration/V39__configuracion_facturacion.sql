-- =====================================================================
-- V39: configuración tributaria y credenciales SRI (Fase G, plan §3.5)
-- ---------------------------------------------------------------------
-- Una fila por tenant. Los datos fiscales (razón social, RUC, dirección,
-- teléfono) y el certificado .p12 ya viven en configuracion_consultorio
-- (V21/V24), así que aquí sólo va lo específico de la facturación
-- electrónica: ambiente, tipo de emisión, banderas de contribuyente,
-- secuencialización, URL de los servicios web del SRI y credenciales
-- (cifradas en la aplicación con IntegracionSecretCipher).
-- =====================================================================

CREATE TABLE configuracion_facturacion (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL UNIQUE REFERENCES tenants (id) ON DELETE CASCADE,
    ambiente                VARCHAR(20)  NOT NULL DEFAULT 'PRUEBAS',
    tipo_emision            VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    contribuyente_especial  BOOLEAN      NOT NULL DEFAULT FALSE,
    agente_retencion        BOOLEAN      NOT NULL DEFAULT FALSE,
    obligado_contabilidad   BOOLEAN      NOT NULL DEFAULT FALSE,
    establecimiento         VARCHAR(3)   NOT NULL DEFAULT '001',
    punto_emision           VARCHAR(3)   NOT NULL DEFAULT '001',
    secuencial_actual       INTEGER      NOT NULL DEFAULT 0,
    sri_url_pruebas         VARCHAR(500) NOT NULL DEFAULT 'https://celpruebas.sri.gob.ec',
    sri_url_produccion      VARCHAR(500) NOT NULL DEFAULT 'https://cel.sri.gob.ec',
    sri_usuario             VARCHAR(100),
    sri_clave               TEXT,
    firma_password          TEXT,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by              BIGINT REFERENCES usuarios (id),
    CONSTRAINT chk_fact_ambiente      CHECK (ambiente IN ('PRUEBAS', 'PRODUCCION')),
    CONSTRAINT chk_fact_tipo_emision  CHECK (tipo_emision IN ('NORMAL', 'PRUEBAS')),
    CONSTRAINT chk_fact_establecimiento CHECK (establecimiento ~ '^[0-9]{3}$'),
    CONSTRAINT chk_fact_punto_emision   CHECK (punto_emision ~ '^[0-9]{3}$'),
    CONSTRAINT chk_fact_secuencial      CHECK (secuencial_actual >= 0 AND secuencial_actual <= 999999999)
);

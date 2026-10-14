-- =====================================================================
-- V43: notas de crédito y anulación de comprobantes (Fase H, plan §3.9/§3.10)
-- ---------------------------------------------------------------------
-- 1) Secuencial por TIPO de comprobante: cada documento electrónico
--    (factura 01, nota de crédito 04, nota de débito 05, retención 07)
--    tiene su propia serie dentro del mismo establecimiento/punto de
--    emisión, como exige la ficha técnica del SRI.
-- 2) Referencia al comprobante modificado + motivo (obligatorios en la
--    nota de crédito, plan §3.9).
-- 3) Anulación controlada (§3.10): se registra la solicitud con su
--    motivo, usuario, resultado y auditoría. Nunca se borra la fila del
--    comprobante emitido.
-- =====================================================================

ALTER TABLE comprobantes_electronicos
    DROP CONSTRAINT uq_comp_secuencial;

ALTER TABLE comprobantes_electronicos
    ADD CONSTRAINT uq_comp_secuencial UNIQUE (tenant_id, tipo_comprobante,
                                              establecimiento, punto_emision, secuencial);

ALTER TABLE configuracion_facturacion
    ADD COLUMN secuencial_nota_credito INTEGER NOT NULL DEFAULT 0;

ALTER TABLE comprobantes_electronicos
    ADD COLUMN comprobante_referencia_id BIGINT REFERENCES comprobantes_electronicos (id),
    ADD COLUMN motivo VARCHAR(500);

CREATE INDEX idx_comp_referencia ON comprobantes_electronicos (comprobante_referencia_id);

CREATE TABLE anulaciones_comprobante (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT      NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    comprobante_id    BIGINT      NOT NULL REFERENCES comprobantes_electronicos (id) ON DELETE CASCADE,
    motivo            VARCHAR(500) NOT NULL,
    -- PENDIENTE → APLICADA | RECHAZADA (§3.10: estado y resultado)
    estado            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    resultado         VARCHAR(20),
    mensaje           TEXT,
    solicitado_por    BIGINT REFERENCES usuarios (id),
    solicitado_en     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    aplicado_en       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_anul_estado CHECK (estado IN ('PENDIENTE', 'APLICADA', 'RECHAZADA')),
    CONSTRAINT chk_anul_resultado CHECK (resultado IS NULL OR resultado IN ('APLICADA', 'RECHAZADA'))
);

CREATE INDEX idx_anul_comprobante ON anulaciones_comprobante (comprobante_id);
CREATE INDEX idx_anul_estado ON anulaciones_comprobante (estado);

-- =====================================================================
-- V42: totales del comprobante (Fase G.4, plan §3.7 bandeja)
-- ---------------------------------------------------------------------
-- El desglose tributario vive en el XML, pero la bandeja necesita el
-- importe total y la fecha de emisión sin volver a parsear el
-- comprobante: se guardan al emitir (SriXmlGenerator/FacturaBuilder).
-- =====================================================================

ALTER TABLE comprobantes_electronicos
    ADD COLUMN total NUMERIC(14, 2),
    ADD COLUMN fecha_emision DATE;

COMMENT ON COLUMN comprobantes_electronicos.total IS
    'Importe total de la factura (importeTotal del XML), incluido el IVA';
COMMENT ON COLUMN comprobantes_electronicos.fecha_emision IS
    'Fecha de emisión usada en la clave de acceso (día hábil ajustado)';

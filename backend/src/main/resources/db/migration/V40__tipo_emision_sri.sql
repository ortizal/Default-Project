-- =====================================================================
-- V40: tipo de emisión del SRI (Fase G, ficha técnica tabla 2)
-- ---------------------------------------------------------------------
-- V39 sembró 'PRUEBAS' como segundo valor de tipo_emision; el SRI sólo
-- admite 1 = Emisión Normal y 2 = Emisión por Indisponibilidad del
-- Sistema (contingencia). El ambiente PRUEBAS/PRODUCCIÓN es un campo
-- distinto, gestionado en la columna `ambiente`.
-- =====================================================================

ALTER TABLE configuracion_facturacion DROP CONSTRAINT chk_fact_tipo_emision;

UPDATE configuracion_facturacion
SET tipo_emision = 'CONTINGENCIA'
WHERE tipo_emision = 'PRUEBAS';

ALTER TABLE configuracion_facturacion
    ADD CONSTRAINT chk_fact_tipo_emision CHECK (tipo_emision IN ('NORMAL', 'CONTINGENCIA'));

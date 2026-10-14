-- =====================================================================
-- §4.14 Menores: los consentimientos reutilizan el tutor ya registrado
-- en el paciente (V12) en lugar de pedir el nombre a mano.
--
-- Se guarda un snapshot (id débil + parentesco + teléfono): los tutores
-- se reescriben al editar el paciente, así que el consentimiento —
-- igual que su texto — conserva los datos con los que se emitió.
-- =====================================================================

ALTER TABLE consentimientos
    ADD COLUMN tutor_id          BIGINT,
    ADD COLUMN tutor_parentesco  VARCHAR(20),
    ADD COLUMN tutor_telefono    VARCHAR(30);

COMMENT ON COLUMN consentimientos.tutor_id IS
    'Referencia débil a paciente_tutores (sin FK: el paciente puede reemplazar sus tutores)';
COMMENT ON COLUMN consentimientos.tutor_parentesco IS
    'PADRE/MADRE copiados al emitir el consentimiento';

CREATE INDEX idx_consentimientos_tutor ON consentimientos (tenant_id, tutor_id);

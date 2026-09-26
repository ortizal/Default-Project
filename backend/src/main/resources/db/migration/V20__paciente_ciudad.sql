-- Datos de contacto del paciente: ciudad junto a la dirección
ALTER TABLE pacientes
    ADD COLUMN ciudad VARCHAR(120);

ALTER TABLE configuracion_consultorio
    ADD COLUMN razon_social VARCHAR(200),
    ADD COLUMN ruc VARCHAR(13),
    ADD COLUMN correo_electronico VARCHAR(190),
    ADD COLUMN firma_digital BYTEA,
    ADD COLUMN firma_digital_nombre VARCHAR(255);
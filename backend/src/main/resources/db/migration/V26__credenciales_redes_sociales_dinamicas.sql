ALTER TABLE configuracion_integraciones
    ADD COLUMN meta_client_id VARCHAR(255),
    ADD COLUMN meta_client_secret TEXT,
    ADD COLUMN meta_redirect_uri VARCHAR(1000),
    ADD COLUMN tiktok_client_key VARCHAR(255),
    ADD COLUMN tiktok_client_secret TEXT,
    ADD COLUMN tiktok_redirect_uri VARCHAR(1000);
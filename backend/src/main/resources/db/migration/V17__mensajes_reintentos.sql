-- Recuperación de chats (Fase 10): controla los reintentos al reenviar un
-- mensaje de salida que falló o al retomar una conversación sin respuesta
-- tras un reinicio del servicio. Sin este contador, cada barrido volvería a
-- intentar indefinidamente un envío que sigue fallando.
ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS reintentos INT NOT NULL DEFAULT 0;

ALTER TABLE odontologos ADD COLUMN etiquetas VARCHAR(255);
ALTER TABLE odontologos ADD COLUMN google_calendar_id VARCHAR(255);
ALTER TABLE citas ADD COLUMN google_event_id_doctor VARCHAR(255);
ALTER TABLE citas ADD COLUMN google_calendar_id_doctor VARCHAR(255);
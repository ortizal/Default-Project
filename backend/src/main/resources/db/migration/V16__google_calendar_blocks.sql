ALTER TABLE bloqueos_agenda
    ADD COLUMN google_event_id VARCHAR(255),
    ADD COLUMN google_calendar_id VARCHAR(255);

CREATE UNIQUE INDEX uq_bloqueos_google_event
    ON bloqueos_agenda (google_calendar_id, google_event_id)
    WHERE google_event_id IS NOT NULL;
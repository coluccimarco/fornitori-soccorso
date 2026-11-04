-- FORNITORI / migrazione additiva: vincoli di coerenza e indici per le query più frequenti.
-- Non cancella e non riscrive dati; si può rieseguire. L'applicazione funziona anche senza questo script.
-- Se una VALIDATE fallisce, la transazione si annulla: verificare i dati, non correggerli alla cieca.
BEGIN;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_mission_active_has_driver' AND conrelid = 'missions'::regclass) THEN
        ALTER TABLE missions ADD CONSTRAINT ck_mission_active_has_driver
            CHECK (is_fixture OR stato NOT IN ('proposta', 'accettata') OR driver_id IS NOT NULL) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_outbox_channel' AND conrelid = 'notifications_outbox'::regclass) THEN
        ALTER TABLE notifications_outbox ADD CONSTRAINT ck_outbox_channel
            CHECK (channel IN ('telegram', 'whatsapp', 'internal')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_outbox_attempts' AND conrelid = 'notifications_outbox'::regclass) THEN
        ALTER TABLE notifications_outbox ADD CONSTRAINT ck_outbox_attempts
            CHECK (tentativi >= 0) NOT VALID;
    END IF;
END $$;
ALTER TABLE missions VALIDATE CONSTRAINT ck_mission_active_has_driver;
ALTER TABLE notifications_outbox VALIDATE CONSTRAINT ck_outbox_channel;
ALTER TABLE notifications_outbox VALIDATE CONSTRAINT ck_outbox_attempts;

CREATE INDEX IF NOT EXISTS idx_missions_driver_live ON missions(driver_id) WHERE NOT is_fixture;
CREATE INDEX IF NOT EXISTS idx_mission_notes_mission ON mission_notes(mission_id);
CREATE INDEX IF NOT EXISTS idx_mission_events_mission ON mission_events(mission_id);
CREATE INDEX IF NOT EXISTS idx_mission_events_type ON mission_events(event_type, id DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_outbox_mission ON notifications_outbox(mission_id);
CREATE INDEX IF NOT EXISTS idx_chat_mission_requests_recipient ON chat_mission_requests(destinatario);
COMMIT;

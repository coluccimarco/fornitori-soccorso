-- FORNITORI / migrazione: rimuove strutture inutilizzate e rende persistente lo stato di invio delle notifiche.
-- Da applicare dopo gli script precedenti. Si può rieseguire. Dopo questo script non rieseguire 01_schema.sql e 02_seed.sql.
BEGIN;
DROP TABLE IF EXISTS chat_sessions;
ALTER TABLE drivers DROP COLUMN IF EXISTS stato_seed;

-- PENDING: da inviare. SENDING: riservata da un dispatcher fino a prossimo_tentativo_il.
-- SENT/FAILED: esito finale. SKIPPED: nessun destinatario, non verrà mai inviata.
ALTER TABLE notifications_outbox ADD COLUMN IF NOT EXISTS prossimo_tentativo_il TIMESTAMPTZ;
ALTER TABLE notifications_outbox DROP CONSTRAINT IF EXISTS notifications_outbox_stato_check;
ALTER TABLE notifications_outbox ADD CONSTRAINT notifications_outbox_stato_check
    CHECK (stato IN ('PENDING','SENDING','SENT','FAILED','SKIPPED'));

-- Le missioni della centrale non hanno una chat: le righe esistenti si chiudono senza conservare il telefono.
UPDATE notifications_outbox SET stato = 'SKIPPED', destinatario = 'internal' WHERE channel = 'internal';

CREATE INDEX IF NOT EXISTS idx_notifications_outbox_claim
    ON notifications_outbox(channel, id) WHERE stato IN ('PENDING','SENDING');
COMMIT;

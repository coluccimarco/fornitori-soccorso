-- FORNITORI / migrazione notifiche: chat a cui inviare i messaggi delle missioni create da chatbot.
-- Non cancella dati e si può rieseguire.
BEGIN;
ALTER TABLE chat_mission_requests ADD COLUMN IF NOT EXISTS destinatario VARCHAR(120);
COMMIT;

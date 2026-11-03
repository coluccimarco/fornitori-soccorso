-- FORNITORI / migrazione chatbot: chiave di idempotenza delle missioni create da chat.
-- Non cancella dati e si può rieseguire.
BEGIN;
CREATE TABLE IF NOT EXISTS chat_mission_requests (
    request_id VARCHAR(64) PRIMARY KEY,
    mission_id BIGINT NOT NULL UNIQUE REFERENCES missions(id),
    session_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
COMMIT;

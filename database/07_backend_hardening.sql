-- Additive migration. Existing fixture/live rows are never rewritten.
BEGIN;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'missions_origin_pair' AND conrelid = 'missions'::regclass) THEN
        ALTER TABLE missions ADD CONSTRAINT missions_origin_pair
            CHECK ((origine_lat IS NULL) = (origine_lng IS NULL)) NOT VALID;
    END IF;
END $$;
-- Fails explicitly on legacy invalid pairs: investigate instead of inventing coordinates.
ALTER TABLE missions VALIDATE CONSTRAINT missions_origin_pair;

CREATE TABLE IF NOT EXISTS chat_lookup_grants (
    session_id VARCHAR(64) PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_chat_lookup_grants_expiry ON chat_lookup_grants(expires_at);
ALTER TABLE chat_mission_requests ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);
COMMIT;

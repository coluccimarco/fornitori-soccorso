-- FORNITORI / migrazione additiva: stati operativi, annullamento, supporto autista, polizza da verificare,
-- disponibilità e officina di riferimento degli autisti. Non riscrive le missioni esistenti. Si può rieseguire.
BEGIN;

-- Stati della missione.
ALTER TABLE missions DROP CONSTRAINT IF EXISTS missions_stato_check;
ALTER TABLE missions ADD CONSTRAINT missions_stato_check CHECK (stato IN (
    'nuova', 'proposta', 'accettata', 'eseguita', 'non_accettata', 'scaduta',
    'verifica_polizza', 'gestione_manuale', 'annullata')) NOT VALID;
ALTER TABLE missions VALIDATE CONSTRAINT missions_stato_check;

-- Dati raccolti in chat e annullamento.
ALTER TABLE missions ADD COLUMN IF NOT EXISTS telefono_sul_posto VARCHAR(32);
ALTER TABLE missions ADD COLUMN IF NOT EXISTS tipo_problema VARCHAR(30);
ALTER TABLE missions ADD COLUMN IF NOT EXISTS targa_dichiarata VARCHAR(20);
ALTER TABLE missions ADD COLUMN IF NOT EXISTS annullata_da VARCHAR(10);
ALTER TABLE missions ADD COLUMN IF NOT EXISTS annullata_il TIMESTAMPTZ;
ALTER TABLE missions ADD COLUMN IF NOT EXISTS motivo_annullamento TEXT;

ALTER TABLE missions DROP CONSTRAINT IF EXISTS ck_mission_tipo_problema;
ALTER TABLE missions ADD CONSTRAINT ck_mission_tipo_problema CHECK (tipo_problema IS NULL OR tipo_problema IN (
    'RUOTA_CON_SCORTA', 'RUOTA_SENZA_SCORTA', 'BATTERIA_SCARICA', 'GUASTO_MOTORE', 'INCIDENTE')) NOT VALID;
ALTER TABLE missions VALIDATE CONSTRAINT ck_mission_tipo_problema;

ALTER TABLE missions DROP CONSTRAINT IF EXISTS ck_mission_annullata_da;
ALTER TABLE missions ADD CONSTRAINT ck_mission_annullata_da
    CHECK (annullata_da IS NULL OR annullata_da IN ('autista', 'centrale')) NOT VALID;
ALTER TABLE missions VALIDATE CONSTRAINT ck_mission_annullata_da;

ALTER TABLE missions DROP CONSTRAINT IF EXISTS ck_mission_cancelled;
ALTER TABLE missions ADD CONSTRAINT ck_mission_cancelled CHECK (stato <> 'annullata'
    OR (annullata_da IS NOT NULL AND annullata_il IS NOT NULL AND costo_finale IS NOT NULL)) NOT VALID;
ALTER TABLE missions VALIDATE CONSTRAINT ck_mission_cancelled;

-- Il cliente può mancare solo finché la polizza è da verificare, o se la centrale ha chiuso la pratica senza cliente.
ALTER TABLE missions ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE missions DROP CONSTRAINT IF EXISTS ck_mission_customer_required;
ALTER TABLE missions ADD CONSTRAINT ck_mission_customer_required CHECK (customer_id IS NOT NULL
    OR stato = 'verifica_polizza' OR (stato = 'annullata' AND annullata_da = 'centrale' AND driver_id IS NULL)) NOT VALID;
ALTER TABLE missions VALIDATE CONSTRAINT ck_mission_customer_required;

-- Avvisi e richieste di supporto dell'autista.
ALTER TABLE mission_notes ADD COLUMN IF NOT EXISTS tipo VARCHAR(10) NOT NULL DEFAULT 'AVVISO';
ALTER TABLE mission_notes ADD COLUMN IF NOT EXISTS gestita_il TIMESTAMPTZ;
ALTER TABLE mission_notes ADD COLUMN IF NOT EXISTS gestita_da BIGINT REFERENCES app_users(id);
ALTER TABLE mission_notes DROP CONSTRAINT IF EXISTS ck_mission_note_tipo;
ALTER TABLE mission_notes ADD CONSTRAINT ck_mission_note_tipo CHECK (tipo IN ('AVVISO', 'SUPPORTO')) NOT VALID;
ALTER TABLE mission_notes VALIDATE CONSTRAINT ck_mission_note_tipo;

-- Disponibilità e punto di partenza e rientro dell'autista.
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS disponibile BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS officina_riferimento_id BIGINT REFERENCES workshops(id);
UPDATE drivers d SET officina_riferimento_id = (
    SELECT w.id
    FROM workshops w, vehicles v
    WHERE v.id = d.vehicle_id
    ORDER BY power((w.lat - v.base_lat)::double precision, 2)
           + power((w.lng - v.base_lng)::double precision * cos(radians(v.base_lat::double precision)), 2), w.id
    LIMIT 1)
WHERE d.officina_riferimento_id IS NULL;

ALTER TABLE customers ADD COLUMN IF NOT EXISTS cognome VARCHAR(100);

-- Una targa senza polizza verificabile autorizza una pratica anche senza cliente: il grant ricorda la targa.
ALTER TABLE chat_lookup_grants ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE chat_lookup_grants ADD COLUMN IF NOT EXISTS targa_norm VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_mission_notes_open_support
    ON mission_notes(mission_id) WHERE tipo = 'SUPPORTO' AND gestita_il IS NULL;
CREATE INDEX IF NOT EXISTS idx_mission_proposals_negative
    ON mission_proposals(mission_id, driver_id) WHERE stato IN ('non_accettata', 'scaduta');
CREATE INDEX IF NOT EXISTS idx_missions_to_assign
    ON missions(id) WHERE NOT is_fixture AND stato IN ('nuova', 'non_accettata', 'scaduta');
COMMIT;

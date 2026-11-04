-- FORNITORI / migrazione additiva: la fascia notturna inizia alle 22:00 invece che alle 20:00.
-- Cambia solo la configurazione: le missioni già chiuse conservano il costo calcolato. Si può rieseguire.
BEGIN;
UPDATE pricing_config SET night_start = '22:00' WHERE id = 1 AND night_start = '20:00';
COMMIT;

-- FORNITORI: pulizia dei dati di prova prima di avviare il backend nuovo.
--
-- NON spostare questo file in database/: la CI e l'installazione eseguono database/*.sql in ordine.
-- Si esegue con psql, a backend fermo e dopo la migrazione 11. Seguire docs/deploy/CHECKLIST_AMBIENTE_REALE.md.
-- Questo script non è mai stato eseguito: va provato prima su una copia del database.
--
-- Di default NON cancella nulla: mostra cosa farebbe e chiude con ROLLBACK.
--   Anteprima:  psql -d <database> -f docs/deploy/pulizia_dati_prova.sql
--   Conferma:   psql -d <database> -v conferma=SI -f docs/deploy/pulizia_dati_prova.sql
--
-- ambito=aperte (default): solo le missioni live che lo scheduler riprenderebbe o rimetterebbe in giro,
--                          cioè nuova, proposta, non_accettata, scaduta. Storico e missioni accettate restano.
-- ambito=tutte:            tutte le missioni live, comprese quelle eseguite o accettate, e i grant della chat.
--                          Usarlo solo se nessuna missione live è reale.
--
-- Non tocca mai: missioni di riferimento (is_fixture), clienti, autisti, mezzi, officine, utenti, tariffe.

\set ON_ERROR_STOP on

\if :{?ambito}
\else
  \set ambito aperte
\endif
\if :{?conferma}
\else
  \set conferma NO
\endif

SELECT :'ambito' IN ('aperte', 'tutte') AS ambito_valido, :'conferma' = 'SI' AS confermato \gset
\if :ambito_valido
\else
  \echo 'ambito non valido: usare aperte oppure tutte. Nessuna modifica.'
  \quit
\endif

\echo 'Database:'
SELECT current_database() AS database, now() AS adesso, :'ambito' AS ambito, :'conferma' AS conferma;

BEGIN;

CREATE TEMP TABLE missioni_da_pulire ON COMMIT DROP AS
SELECT id, codice, stato, creata_il
FROM missions
WHERE NOT is_fixture
  AND (:'ambito' = 'tutte' OR stato IN ('nuova', 'proposta', 'non_accettata', 'scaduta'));

\echo 'Missioni che verrebbero cancellate, per stato:'
SELECT stato, count(*) AS missioni, min(creata_il) AS prima, max(creata_il) AS ultima
FROM missioni_da_pulire GROUP BY stato ORDER BY stato;

\echo 'Missioni live che restano, per stato:'
SELECT stato, count(*) AS missioni
FROM missions
WHERE NOT is_fixture AND id NOT IN (SELECT id FROM missioni_da_pulire)
GROUP BY stato ORDER BY stato;

\echo 'Clienti oltre il seed (id > 40), solo da controllare a mano: lo script non li cancella.'
SELECT id, nome, targa FROM customers WHERE id > 40 ORDER BY id;

-- Prima le tabelle collegate, poi le missioni: ogni DELETE stampa quante righe ha tolto.
DELETE FROM notifications_outbox WHERE mission_id IN (SELECT id FROM missioni_da_pulire);
DELETE FROM mission_notes WHERE mission_id IN (SELECT id FROM missioni_da_pulire);
DELETE FROM mission_events WHERE mission_id IN (SELECT id FROM missioni_da_pulire);
DELETE FROM mission_proposals WHERE mission_id IN (SELECT id FROM missioni_da_pulire);
DELETE FROM chat_mission_requests WHERE mission_id IN (SELECT id FROM missioni_da_pulire);
DELETE FROM missions WHERE id IN (SELECT id FROM missioni_da_pulire);

SELECT :'ambito' = 'tutte' AS pulisci_grant \gset
\if :pulisci_grant
  DELETE FROM chat_lookup_grants;
\endif

\echo 'Missioni che lo scheduler riprenderebbe dopo la pulizia (deve essere 0):'
SELECT count(*) AS ancora_assegnabili
FROM missions
WHERE NOT is_fixture AND stato IN ('nuova', 'non_accettata', 'scaduta');

\if :confermato
  COMMIT;
  \echo 'FATTO: modifiche confermate.'
\else
  ROLLBACK;
  \echo 'ANTEPRIMA: nessuna modifica salvata. Per confermare aggiungere -v conferma=SI'
\endif

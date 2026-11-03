# Dizionario dati

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Database Lead<br>
**Indice:** tabelle · integrità · baseline · tariffa

Schema relazionale PostgreSQL creato da `database/01_schema.sql`. Gli importi sono
`NUMERIC`, gli orari operativi `TIMESTAMPTZ`; Hibernate verifica lo schema senza
crearlo o aggiornarlo.

| Tabella | Scopo e relazioni principali |
|---|---|
| `app_users` | Utenti autenticabili, email univoca, hash password BCrypt e ruolo |
| `drivers` | Autista legato a un solo utente e a un solo mezzo; `stato_seed` è solo il valore importato |
| `vehicles` | Targa univoca, tipologia, portata e coordinate della base operativa |
| `insurance_companies` | Compagnie assicurative |
| `customers` | Anagrafica cliente, targa normalizzata univoca, compagnia FK, scadenza polizza |
| `workshops` | Officine e coordinate |
| `missions` | Missione, cliente/autista/officina FK, stato, località, date, chilometri e costi |
| `mission_proposals` | Tentativi di proposta e relativa scadenza; consente di conservare lo storico |
| `mission_notes` | Segnalazioni testuali collegate alla missione e all'utente |
| `mission_events` | Eventi di audit della missione con dettagli JSON |
| `pricing_config` | Parametri tariffari e durata della proposta |
| `chat_sessions` | Stato della conversazione per canale e chat esterna |
| `notifications_outbox` | Eventi da inviare e stato dei tentativi di notifica |

## Integrità e comportamento

- Chiavi primarie identity e foreign key proteggono le relazioni.
- Vincoli `CHECK` limitano ruoli, stati, coordinate, importi e note.
- Indici supportano ricerca missioni per stato/fixture e per cliente.
- Indice parziale impedisce più missioni operative attive per lo stesso autista;
  non coinvolge i record fixture.
- L'unicità della targa normalizzata impedisce duplicati anche con spazi o
  differenze di maiuscole.
- `is_fixture=TRUE` separa i 30 record didattici dalle missioni operative nuove.
  Disponibilità e scadenze live devono basarsi solo su missioni non fixture.
- Il seed è ri-eseguibile senza duplicare le righe fixture e sincronizza le
  sequenze identity per le successive insert applicative.

## Baseline docente

I conteggi attesi verificati sul foglio Excel (non sul database locale) sono:
10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti, 6 officine e 30 missioni.
Lo storico atteso riporta 16 eseguite, 6 pending, 4 scadute e 4 non accettate,
per un totale eseguito di EUR 1.509,71. `04_assert_baseline.sql` verifica anche
i subtotali per autista e termina con errore in caso di discrepanza.

Il foglio storico contiene missioni aperte alla data del 28/10/2026 e due missioni
attive assegnate all'autista 3 (M-2026-00026 e M-2026-00028). Il seed conserva
questi valori: non vanno corretti per soddisfare le regole operative.

## Tariffa

La configurazione conserva la maggiorazione notturna del foglio Excel come aliquota
`0.25`; il calcolo applicativo corrispondente è `(50 + 1.20 × km) × 1.25`. L'Excel
indica l'orario di accettazione per stabilire la fascia, mentre il PDF non specifica
il timestamp. La decisione va confermata prima dell'implementazione Sprint 2.

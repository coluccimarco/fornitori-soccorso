# Database PostgreSQL

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Database Lead<br>
**Indice:** schema · relazioni · fixture · setup · verifica

## Scopo e stato

Descrive il modello e le istruzioni per un database locale isolato. Script e seed
sono **IMPLEMENTATI**; la baseline PostgreSQL locale è **BLOCCATA/DA VALIDARE**
finché non si dispone di credenziali DB autorizzate. ER:
[`diagrams/database_er.mmd`](./diagrams/database_er.mmd).

## Modello

Tabelle: `app_users`, `drivers`, `vehicles`, `insurance_companies`, `customers`,
`workshops`, `pricing_config`, `missions`, `mission_proposals`, `mission_notes`,
`mission_events`, `chat_sessions`, `notifications_outbox`. Chiavi esterne collegano
autista/utente/mezzo, cliente/compagnia, missione/cliente/autista/officina,
proposte/note/eventi alla missione. Vincoli CHECK e unique index proteggono range,
stati e occupazione live. Vedi [dizionario](./DATABASE_DICTIONARY.md).

## Dataset di riferimento

Attesi dal foglio Excel: 10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti,
6 officine e 30 missioni storiche; 16 completate, 6 pendenti, 4 scadute,
4 rifiutate e totale completate €1.509,71. Questi valori sono **baseline sorgente**,
non un risultato PostgreSQL locale. Il seed usa hash BCrypt e `is_fixture=TRUE`.
Non alterare lo storico per far passare l'assertion.

## Configurazione e caricamento

Prerequisiti: PostgreSQL 18+ o compatibile e `psql`; database locale vuoto e isolato.
Imposta URL, username e password via variabili d'ambiente come nel README, quindi:

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/03_verifiche.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
```

Lo schema usa Hibernate `validate`; non usare `ddl-auto=update`. Gli script non
cancellano il database. Le missioni fixture non partecipano alla disponibilità
operativa e non devono essere marcate retroattivamente scadute.

## Criteri di completamento

Setup riuscito, quattro script completati senza errori, conteggi e assertion
baseline corrispondenti, output/data/commit registrati in
[TEST_EVIDENCE.md](./TEST_EVIDENCE.md). L'attuale job CI usa un servizio PostgreSQL
effimero e non credenziali di produzione.

**Dipendenze:** PostgreSQL, psql, env datasource. **Riferimenti:** [runbook](./BACKEND_SPRINT1_RUNBOOK.md),
[decisioni](./DECISION_LOG.md), [regole](./05_BUSINESS_RULES.md).

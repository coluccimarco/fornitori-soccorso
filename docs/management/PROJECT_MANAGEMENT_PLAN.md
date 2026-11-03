# Piano di gestione del progetto

**Codice:** FOR-PMP-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Responsabile:** team (facilitatore indicato storicamente come M1) · **Stato:** baseline ricostruita, approvazione da confermare

## Metodo e calendario

Il contesto dichiara cinque sviluppatori e quattro Sprint giornalieri secondo
Scrum adattato. Le schede `docs/agile/SPRINT_1.md`–`SPRINT_4.md` descrivono obiettivi
e attività; date, verbali di planning/daily/review/retrospettiva e consuntivi non
sono stati ricostruiti dai soli commit. Per lo stato verificabile vedi
[registro Sprint](../agile/README.md).

## Governance

- **Fonte implementativa:** codice sorgente e configurazione al commit dichiarato.
- **Fonte API:** `api/openapi.yaml`; modifiche concordate da provider e consumer.
- **Fonte DB:** script versionati in `database/`; niente migrazioni manuali non documentate.
- **Fonte requisiti:** specifica docente, PDF/Excel da acquisire; il BUR corrente è
  una baseline provvisoria, non sostituisce le fonti originali.
- **Fonte QA:** job, test, assert e report datati. “Verificato” richiede il riferimento
  alla prova e al commit.

## Cadenza operativa

1. Selezionare item del backlog solo dopo chiarimento di valore, owner, dipendenze,
   acceptance e prova.
2. Durante il lavoro aggiornare issue/impedimenti e non cambiare scope in silenzio.
3. Aprire PR con test, contratti, documentazione e revisione del consumer per i
   cambi API/schema.
4. Alla review dimostrare la build concreta; separare simulazioni da provider reali.
5. Registrare retrospettive solo quando esiste un verbale reale.

## Gate e metriche

| Gate | Condizione minima | Stato alla baseline |
|---|---|---|
| Specifica | PDF/Excel acquisiti e requisiti confermati | BLOCKED |
| Backend | Maven verify, CI e assert PostgreSQL | CI PASS a `f763d53`, run `41656606659` |
| Contratto | Status e ruoli OpenAPI coerenti coi controller/test | 34 operation; 30 implemented, 4 designed; description aggiornata |
| Chatbot | Test automatizzati e canale dichiarato | Test presenti; CI verde; Telegram è dichiarato provato nei documenti modulo |
| UI e mappe | Prodotto presente e testabile | Client e API mappe presenti; browser E2E, provider live e mappa visuale NOT RUN |
| Rilascio didattico | Acceptance docente e demo ripetibile | DA CONFERMARE |

Non si dichiarano velocity, burndown, percentuali di completamento o story point
effettivi senza registri attendibili. Il dashboard HTML storico è statico.

## Responsabilità e cambi

La [matrice RACI](./RACI_MATRIX.md) individua owner funzionali da confermare. Ogni
variazione a requisiti segue un change record; aggiornare requisito, OpenAPI,
test e guide insieme. Nessun push, merge o commit è automatico come conseguenza
della generazione documentale.

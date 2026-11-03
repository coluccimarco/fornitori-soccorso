# Sprint 2 — Workflow missioni

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Scrum Master<br>
**Indice:** goal · implementazione backend · acceptance · test · blocchi

## Sprint Goal

Completare il workflow backend centrale/autista: missione, proposta, timeout,
accept/reject, esecuzione, costi, note e dashboard.

## Stato attività

| Storia / attività | Owner | Dipendenze | Stima indicativa |
|---|---|---|---|
| Regole pricing e stati | M1 | D-002/D-003 | Implementato; timestamp accept e `HALF_UP`, conferma docente pendente |
| Service/repository missione, lock e transizioni | M1 | schema/DB | Implementato nel branch |
| Endpoint REST e security per ruoli | M1 | contratto | Implementato; promozione OpenAPI dopo CI PostgreSQL |
| Test pricing, deadline, permessi e concorrenza PostgreSQL | M1 | PostgreSQL, `Clock` | Unit/MVC e PostgreSQL CI PASS; locale senza password |
| UI centrale e autista | M2 | API | Fuori dal lavoro backend; da integrare |
| Contratti mappe, chatbot e notifiche | M3/M4/M5 | APIs/eventi | Integrazione e provider ancora da completare |

## Acceptance criteria

- Missione creata `nuova`; proposta registrata nello storico e assegnazione serializzata con lock.
- `now >= deadline` impedisce acceptance; scheduler modifica solo missioni live.
- Reject/expire mantengono lo storico; accettazione e chiusura generano eventi/outbox transazionali.
- Chiusura calcola tariffa server-side in Europe/Rome; rettifica mantiene il costo calcolato e registra motivo/audit.
- Autorizzazioni driver verificate server-side; dashboard distingue reference/live e storico è limitato all'autista autenticato.
- Il test PostgreSQL di concorrenza e il flusso DB completo devono passare nella CI prima di promuovere le operazioni OpenAPI.

## Demo, DoD e rischi

Demo prevista: creare chiamata, proporre, rifiutare/accettare, chiudere, mostrare
costo rettificato e aggregato. Implementazione backend disponibile sul branch `feature/backend`, con test
unit/MVC e CI PostgreSQL passati. La verifica locale resta bloccata dall'assenza
di credenziali; le operazioni OpenAPI sono state promosse a `implemented`.

**Dipendenze:** decisioni, DB funzionante, Java25 CI, API approvate.
**Riferimenti:** [business rules](../05_BUSINESS_RULES.md),
[states](../diagrams/mission_states.mmd), [report Sprint 2](../SPRINT_2_REPORT.md).

### Nota di riconciliazione — 03/11/2026

La scheda è uno snapshot di Sprint e non è stata riscritta retroattivamente.
Nel `main` integrato la funzionalità è arrivata con PR #3; il run CI
`41651140562` sul commit `25c613b` ha entrambi i job Java/OpenAPI e PostgreSQL 18
in success. Questa evidenza non certifica una Sprint Review o una demo UI.

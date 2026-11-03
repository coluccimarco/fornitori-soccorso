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
| Test pricing, deadline, permessi e concorrenza PostgreSQL | M1 | PostgreSQL, `Clock` | Unit/MVC passati; test PostgreSQL aggiunti ma non eseguibili localmente senza password |
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
costo rettificato e aggregato. Implementazione backend disponibile sul branch
`feature/backend-sprint2`; test unit/MVC passati. L'integrazione PostgreSQL locale è
bloccata dall'assenza di credenziali, quindi DoD e promozione OpenAPI restano
subordinate al job CI PostgreSQL.

**Dipendenze:** decisioni, DB funzionante, Java25 CI, API approvate.
**Riferimenti:** [business rules](../05_BUSINESS_RULES.md),
[states](../diagrams/mission_states.mmd), [report Sprint 2](../SPRINT_2_REPORT.md).

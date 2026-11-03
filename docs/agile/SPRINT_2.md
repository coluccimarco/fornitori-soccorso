# Sprint 2 — Workflow missioni

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Scrum Master<br>
**Indice:** goal · storie · dipendenze · acceptance · demo · rischi

## Sprint Goal

Concludere il flusso centrale/autista: missione, proposta, timeout, accept/reject,
esecuzione, costi e dashboard. Tutto **PIANIFICATO**, non implementato in Sprint 1.

## Attività pianificate

| Storia / attività | Owner | Dipendenze | Stima indicativa |
|---|---|---|---|
| Conferma regole tariffa/timestamp e stati | M1 + docente | D-002/D-003 | 1 h |
| Service/repository missione con lock e transizioni | M1 | schema/DB | 4–6 h |
| Endpoint OpenAPI e security per ruoli | M1 | contract review | 2–3 h |
| UI centrale: create/list/detail/propose/countdown | M2 | API create/propose | 4–6 h |
| UI autista: accept/reject/detail/complete | M2 | auth driver + APIs | 3–5 h |
| Test concurrency, timeout, pricing, idempotenza | M1 | PostgreSQL, Clock | 3–5 h |
| Smoke contract con UI e review | M1/M2 | feature complete | 1–2 h |

M3/M4/M5 partecipano alle sessioni di contract refinement, non costruiscono
integrazioni premature senza eventi/route definiti.

## Acceptance criteria

- Missione creata `nuova`, proposta ad autista libero e registrata nello storico.
- Accettazione a deadline valida; reject/expire e nuova proposta atomici.
- `now >= deadline` impedisce acceptance anche senza scheduler.
- Doppia assegnazione bloccata sotto concorrenza; fixture non alterate.
- Chiusura calcola importo server-side una sola volta e rettifica ha motivo/audit.
- Dashboard totalizza correttamente; UI gestisce 400/401/403/409.

## Demo, DoD e rischi

Demo: creare chiamata, proporre, rifiutare/accettare in due sessioni, chiudere,
mostrare costo e aggregato. DoD: integration tests PostgreSQL verdi e diagrammi/API
aggiornati. Rischi: tariffa ambigua, lock/timeout, scope e UI in un giorno.
Passaggio a Sprint 3 solo dopo test di concurrency e payload congelati.

**Dipendenze:** decisioni, DB funzionante, Java25 CI, API approvate.
**Riferimenti:** [business rules](../05_BUSINESS_RULES.md),
[states](../diagrams/mission_states.mmd).

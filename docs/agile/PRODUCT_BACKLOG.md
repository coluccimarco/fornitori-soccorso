# Product backlog FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Product Owner/Team Lead<br>
**Indice:** priorità · epic · dipendenze · refinement

## Scopo e regole

Backlog iniziale per i quattro Sprint. Priorità P0/P1/P2; stime sono ipotesi da
raffinare con il gruppo. Lo stato reale va aggiornato tramite issue e PR, non
retrodatato.

| ID | Priorità | Epic / storia | Owner | Dipendenze | Stato |
|---|---|---|---|---|---|
| B-01 | P0 | Seed, schema, auth e anagrafiche Sprint 1 | M1 | Excel/spec | Implementato; PostgreSQL da validare |
| B-02 | P0 | Java 25, CI e contratti condivisi | M1 | JDK 25 | Implementato localmente; CI remota da validare |
| B-03 | P1 | Login e anagrafiche web | M2 | B-01, API | Pianificato |
| B-04 | P0 | Creazione missione e dettaglio centrale | M1/M2 | B-01, requisiti | Pianificato |
| B-05 | P0 | Proposal/accept/reject con timeout e concorrenza | M1/M2 | B-04, DB | Pianificato |
| B-06 | P0 | Chiusura, pricing e dashboard | M1/M2 | B-05, decisioni costo | Pianificato |
| B-07 | P1 | Chatbot menu guidato e creazione richiesta verificata | M3/M1 | B-04, auth machine | Pianificato |
| B-08 | P1 | Mappe e route con km stimati/effettivi distinti | M4/M1/M2 | B-04, provider | Pianificato |
| B-09 | P1 | Eventi, outbox, webhook e notifiche idempotenti | M5/M1/M3 | B-05/B-06 | Pianificato |
| B-10 | P0 | QA integrata, sicurezza e demo | Tutti | B-03–B-09 | Pianificato |

## Refinement

Ogni storia deve avere scenario utente, dati/permessi, acceptance criteria,
dipendenze, test, rollback/rischio e owner prima di entrare nello Sprint. Cambi
business o API passano dal [contratto integrato](../team/INTEGRATION_CONTRACTS.md).

**Criterio di completamento:** backlog prioritizzato e nessuna story con blocker
nascosto. **Riferimenti:** [piano Sprint](../08_AGILE_SCRUM.md), [business rules](../05_BUSINESS_RULES.md).

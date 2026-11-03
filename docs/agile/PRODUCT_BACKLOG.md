# Product backlog FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Product Owner/Team Lead<br>
**Indice:** priorità · epic · dipendenze · refinement

## Scopo e regole

Backlog iniziale per i quattro Sprint. Priorità P0/P1/P2; stime sono ipotesi da
raffinare con il gruppo. Lo stato reale va aggiornato tramite issue e PR, non
retrodatato.

| ID | Priorità | Epic / storia | Owner | Dipendenze | Stato |
|---|---|---|---|---|---|
| B-01 | P0 | Seed, schema, auth e anagrafiche Sprint 1 | M1 | Dati/spec | Implementato; baseline PostgreSQL CI PASS |
| B-02 | P0 | Java 25, CI e contratti condivisi | M1 | JDK 25 | Implementato; CI Java 25/PostgreSQL Sprint 2 PASS |
| B-03 | P1 | Login e anagrafiche web | M2 | B-01, API | Pianificato |
| B-04 | P0 | API backend missione e dettaglio centrale | M1 | B-01, requisiti | Implementato in `main`; PR #3 merged |
| B-05 | P0 | Proposal/accept/reject con timeout e concorrenza | M1 | B-04, DB | Implementato e test PostgreSQL CI PASS |
| B-06 | P0 | Chiusura, pricing e dashboard backend | M1 | B-05, decisioni costo | Implementato e test PostgreSQL CI PASS |
| B-07 | P1 | Chatbot menu guidato e creazione richiesta via API | M3/M1 | API chat, service token | Implementato; verifica titolarità via targa insufficiente per produzione |
| B-08 | P1 | Mappe e route con km stimati/effettivi distinti | M4/M1/M2 | B-04, provider | Pianificato |
| B-09 | P1 | Outbox e notifiche del bot Telegram | M5/M1/M3 | B-05/B-06, canale chat | Implementato per richieste Telegram supportate; prova provider dichiarata in README bot |
| B-10 | P0 | QA integrata, sicurezza e demo | Tutti | B-03–B-09 | Parziale: CI backend PASS; UI, mappe e provider WhatsApp assenti |
| B-11 | P1 | UI browser per centrale e autista | M2 | API missioni/dashboard | Pianificato; non presente nel repository |
| B-12 | P1 | Routing e mappa con km stimati separati dagli effettivi | M4/M1/M2 | Provider e API maps | Pianificato; API `designed` |
| B-13 | P2 | Webhook e provider WhatsApp | M5/M1/M3 | Specifica e credenziali provider | Pianificato; non presente |

### Riconciliazione stato (03/11/2026)

Questa tabella è stata aggiornata contro `main` a `25c613b`, PR #3 e workflow CI
`41651140562`. Le colonne Priorità/Owner/Storia restano stime del backlog preesistente,
non consuntivo o approvazione docente. Il run CI copre backend, OpenAPI e PostgreSQL,
non la UI né provider reali.

## Refinement

Ogni storia deve avere scenario utente, dati/permessi, acceptance criteria,
dipendenze, test, rollback/rischio e owner prima di entrare nello Sprint. Cambi
business o API passano dal [contratto integrato](../team/INTEGRATION_CONTRACTS.md).

**Criterio di completamento:** backlog prioritizzato e nessuna story con blocker
nascosto. **Riferimenti:** [piano Sprint](../08_AGILE_SCRUM.md), [business rules](../05_BUSINESS_RULES.md).

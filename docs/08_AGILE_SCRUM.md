# Agile Scrum — piano di quattro Sprint

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Scrum Master — Membro 1<br>
**Indice:** cadenza · backlog · Sprint 1–4 · rituali · passaggi

## Scopo e attendibilità

Le schede in [`agile/`](./agile/) descrivono un piano adattato a quattro Sprint
giornalieri. Il codice e la storia Git sono avanzati oltre alcune descrizioni del
piano. La tabella sotto riporta l'evidenza tecnica corrente, non verbali o demo
retrospettivi. Per i limiti vedere [il registro Scrum](./agile/README.md).

## Rituali e regole

Planning, daily, review e retrospettiva sono pratiche da usare; non ci sono verbali
completi verificati per ogni giornata. Registrare un evento solo se effettivamente
svolto. Non derivare story point o velocità dai commit.

## Piano

| Sprint/area | Goal | Stato rilevato al 03/11/2026 |
|---|---|---|
| 1 — Fondamenta | DB, autenticazione, API anagrafiche, contratti | Codice e report presenti; CI Java/PostgreSQL latest PASS |
| 2 — Missioni | Workflow, assegnazione, timeout, costo, dashboard | Implementato in `main` dopo PR #3; PostgreSQL CI PASS |
| 3 — Integrazioni | Chatbot, canali, mappe e notifiche | Chatbot Python/API e dispatcher Telegram presenti; prova Telegram dichiarata nel README del modulo; mappe e WhatsApp non implementati |
| 4 — QA/consegna | Verifica dello scope integrato e demo | Qualità backend CI verificata; UI/browser e mappe assenti; completamento Sprint da confermare |

**Dipendenze:** fonti docente, backlog, OpenAPI e disponibilità per chiarimenti.
**Completamento:** review solo con evidenza di esecuzione; schede non equivalgono a verbali.
**Riferimenti:** [Backlog](./agile/PRODUCT_BACKLOG.md),
[template daily](./agile/DAILY_SCRUM_TEMPLATE.md).

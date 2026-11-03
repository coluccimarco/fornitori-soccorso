# Agile Scrum — piano di quattro Sprint

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Scrum Master — Membro 1<br>
**Indice:** cadenza · backlog · Sprint 1–4 · rituali · passaggi

## Scopo

Pianifica quattro giornate di lavoro dei cinque componenti. Solo attività marcate
**IMPLEMENTATO** hanno evidenza nel repository; gli Sprint futuri sono **PIANIFICATI**.
Il dettaglio è in [`agile/`](./agile/).

## Rituali e regole

Ogni giornata: Sprint Planning, brevi sync coordinati, review dimostrabile e
retrospettiva. Ogni story ha owner, dipendenze, acceptance criteria e test. Se una
dipendenza blocca una story, registrare issue e piano alternativo; non simularne il
completamento.

## Piano

| Sprint | Goal | Stato reale / passaggio |
|---|---|---|
| 1 — Fondamenta | DB, autenticazione, API anagrafiche, contratti | Backend/seed/contratto presenti; build Java 25 e test MVC PASS; PostgreSQL 18 locale attivo ma import bloccato da credenziali non configurate; baseline PostgreSQL in CI PASS; UI e onboarding restano task successivi |
| 2 — Missioni | Creazione, proposta, accept/reject, timeout, chiusura, pricing, dashboard | Pianificato; entra solo dopo conferma regole tariffa e setup DB |
| 3 — Integrazioni | Chatbot, mappe/routing, notifiche e idempotenza | Pianificato; usa contratti approvati Sprint 2 |
| 4 — QA | E2E, sicurezza, accessibilità, bugfix e demo | Pianificato; richiede stack integrato e dati di test |

**Dipendenze:** backlog, OpenAPI e disponibilità del docente per ambiguità business.
**Completamento:** review di ogni Sprint con criteri specifici in `agile/SPRINT_n.md`.
**Riferimenti:** [Backlog](./agile/PRODUCT_BACKLOG.md),
[template daily](./agile/DAILY_SCRUM_TEMPLATE.md).

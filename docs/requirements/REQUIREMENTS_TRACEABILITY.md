# Tracciabilità requisiti

**Codice:** FOR-TRACE-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Gli ID `ST-###` sono chiavi di tracciabilità introdotte in questa baseline, non
ID di ticket o user story originali. Modificare la matrice con le fonti docente.

| Requisito | Story baseline | Componente | Endpoint/artefatto | Test / evidenza | Stato |
|---|---|---|---|---|---|
| BR-001 | ST-001 — consultare anagrafiche | Spring Registry/DB | `/autisti`, `/mezzi`, `/compagnie`, `/clienti`, `/officine`; `01_schema.sql` | `ApiSecurityTests`; run CI `41651140562` | IMPLEMENTATO/CI |
| BR-002 | ST-002 — accesso per ruolo | Security/Auth | `/auth/*`; `SecurityConfig` | `ApiSecurityTests`, `ChatApiSecurityTests` | IMPLEMENTATO/CI |
| BR-003 | ST-003 — creare/consultare missione | Mission service | `/missioni`, schema `missions` | `MissionPostgresIntegrationTests` | IMPLEMENTATO/CI |
| BR-004 | ST-004 — proporre e mantenere storico | Mission service | `/proposte`, `mission_proposals` | `MissionPostgresIntegrationTests` | IMPLEMENTATO/CI |
| BR-005 | ST-005 — timeout e concorrenza | Scheduler/service/DB | `MissionExpiryScheduler`, lock/index | `MissionPostgresIntegrationTests` | IMPLEMENTATO/CI |
| BR-006 | ST-006 — chiusura e tariffa | Pricing/Mission | `/complete`, `PricingService` | `PricingServiceTests`, `MissionPostgresIntegrationTests` | IMPLEMENTATO/CI; regola da convalidare |
| BR-007 | ST-007 — rettifica motivata | Mission/audit | `/cost`, `mission_events` | `MissionPostgresIntegrationTests` | IMPLEMENTATO/CI |
| BR-008 | ST-008 — dashboard/storico personale | Mission service | `/dashboard`, `/autista/*` | `MissionPostgresIntegrationTests`, `ApiSecurityTests` | IMPLEMENTATO/CI |
| BR-009 | ST-009 — richiesta da chatbot | Python bot + ChatController | `/chat/lookup-targa`, `/chat/missioni`, `/stato` | `chatbot/tests/test_dialogs.py`, HTTP/backend, `ChatPostgresIntegrationTests` | IMPLEMENTATO/CI; identità limitata |
| BR-010 | ST-010 — notifica Telegram | Outbox + `NotificationDispatcher` | `/chat/notifiche`, `/esito` | `test_notifications.py`, `ChatPostgresIntegrationTests` | PARZIALE; CI non è prova provider reale |
| BR-011 | ST-011 — routing | M4/mappe | `/maps/route` designed | Nessun test runtime nel repo | PIANIFICATO |
| BR-012 | ST-012 — canale WhatsApp | M5/provider | webhook designed | Nessun test runtime nel repo | PIANIFICATO |
| BR-013 | ST-013 — interfaccia web | M2/frontend | Nessun modulo nel repo | Nessun test UI | PIANIFICATO |

## Interpretazione della prova

Il run CI `41651140562` su `25c613b` contiene job Java/OpenAPI e PostgreSQL 18:
entrambi success. Questo prova solo i controlli configurati in
[backend-ci.yml](../../.github/workflows/backend-ci.yml). I test del bot sono test
software; la prova Telegram reale è documentata in `chatbot/README.md`, ma non
riprodotta da questa sessione. I requisiti del docente non possono essere marcati
approvati senza PDF/Excel.

## Lacune

- Nessun ID di requisito del PDF originale e nessuna user story/ticket storico è
  stata verificata; `BR-*` e `ST-*` sono identificativi locali stabili.
- Acceptance di UI, mappe, WhatsApp e identità del richiedente restano DA CONFERMARE.
- Aggiornare la matrice insieme a casi di test identificati e link a run/commit.
- Workbook filtrabile in [`../quality/registri.xlsx`](../quality/registri.xlsx).

# Contratti d'integrazione tra membri

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Integration Lead<br>
**Indice:** source of truth · flussi · ownership · change process · criteri

## Fonte normativa

`api/openapi.yaml` definisce path, request/response e stati. Sprint 1 implementa
auth/sessione/anagrafiche; missioni, dashboard, maps e webhook sono **PIANIFICATI**.
La tabella di dettaglio è in [API_CONTRACT.md](../API_CONTRACT.md).

## Dipendenze

| Da → a | Contratto/dato | Stato |
|---|---|---|
| M2 → M1 | Sessione cookie + CSRF; auth e GET anagrafiche | Implementato |
| M2 → M1 | Mission create, proposal, driver view, dashboard | Pianificato |
| M3 → M1 | Lookup targa e create mission con identità verificata | Lookup centrale implementato; API chatbot non approvata |
| M3 ↔ M5 | Conversazione, callback, provider idempotente | Pianificato |
| M4 → M1/M2 | Coordinate veicolo/origine/officina, route e km suggeriti | coordinate anagrafiche implementate; routing pianificato |
| M1 → M5 | Outbox + `MISSION_ACCEPTED` / `MISSION_COMPLETED` | Tabella predisposta; eventi/worker pianificati |

## Change process

Proponente apre issue con motivo, DTO JSON, auth, errori, compatibilità, test e
consumer. M1 aggiorna OpenAPI; ogni consumer rivede e approva; solo dopo si implementa.
Versionare cambi breaking con coordinamento esplicito. I componenti non interrogano
direttamente le tabelle degli altri.

## Criteri

Contract test/fixture JSON condivise, ruoli verificati server-side, no segreti,
error behavior documentato e feature status aggiornato.<br>
**Dipendenze:** ownership componenti e API YAML.<br>
**Riferimenti:** [Git](../07_GIT_WORKFLOW.md), [backlog](../agile/PRODUCT_BACKLOG.md).

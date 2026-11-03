# Contratti d'integrazione tra membri

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Integration Lead<br>
**Indice:** source of truth · flussi · ownership · change process · criteri

## Fonte normativa

`api/openapi.yaml` definisce path, request/response e stati per operazione. La
baseline integrata contiene auth, anagrafiche, missioni, dashboard, API chatbot
e API mappe; gestione utenti e webhooks in ingresso restano **DESIGNED**. OpenAPI
contiene 34 operation (30 implemented, 4 designed). La tabella di dettaglio è in
[API_SPECIFICATION.md](../architecture/API_SPECIFICATION.md).

## Dipendenze

| Da → a | Contratto/dato | Stato |
|---|---|---|
| M2 → M1 | Sessione cookie + CSRF; auth e GET anagrafiche | Implementato |
| M2 → M1 | Mission create, proposal, driver view, dashboard | API e client statico presenti; browser E2E non eseguito |
| M3 → M1 | Lookup targa e create mission via servizio | API chatbot implementate; la targa non prova identità |
| M3 ↔ M5 | Conversazione, dispatcher Telegram e notifiche | Console/Telegram e notifiche presenti; webhook/WhatsApp pianificati |
| M4 → M1/M2 | Coordinate veicolo/origine/officina, route e km suggeriti | API `/maps/route` implementata e testata in CI; nessuna vista mappa nel client |
| M1 → M5 | Outbox + `MISSION_ACCEPTED` / `MISSION_COMPLETED` | Outbox backend e dispatcher bot presenti; consegna Telegram dichiarata in README bot |

## Provider esterni

| Provider | Flusso | Stato e limite |
|---|---|---|
| Telegram | Bot Python in long polling; notifiche via dispatcher | Implementato; la prova reale è documentata nel README del modulo ma non è coperta dalla CI e non è stata ripetuta in questa revisione |
| Nominatim / OSRM | Backend geocodifica e calcola percorso su richiesta | Client e API implementati/testati; provider pubblici senza disponibilità o rate limit garantiti |
| WhatsApp | Ingresso webhook e notifiche | Non implementato; due operation OpenAPI `designed` |
| Fake backend bot | Dati sintetici per sviluppo/test | Implementato; non sostituisce API o dati runtime |

Il frontend usa API operatori same-origin con sessione/CSRF, ma attualmente non
chiama `/api/v1/maps/route`. Codice e stato OpenAPI non dimostrano un collaudo
browser o una prova live del provider.

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

# Contratto API e integrazioni

**Versione:** 1.2 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — API Owner<br>
**Indice:** implementato · progettato · dipendenze tra membri

La definizione normativa è [../api/openapi.yaml](../api/openapi.yaml). Per ogni
operazione `x-implementation-status` distingue `implemented` da `designed`.
Le sessioni sono server-side; il cookie è HttpOnly. Le richieste mutative usano
CSRF con header `X-CSRF-TOKEN`.

| Metodo / endpoint | Permesso | Payload / risposta | Stato |
|---|---|---|---|
| `GET /api/v1/auth/csrf` | Pubblico | `{token, headerName}` | Implementato |
| `POST /api/v1/auth/login` | Pubblico + CSRF | JSON `{email,password}`; profilo utente, sessione | Implementato |
| `GET /api/v1/auth/me` | Autenticato | Profilo senza password | Implementato |
| `POST /api/v1/auth/logout` | Autenticato + CSRF | `204`; sessione invalidata | Implementato |
| `GET /api/v1/autisti` | RESPONSABILE | Anagrafiche e disponibilità operativa | Implementato |
| `GET /api/v1/mezzi` | RESPONSABILE | Mezzi, basi e coordinate | Implementato |
| `GET /api/v1/compagnie` | RESPONSABILE | Compagnie | Implementato |
| `GET /api/v1/clienti?search=...` | RESPONSABILE | Ricerca per nome o targa | Implementato |
| `POST /api/v1/clienti` | RESPONSABILE + CSRF | Crea cliente; `201`, `400`, `409` | Implementato |
| `GET /api/v1/officine` | RESPONSABILE | Officine e coordinate | Implementato |
| `/api/v1/utenti[/{id}]` | RESPONSABILE | Profili senza credenziali | Progettato |
| `/api/v1/missioni`, `/api/v1/missioni/{id}*` | Centrale/autista secondo operazione | Creazione, proposta, accept/reject, chiusura, rettifica e note | Implementato; CI PostgreSQL PASS |
| `GET /api/v1/dashboard` | RESPONSABILE | Riepilogo storico o live | Implementato; CI PostgreSQL PASS |
| `GET /api/v1/autista/{missioni,storico}` | AUTISTA autenticato | Solo dati dell'autista autenticato | Implementato; CI PostgreSQL PASS |
| `GET /api/v1/maps/route` | Centrale/autista autorizzato | Geocodifica, percorso e km stimati | Implementato |
| `POST /api/v1/webhooks/{telegram,whatsapp}` | Firma/secret del provider | Evento verificato | Progettato |
| `PATCH /api/v1/missioni/{id}/intervento` | RESPONSABILE + CSRF | Corregge tipo e officina prima della proposta | Implementato |
| `POST /api/v1/chat/lookup-targa` | SERVICE_CHATBOT | Riconosce il cliente dalla targa: nome e compagnia | Implementato |
| `POST /api/v1/chat/missioni` | SERVICE_CHATBOT | Creazione idempotente per `requestId` | Implementato |
| `POST /api/v1/chat/stato` | SERVICE_CHATBOT | Stato dell'ultima richiesta aperta della chat | Implementato |
| `GET /api/v1/chat/notifiche`, `POST /api/v1/chat/notifiche/{id}/esito` | SERVICE_CHATBOT | Notifiche da inviare ed esito dell'invio | Implementato |

Per tutte le operazioni protette: `401` indica sessione assente/non valida, `403`
ruolo insufficiente, `400` input non valido, `409` conflitto. I dettagli delle 30
operazioni implementate sono definiti in OpenAPI; i webhook restano
progettati e non devono essere chiamati come funzionalità disponibili.

## Dipendenze tra membri

- **Membro 2 — Frontend:** login JSON (prima recuperare CSRF), sessione/cookie,
  GET anagrafiche, filtro clienti e gestione di `401`/`403`/`409`.
- **Membro 3 — Chatbot:** lookup targa, creazione missione e stato sono implementati
  e autenticati con `FORNITORI_CHAT_TOKEN`; il cliente si riconosce dalla sola targa
  ([D-007](./DECISION_LOG.md)).
- **Membro 4 — Mappe:** `GET /api/v1/maps/route` converte l'indirizzo in coordinate e
  restituisce percorso GeoJSON, punti e km; la visualizzazione Leaflet è da fare nella UI.
- **Membro 5 — Notifiche:** gli eventi `MISSION_ACCEPTED` e `MISSION_COMPLETED`
  vengono scritti nell'outbox in modo transazionale; il dispatcher/provider restano da implementare.

Ogni cambio di payload o permesso deve aggiornare OpenAPI e coordinarsi con i
consumatori prima dell'integrazione.

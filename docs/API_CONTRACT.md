# Contratto API e integrazioni

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — API Owner<br>
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
| `/api/v1/missioni`, `/api/v1/missioni/{id}*` | Centrale/autista secondo operazione | Creazione, proposta, accept/reject, chiusura, rettifica e note | Progettato per Sprint 2 |
| `GET /api/v1/dashboard` | RESPONSABILE | Riepilogo storico o live | Progettato |
| `GET /api/v1/autista/{missioni,storico}` | AUTISTA autenticato | Solo dati dell'autista autenticato | Progettato |
| `GET /api/v1/maps/route` | Centrale/autista autorizzato | Coordinate, percorso e km stimati | Progettato |
| `POST /api/v1/webhooks/{telegram,whatsapp}` | Firma/secret del provider | Evento verificato | Progettato |
| `/api/v1/chat/lookup-targa`, `/api/v1/chat/missioni` | SERVICE_CHATBOT + challenge | Lookup minimizzato e creazione idempotente | Progettato |
| `GET /api/v1/chat/missioni/{id}/stato` | SERVICE_CHATBOT + token verifica | Stato minimizzato, no accesso cross-customer | Progettato |

Per tutte le operazioni protette: `401` indica sessione assente/non valida, `403`
ruolo insufficiente, `400` input non valido, `409` conflitto. Le API missioni e le
integrazioni presenti in OpenAPI non sono implementate e non devono essere chiamate
come se fossero disponibili.

## Dipendenze tra membri

- **Membro 2 — Frontend:** login JSON (prima recuperare CSRF), sessione/cookie,
  GET anagrafiche, filtro clienti e gestione di `401`/`403`/`409`.
- **Membro 3 — Chatbot:** lookup targa, creazione missione e stato hanno ora route
  progettate, non funzionanti. Usano token service e challenge; il metodo challenge
  richiede approvazione prima della delivery per prevenire enumerazione e accessi
  cross-customer.
- **Membro 4 — Mappe:** basi mezzi, coordinate officine e campi coordinate missione
  sono nel modello; route esterno e distanze non sono ancora implementati.
- **Membro 5 — Notifiche:** `notifications_outbox` è predisposta; gli eventi
  `MISSION_ACCEPTED` e `MISSION_COMPLETED` e il dispatcher sono da implementare.

Ogni cambio di payload o permesso deve aggiornare OpenAPI e coordinarsi con i
consumatori prima dell'integrazione.

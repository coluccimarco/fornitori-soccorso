# Contratti d'integrazione tra membri

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)<br>
**Indice:** fonte normativa · dipendenze · provider · fuori ambito · cambi

## Fonte normativa

`api/openapi.yaml` definisce path, request, response e stati per operazione. Contiene
38 operazioni: 34 `implemented` e 4 `designed` (gestione utenti e webhook, fuori
ambito). La mappa delle superfici è in
[API_SPECIFICATION.md](../architecture/API_SPECIFICATION.md).

Gli errori hanno la forma `{status, error, message}`: `error` è un codice stabile,
`message` è un testo per l'utente scritto dal servizio, presente su alcuni 400, 404,
409 e 422.

## Dipendenze

| Da → a | Contratto o dato | Stato |
|---|---|---|
| Frontend → Backend | Sessione cookie + CSRF; auth e anagrafiche | Implementato |
| Frontend → Backend | Missioni, proposte, storico proposte, correzione intervento, rettifica costo, dashboard, avvisi, area autista | Implementato |
| Frontend → Mappe | `GET /api/v1/maps/route` con header CSRF, mappa Leaflet e km proposti alla chiusura | Implementato |
| Chatbot → Backend | `/api/v1/chat/lookup-targa`, `/missioni`, `/stato` con token di servizio | Implementato; la sola targa è un limite noto |
| Chatbot ↔ Notifiche | `POST /api/v1/chat/notifiche/claim` e `/{id}/esito` | Implementato per Telegram |
| Backend → Notifiche | Outbox con `MISSION_ACCEPTED` e `MISSION_COMPLETED` nella transazione di dominio | Implementato |

## Regole operative

- **Lookup chat:** un lookup positivo autorizza la creazione per 15 minuti, con stessa
  sessione e stesso cliente. Oltre questo limite il backend risponde 403 e il bot rifà
  il lookup una volta, quindi prosegue. Un `requestId` riusato con dati diversi dà 409.
- **Notifiche:** `claim` porta le righe da `PENDING` a `SENDING` in modo atomico. Una
  riserva senza esito scade dopo 5 minuti, un invio fallito riprova dopo 30 secondi,
  dopo 5 tentativi la riga passa a `FAILED`.
- **Mappe:** la `GET` richiede `X-CSRF-TOKEN` perché può salvare coordinate e km stimati.

## Provider esterni

| Provider | Flusso | Limite |
|---|---|---|
| Telegram | Bot Python in long polling; notifiche dal dispatcher | Consegna almeno una volta in caso di crash |
| Nominatim / OSRM | Il backend geocodifica e calcola il percorso su richiesta | Servizi pubblici senza disponibilità garantita |

## Fuori ambito

| Elemento | Decisione |
|---|---|
| WhatsApp | Non integrato; `whatsapp` resta solo un valore del canale di origine |
| Webhook in ingresso | Non integrati; il bot usa il long polling |
| Gestione utenti | Non esposta: un utente autista richiede anche autista e mezzo dedicati; gli account arrivano dal seed |
| Verifica dell'identità del cliente | Non prevista nella demo: la sola targa è un limite noto |

## Processo di cambio

Chi propone apre una issue con motivo, DTO JSON, auth, errori e consumer coinvolti.
Il Membro 1 aggiorna OpenAPI, ogni consumer rivede, poi si implementa. I componenti
non interrogano direttamente le tabelle degli altri.

**Riferimenti:** [Git](../07_GIT_WORKFLOW.md), [backlog](../agile/PRODUCT_BACKLOG.md).

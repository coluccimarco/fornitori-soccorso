# Specifica API

**Codice:** FOR-API-001 · **Versione:** 1.1 · **Data:** 03/11/2026
**Baseline:** `main` / `f763d53`

La fonte normativa è [`api/openapi.yaml`](../../api/openapi.yaml); questa pagina
offre una mappa, non replica i payload.

## Stato rilevato

Il documento YAML contiene 34 operazioni: 30 con
`x-implementation-status: implemented` e 4 `designed`. Le operation `designed`
coprono gestione utenti (2) e webhook in ingresso Telegram/WhatsApp (2). L'API
mappe è implementata nel backend; la UI web non la invoca. Lo status dichiara la
presenza runtime del contratto, non certifica una verifica end-to-end o un
provider esterno reale.

## Superfici

| Superficie | Authn/authz | Operazioni principali | Stato |
|---|---|---|---|
| `/api/v1/auth/*` | Sessione, CSRF | login/me/logout/csrf | Implementato |
| `/api/v1/{autisti,mezzi,compagnie,clienti,officine}` | RESPONSABILE; cookie e CSRF sulle mutazioni | anagrafiche e creazione cliente | Implementato |
| `/api/v1/missioni/*` | Centrale o AUTISTA assegnato secondo use case | consultazione, proposta, accept/reject, complete, rettifica, note/intervento | Implementato |
| `/api/v1/dashboard`, `/autista/*` | RESPONSABILE o proprio AUTISTA | report e storico personale | Implementato |
| `/api/v1/chat/*` | `SERVICE_CHATBOT` bearer token, stateless | lookup targa, create, stato, outbox/esito | Implementato |
| `/api/v1/maps/route` | Sessione e ruolo/ownership missione-driver | geocodifica opzionale, percorso e km stimati | Implementato; provider esterni non garantiti |
| `/api/v1/utenti*`, `/api/v1/webhooks/*` | Ruoli previsti | utenti, ingressi provider | Designed |

## Regole di consumo

Per contratti aggiornati consultare OpenAPI e verificare `x-required-role`,
`security`, status e risposta errore. Sessione operatore: token CSRF su mutazioni.
Chatbot: `SERVICE_CHATBOT` tramite `FORNITORI_CHAT_TOKEN`; credenziale assente o
non valida viene rifiutata. Il client non deve considerare un endpoint `designed`
disponibile solo perché definito nello YAML.

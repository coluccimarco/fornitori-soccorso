# Specifica API

**Codice:** FOR-API-001 · **Versione:** 1.0 · **Data:** 03/11/2026
La fonte normativa è [`api/openapi.yaml`](../../api/openapi.yaml); questa pagina
offre una mappa, non replica i payload.

## Stato rilevato

Al commit `25c613b` il documento YAML contiene 34 operazioni: 29 con
`x-implementation-status: implemented` e 5 `designed`. Le 5 designed coprono
gestione utenti (2), mappe (1) e webhooks in ingresso Telegram/WhatsApp (2).
La description generale YAML è rimasta indietro e dice che mission/chat/dashboard
sono progettati: in caso di conflitto fanno fede controller/codice/test per lo
stato runtime e gli schemi per la forma del contratto. Il contract owner deve
aggiornare la description, senza togliere la distinzione `implemented/designed`.

## Superfícies

| Superficie | Authn/authz | Operazioni principali | Stato |
|---|---|---|---|
| `/api/v1/auth/*` | Sessione, CSRF | login/me/logout/csrf | Implementato |
| `/api/v1/{autisti,mezzi,compagnie,clienti,officine}` | RESPONSABILE; cookie e CSRF sulle mutazioni | anagrafiche e creazione cliente | Implementato |
| `/api/v1/missioni/*` | Centrale o AUTISTA assegnato secondo use case | consultazione, proposta, accept/reject, complete, rettifica, note/intervento | Implementato |
| `/api/v1/dashboard`, `/autista/*` | RESPONSABILE o proprio AUTISTA | report e storico personale | Implementato |
| `/api/v1/chat/*` | Bearer service token, stateless | lookup targa, create, stato, outbox/esito | Implementato |
| `/api/v1/maps/route` | Ruolo previsto e ownership driver | richiesta itinerario | Designed |
| `/api/v1/utenti*`, `/api/v1/webhooks/*` | Ruoli previsti | utenti, ingressi provider | Designed |

## Regole di consumo

Per contratti aggiornati consultare OpenAPI e verificare `x-required-role`,
`security`, status e risposta errore. Sessione operatore: token CSRF su mutazioni.
Chatbot: Bearer token da `FORNITORI_CHAT_TOKEN`; assenza/invalidità rifiutata.
Il client non deve considerare un endpoint `designed` disponibile perché definito
nello YAML.

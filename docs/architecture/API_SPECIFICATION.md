# Specifica API

**Codice:** FOR-API-001 · **Versione:** 2.1 · **Data:** 05/11/2026

La fonte normativa è [`api/openapi.yaml`](../../api/openapi.yaml); questa pagina offre una mappa,
non replica i payload.

## Stato

Il documento contiene 42 operazioni: 38 `implemented`, che coincidono con i mapping dei 6
controller, e 4 `designed`, fuori ambito: gestione utenti (2) e webhook Telegram e WhatsApp (2).

## Superfici

| Superficie | Autenticazione e ruolo | Operazioni principali | Stato |
|---|---|---|---|
| `/api/v1/auth/*` | Sessione, CSRF | login, me, logout, csrf | Implementato |
| `/api/v1/{autisti,mezzi,compagnie,clienti,officine}` | RESPONSABILE; CSRF sulle mutazioni | anagrafiche e creazione cliente | Implementato |
| `/api/v1/missioni/*` | Centrale o AUTISTA assegnato secondo il caso d'uso | consultazione, proposta, accept/reject, complete, rettifica, note e supporto gestito, intervento, annullamento, verifica polizza | Implementato |
| `/api/v1/missioni/{id}/proposte` (GET) | RESPONSABILE | storico proposte in sola lettura | Implementato |
| `/api/v1/missioni/{id}/proposta-automatica`, `/api/v1/centrale/avvisi` | RESPONSABILE | proposta al più vicino libero, avvisi su rifiuti, scadenze e supporto | Implementato |
| `/api/v1/dashboard`, `/autista/*` | RESPONSABILE o proprio AUTISTA | dashboard operativa e costi, storico personale, dettaglio missione aperta, disponibilità | Implementato |
| `/api/v1/chat/*` | Token bearer `SERVICE_CHATBOT`, stateless | lookup targa, creazione, stato, riserva notifiche ed esito | Implementato |
| `/api/v1/maps/route` | Sessione e CSRF; ruolo e proprietà della missione | geocodifica, percorso e km stimati | Implementato |
| `/api/v1/utenti*`, `/api/v1/webhooks/*` | — | utenti, ingressi provider | Fuori ambito |

## Regole di consumo

Verificare in OpenAPI `x-required-role`, `security`, status e risposta d'errore. Le mutazioni di
sessione richiedono il CSRF, come la `GET` delle mappe, che può salvare dati. Il chatbot usa
`FORNITORI_CHAT_TOKEN`: credenziale assente o errata dà `401`.

## Errori

Gli errori hanno la forma `{status, error, message}`. `error` è un codice stabile (`conflict`,
`not_found`, `invalid_request`…); `message` è un testo per l'utente scritto dal servizio, presente
su alcuni 400, 404, 409 e 422. Non contengono mai eccezioni o SQL.

Il corpo delle richieste ha un limite di 64 KiB (413) e deve dichiarare la lunghezza (411). Limiti dei campi: nome cliente 150, cognome 100, telefono 32, targa 20, modello e città 150, polizza 100; km e
importi `NUMERIC(10,2)`. Il bearer del chatbot è opaco, non un JWT.

# Functional Design — FORNITORI

**Codice:** FOR-FUD-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Responsabile:** M1–M5 per componente · **Stato:** design e runtime distinti per scenario
**Fonte API normativa:** [`api/openapi.yaml`](../../api/openapi.yaml)

## Maturità funzionale

Le API backend per auth, anagrafiche, missioni, dashboard e chatbot sono presenti;
il chatbot Python supporta console/Telegram. Frontend statico e API routing sono
presenti; la vista cartografica non è integrata. L'operatività Telegram e le notifiche su provider sono dichiarate
nel README del modulo; la CI automatizza test software ma non il servizio Telegram.

**Attori:** RESPONSABILE, AUTISTA autenticato, cliente via bot, servizio chatbot,
dispatcher notifiche. Due scenari browser con PostgreSQL isolato sono verificati
nel rapporto BACKEND_E2E_AUDIT; la copertura UX completa resta da approvare.

## UC-001 — Accesso operatore

**Attore:** responsabile o autista · **Scopo:** instaurare sessione con ruolo.
**Prerequisiti:** utente applicativo configurato. **Trigger:** richiesta di login.
**Flusso principale:** il client ottiene CSRF → invia login JSON con token →
backend autentica e imposta sessione → client legge identità/ruolo.
**Alternativi/eccezioni:** credenziale non valida `401`; CSRF mancante/non valido;
sessione revocata al logout; accesso ruolo non autorizzato `403`.
**Postcondizione:** sessione valida o rifiuto senza accesso.
**API:** `/api/v1/auth/csrf`, `/login`, `/me`, `/logout`.
**Acceptance/test:** cookie/CSRF, login, logout, `401/403` in `ApiSecurityTests`.
**Stato:** IMPLEMENTATO, test backend in CI.

## UC-002 — Consultare anagrafiche e ricercare cliente

**Attore:** RESPONSABILE · **Scopo:** leggere clienti, driver, mezzi, compagnie,
officine; filtrare clienti per nome/targa. **Prerequisiti:** sessione RESPONSABILE.
**Trigger:** richiesta di consultazione.
**Flusso:** il client chiama la risorsa → il service applica filtri/ruoli →
restituisce DTO senza credenziali.
**Alternativi/eccezioni:** sessione assente `401`, ruolo errato `403`, risultato
vuoto restituito senza dati inventati.
**Postcondizione:** dati consultati; nessuna modifica.
**API:** `GET /api/v1/{autisti,mezzi,compagnie,clienti,officine}`.
**Acceptance/test:** ricerca nome e targa normalizzata, risposte e permessi.
**Stato:** IMPLEMENTATO.

## UC-003 — Inserire cliente

**Attore:** RESPONSABILE · **Prerequisiti:** sessione e CSRF.
**Trigger:** inserimento anagrafica. **Flusso:** inviare dati validati → service
controlla duplicati/compagnia → persiste → restituisce DTO. **Alternativi:**
validazione `400`, conflitto duplicato `409`, `401/403`.
**Postcondizione:** cliente persistito una volta. **API:** `POST /api/v1/clienti`.
**Test:** test Registry, validazione e conflitto identificati nei test backend.
**Stato:** IMPLEMENTATO.

## UC-004 — Creare missione e dettaglio

**Attore:** RESPONSABILE o servizio chatbot autenticato. **Scopo:** registrare
richiesta live. **Prerequisiti:** cliente noto e parametri validi; traino richiede
officina secondo DB. **Trigger:** richiesta di soccorso.
**Flusso centrale:** validare dati → assegnare codice/stato iniziale `nuova` →
persistenza transazionale → esporre id/dettaglio. **Flusso chatbot:** lookup
limitato, dialogo guidato, creazione tramite API servizio.
**Eccezioni:** token servizio assente/errato, input errato, rate-limit/duplicato,
cliente inesistente, provider non disponibile; nessuna missione falsa.
**Postcondizione:** missione live salvata e origine/canale registrati.
**API:** `POST /api/v1/missioni`; `POST /api/v1/chat/missioni`;
`POST /api/v1/chat/lookup-targa`.
**Test:** `MissionPostgresIntegrationTests`, `ChatPostgresIntegrationTests`,
`ChatApiSecurityTests`, `chatbot/tests/test_http_backend.py`.
**Stato:** IMPLEMENTATO; verifica titolarità targa insufficiente per uso reale.

## UC-005 — Proporre una missione ad autista

**Attore:** RESPONSABILE · **Prerequisiti:** missione live proponibile e driver
operativamente disponibile. **Trigger:** assegnazione.
**Flusso:** lock/verifica → registra proposta e scadenza basata su configurazione →
notifica/opportunità al driver API. Storico delle proposte conservato.
**Eccezioni:** driver non disponibile/conflitto, missione non compatibile, ruolo
errato; non sovrascrivere una proposta precedente.
**Postcondizione:** stato proposta e deadline persistiti.
**API:** `POST /api/v1/missioni/{id}/proposte`.
**Test:** `MissionPostgresIntegrationTests` include flusso, concorrenza e scadenze.
**Stato:** IMPLEMENTATO, CI PostgreSQL.

## UC-006 — Accettare, rifiutare o far scadere proposta

**Attore:** AUTISTA destinatario; scheduler per scadenza. **Prerequisiti:** proposta
corrente e sessione con identità assegnata.
**Flusso principale:** il driver risponde prima della deadline → service controlla
owner/stato/tempo → accetta o registra rifiuto. Scadenza porta a `scaduta`; centrale
può riproporre mantenendo storico.
**Eccezioni:** accettazione a o dopo deadline respinta; chiamata da altro autista,
duplicato o transizione impossibile respinti. Scheduler periodico opera sulle sole
missioni live.
**Postcondizione:** esito e timeline aggiornati in modo transazionale.
**API:** `/accept`, `/reject`, `/proposte`.
**Test:** test mission service e integrazione PostgreSQL; boundary del Clock e concorrenza.
**Stato:** IMPLEMENTATO.

## UC-007 — Segnalazione e chiusura missione

**Attore:** AUTISTA assegnato; RESPONSABILE può aggiornare il tipo d'intervento.
**Prerequisiti:** missione assegnata/accettata; km effettivi validi al completamento.
**Flusso:** (eventuale) segnalazione autorizzata → invio km effettivi → service
valida stato e calcola costo → completa e crea evento/outbox.
**Alternativi:** missione non chiudibile, km invalidi, ruolo/driver non assegnato;
nessun esito OK falso. **API:** `/intervento`, `/complete`, `/notes`.
**Test:** test PostgreSQL missione, note e completamento.
**Stato:** API backend e UI note/chiusura presenti e verificate nel browser;
la modifica intervento rimane disponibile solo via API.

## UC-008 — Calcolare o rettificare costo

**Attore:** service per calcolo; RESPONSABILE per rettifica.
**Prerequisiti:** missione completabile, km effettivi, configurazione pricing.
**Flusso:** tariffa server-side da configurazione; eventuale rettifica richiede
permesso e motivo e conserva costo calcolato e audit.
**Alternativi:** dati incompleti o ruolo errato; non fidarsi di un totale inviato dal
client. **API:** `/complete`, `PATCH /cost`.
**Regola implementata nel servizio:** tariffa base 50 + 1,20/km, supplemento 25%
20:00–08:00, arrotondamento `HALF_UP`, timezone `Europe/Rome`.
**Acceptance/test:** `PricingServiceTests` e test integrazione. Conferma docente sul
timestamp di riferimento rimane DA CONFERMARE.

## UC-009 — Consultare dashboard e guadagni

**Attore:** RESPONSABILE; AUTISTA per i propri dati. **Prerequisiti:** sessione
valida. **Flusso:** aggregare missioni secondo filtro/modo reference-live; endpoint
autista limita l'identità all'utente autenticato. **Eccezioni:** `401/403`,
risultato vuoto; mai includere altri autisti nello storico personale.
**API:** `GET /api/v1/dashboard`, `/api/v1/autista/missioni`,
`/api/v1/autista/storico`. **Test:** `MissionPostgresIntegrationTests`,
`ApiSecurityTests`. **Stato:** API e client web presenti; 2 scenari Chromium/PostgreSQL PASS locali (04/11).

## UC-010 — Conversazione cliente e consultazione stato

**Attore:** cliente via bot Telegram/console. **Prerequisiti:** runtime bot; token
backend configurato per backend HTTP; canale Telegram avviato.
**Flusso:** invio targa → richiesta informazioni limitate → condivisione posizione/
indirizzo → descrizione problema → riepilogo e creazione → comando `STATO` per
risorsa collegata alla chat. `AIUTO`/`ANNULLA` sono comandi del dialogo.
**Eccezioni:** targa non trovata (limite tentativi), formato non valido, rate limit,
servizio offline, sessione in RAM scaduta/riavvio. La targa non prova identità.
**API:** `/api/v1/chat/lookup-targa`, `/missioni`, `/stato`.
**Test:** `chatbot/tests/test_dialogs.py`, `ChatApiSecurityTests`,
`ChatPostgresIntegrationTests`.
**Stato:** IMPLEMENTATO in console/Telegram, con limitazioni note; non equivale a UI web.

## UC-011 — Ricevere notifiche di presa in carico e completamento

**Attori:** backend/outbox e dispatcher chatbot; destinatario Telegram per richiesta
supportata. **Prerequisiti:** evento accettazione/completamento e dispatcher attivo.
**Flusso:** evento outbox transazionale → bot legge coda Telegram → invia → registra
esito; eventi rimangono pendenti se bot offline, secondo comportamento descritto nel README.
**Eccezioni:** provider offline, errore temporaneo, duplicati; non dichiarare l'invio
universale per missioni senza chat associata. **Test:** `test_notifications.py`,
`ChatPostgresIntegrationTests`, `ChatApiSecurityTests`.
**Stato:** PARZIALE/IMPLEMENTATO per il canale Telegram documentato; WhatsApp e CI
non verificano il provider reale.

## UC-012 — Routing cartografico

**Attore:** autista/centrale. **Flusso progettato:** base mezzo → luogo intervento →
eventuale officina per traino; riparazione sul posto omette officina. Distanza
stimata non equivale ai km effettivi.
**API:** `GET /api/v1/maps/route`, metadata `implemented`; backend usa Nominatim
per geocodifica opzionale e OSRM per route/km stimati. **Acceptance/test:**
`MapProviderClientsTests` e `MapPostgresIntegrationTests` sono inclusi nella CI;
provider live e vista cartografica browser sono NOT RUN/non integrati.
**Stato:** API IMPLEMENTATA; UI cartografica DA FARE se richiesta.

## Diagrammi e confini

Il client browser è presente, ma la vista mappa non è integrata. I diagrammi Mermaid
distinguono contesto runtime, componenti presenti e confini futuri:
[`diagrams/`](../diagrams/). Regole di stato persistite: schema SQL e service
implementati; i flussi UI restano design, non runtime.

# Functional Design — FORNITORI

**Codice:** FOR-FUD-001 · **Versione:** 2.0 · **Data:** 04/11/2026
**Responsabili:** team per componente · **Fonte API normativa:** [`api/openapi.yaml`](../../api/openapi.yaml)

**Attori:** RESPONSABILE, AUTISTA autenticato, cliente via bot, servizio chatbot, dispatcher
notifiche.

## UC-001 — Accesso operatore

**Attore:** responsabile o autista. **Prerequisiti:** utente applicativo configurato.
**Flusso:** il client ottiene il CSRF → invia il login JSON con il token → il backend autentica
e imposta la sessione → il client legge identità e ruolo.
**Eccezioni:** credenziali non valide `401`; CSRF mancante `403`; sessione revocata al logout;
ruolo non autorizzato `403`; troppi tentativi `429`.
**API:** `/api/v1/auth/csrf`, `/login`, `/me`, `/logout`. **Stato:** IMPLEMENTATO.

## UC-002 — Consultare anagrafiche e cercare un cliente

**Attore:** RESPONSABILE. **Flusso:** il client chiama la risorsa → il service applica filtri
e ruoli → restituisce DTO senza credenziali.
**Eccezioni:** `401`, `403`; un risultato vuoto non inventa dati.
**API:** `GET /api/v1/{autisti,mezzi,compagnie,clienti,officine}`. **Stato:** IMPLEMENTATO.

## UC-003 — Inserire un cliente

**Attore:** RESPONSABILE. **Flusso:** dati validati → controllo duplicati e compagnia →
persistenza → DTO. **Eccezioni:** validazione `400`; targa duplicata `409` con messaggio
"Targa già presente"; `401/403`. **API:** `POST /api/v1/clienti`. **Stato:** IMPLEMENTATO.

## UC-004 — Creare una missione

**Attore:** RESPONSABILE o servizio chatbot. **Prerequisiti:** cliente noto; il traino richiede
un'officina. **Flusso centrale:** validazione → codice e stato `nuova` → persistenza. **Flusso
chatbot:** lookup, dialogo guidato, creazione tramite API servizio.
**Eccezioni:** token errato, input errato, rate limit, duplicato, cliente inesistente, provider
non disponibile; nessuna missione falsa.
**API:** `POST /api/v1/missioni`, `POST /api/v1/chat/missioni`, `POST /api/v1/chat/lookup-targa`.
**Stato:** IMPLEMENTATO; la sola targa è un limite noto della demo.

## UC-005 — Proporre una missione a un autista

**Attore:** RESPONSABILE. **Prerequisiti:** missione proponibile e autista disponibile.
**Flusso:** lock e verifica → proposta e scadenza da configurazione → l'autista la vede nella sua
area entro 15 secondi. Con "al più vicino" sceglie il sistema, in linea d'aria, escludendo chi
ha già risposto. **Eccezioni:** autista occupato o stato incompatibile `409`.
**API:** `POST /api/v1/missioni/{id}/proposte`, `POST /api/v1/missioni/{id}/proposta-automatica`.
**Stato:** IMPLEMENTATO.

## UC-006 — Accettare, rifiutare o far scadere una proposta

**Attore:** AUTISTA destinatario; scheduler per la scadenza. **Flusso:** l'autista risponde prima
della deadline → il service controlla destinatario, stato e tempo. Rifiuto e scadenza generano
un avviso per la centrale, che può riproporre mantenendo lo storico.
**Eccezioni:** risposta a o dopo la deadline `409`; altro autista `403`.
**API:** `/accept`, `/reject`, `GET /api/v1/centrale/avvisi`. **Stato:** IMPLEMENTATO.

## UC-007 — Segnalazione, correzione e chiusura

**Attore:** AUTISTA assegnato; RESPONSABILE per correggere intervento e officina.
**Flusso:** eventuale segnalazione → km effettivi → il service valida e calcola il costo →
completa e crea evento e outbox. La centrale corregge tipo e officina dalla scheda missione,
finché la missione è nuova, rifiutata o scaduta.
**Eccezioni:** missione non chiudibile, km non validi, ruolo errato; una nota su una missione
dello storico dà `409`. **API:** `/intervento`, `/complete`, `/notes`. **Stato:** IMPLEMENTATO.

## UC-008 — Calcolare o rettificare il costo

**Attore:** service per il calcolo; RESPONSABILE per la rettifica, dalla scheda missione.
**Regola:** base 50 + 1,20/km, supplemento 25% dalle 20:00 alle 08:00, `HALF_UP`, `Europe/Rome`.
La rettifica richiede un motivo e conserva costo calcolato e audit.
**API:** `/complete`, `PATCH /cost`. **Stato:** IMPLEMENTATO; timestamp di riferimento da
confermare con il docente.

## UC-009 — Consultare dashboard e guadagni

**Attore:** RESPONSABILE; AUTISTA per i propri dati. **Flusso:** aggregati per modalità reference
o live; l'endpoint autista si limita all'utente autenticato.
**API:** `GET /api/v1/dashboard`, `/api/v1/autista/missioni`, `/api/v1/autista/storico`.
**Stato:** IMPLEMENTATO.

## UC-010 — Conversazione cliente e stato

**Attore:** cliente via Telegram o console. **Flusso:** targa → nome e compagnia → posizione o
indirizzo → intervento → problema → creazione. `STATO` risponde per la chat con una frase
comprensibile; `AIUTO` e `ANNULLA` sono comandi del dialogo.
**Eccezioni:** targa non trovata (limite tentativi), formato non valido, rate limit, servizio non
disponibile. Se il riconoscimento della targa scade (15 minuti), il bot lo rinnova da solo.
**API:** `/api/v1/chat/lookup-targa`, `/missioni`, `/stato`. **Stato:** IMPLEMENTATO.

## UC-011 — Notifiche di presa in carico e completamento

**Attori:** backend e dispatcher del bot. **Flusso:** evento outbox transazionale → il bot riserva
le righe → invia → registra l'esito. Se il bot è spento, i messaggi restano in coda.
**Eccezioni:** provider non disponibile (nuovo tentativo dopo 30 secondi, massimo 5); missioni
della centrale senza invio (`SKIPPED`).
**API:** `POST /api/v1/chat/notifiche/claim`, `POST /api/v1/chat/notifiche/{id}/esito`.
**Stato:** IMPLEMENTATO per Telegram; WhatsApp fuori ambito.

## UC-012 — Percorso e mappa

**Attore:** autista e centrale. **Flusso:** base mezzo → luogo intervento → eventuale officina per
il traino. La distanza stimata non equivale ai km effettivi e viene proposta alla chiusura.
**API:** `GET /api/v1/maps/route`. **Stato:** IMPLEMENTATO, con mappa Leaflet nella UI.

## UC-013 — Storico proposte

**Attore:** RESPONSABILE. **Flusso:** dalla scheda missione "Mostra storico proposte" elenca
autista, esito e date di ogni tentativo, anche per le missioni dello storico di riferimento.
**Eccezioni:** `401`, `403`, `404`. **API:** `GET /api/v1/missioni/{id}/proposte`.
**Stato:** IMPLEMENTATO.

## Diagrammi

Le sequenze e gli stati sono in [`diagrams/`](../diagrams/).

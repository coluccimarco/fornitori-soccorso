# Membro 2 — Frontend

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 2<br>
**Indice:** obiettivo · stack · schermate · API · test · acceptance

## Scopo

Costruire UI centrale e area autista usando REST; non duplicare assegnazione,
autorizzazioni o tariffa. Il frontend non è presente nel repository (**PIANIFICATO**).

## Stack e struttura proposta

HTML/CSS/JavaScript/Bootstrap sono la proposta del corso; confermare framework prima
di scaffoldare. Suggerimento: `frontend/src/{api,components,pages,auth,styles}`,
`frontend/tests`, README con setup. Centralizza base URL, sessione e gestione errori.

## Schermate e flussi

1. Login e logout; recupera CSRF e sessione.
2. Dashboard centrale con conteggi e filtri.
3. Anagrafiche: autisti, mezzi, compagnie, clienti, officine; ricerca cliente.
4. Creazione missione e dettaglio, proposta, countdown e storico.
5. Area autista: missione proposta, accept/reject, dettaglio, chiusura e segnalazione.
6. Storico/guadagni e vista mappa solo quando i relativi endpoint saranno attivi.

Sprint 1 accessibile oggi: login e GET anagrafiche implementati. Tutte le schermate
missione/dashboard sono **PIANIFICATE**; non simulare dati come se provenissero dal
server. Quando backend pronto, consumare route `/api/v1` da
[`api/openapi.yaml`](../../api/openapi.yaml).

## Sicurezza, UX e test

Invia cookie sessione con richieste same-origin/credentials e `X-CSRF-TOKEN` alle
mutazioni. Gestisci `401` ritornando al login, `403`, `400`, `409`, loading, empty,
retry esplicito e responsive. Non persistere password/token oltre sessione; evitare
XSS inserendo testo come text node. Testare form, focus/accessibilità, viewport,
errori rete e permessi.

**Dipendenze:** M1 auth/CSRF e API; M4 mappe; Sprint 2 mission workflow.
**Acceptance:** nessun hardcode che finga risultati, flussi da browser testati,
screen responsive e review M1. **Risultato Sprint successivo:** shell navigabile,
login e anagrafiche collegate; mission screen attivabili con endpoint validati.
**Riferimenti:** [contratti](./INTEGRATION_CONTRACTS.md), [API](../04_API_REST.md).

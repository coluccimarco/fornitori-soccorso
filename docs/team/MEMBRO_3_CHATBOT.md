# Membro 3 — Chatbot a menu guidato

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 3<br>
**Indice:** obiettivo · conversazione · API · privacy · test

## Scopo e maturità

Implementare un flusso deterministico a menu, senza dipendenza obbligatoria da AI.
Dialogo in [`chatbot/`](../../chatbot/README.md) provato con backend finto
(**DA VALIDARE** sul backend reale); accesso chatbot a mission create/status è
**DA PROGETTARE/APPROVARE** prima del collegamento.

## Dialogo

Stati proposti: `START → WAIT_PLATE → VERIFY_CUSTOMER → WAIT_LOCATION →
WAIT_PROBLEM → CONFIRM → CREATE_REQUEST → SHOW_STATUS → END`, con `HELP`, `CANCEL`
e fallback input in ogni stato. Conservare stato e contesto minimo, scadenza
conversazione e numero tentativi; non memorizzare PII non necessaria.

1. Avvio, spiegazione e consenso.
2. Richiesta targa; normalizzazione e lookup autorizzato.
3. Verifica titolarità con challenge non prevedibile (non affidarsi alla sola targa).
4. Raccolta posizione e problema; riepilogo/consenso.
5. Richiesta di creare missione nuova; conferma solo dopo risposta backend.
6. Consultazione stato solo dopo verifica del proprietario, mostra nome autista
   solo se autorizzato.
7. Errori: input non valido, lookup multiplo, servizio indisponibile, rate limit;
   nessun dato di altri clienti in risposta.

## API e integrazione

Ricerca cliente `GET /api/v1/clienti?search=...` è oggi API centrale **IMPLEMENTATA**,
non deve essere esposta pubblicamente come lookup anonimo. `POST /api/v1/missioni`,
stato e webhook sono **PIANIFICATI** in OpenAPI. M3 concorda con M1 una credenziale
service-to-service, autorizzazione, minimizzazione output e idempotency key prima
del collegamento. Il bot non accede al DB.

Esempio interno futuro, non contratto definitivo:

```json
{"sessionId":"opaque-id","state":"WAIT_LOCATION","input":"Molfetta, via Roma"}
```

Testare DialoghiChatbot: happy path, targa ignota, challenge errata, input
sconosciuto, cancel, timeout, duplicate callback, mission non autorizzata, provider
offline. Sequenza prevista in [`diagrams/chatbot_sequence.mmd`](../diagrams/chatbot_sequence.mmd).

**Dipendenze:** M1 auth/lookup/mission create; M5 provider e webhook; dataset dialoghi
del corso. **Acceptance:** nessuna lettura cross-customer, test macchina a stati e
handoff idempotente. **Risultato Sprint successivo:** prototipo menu e contratto
condiviso, senza dipendenza AI. **Riferimenti:** [regole privacy](../06_SICUREZZA.md),
[integration contracts](./INTEGRATION_CONTRACTS.md).

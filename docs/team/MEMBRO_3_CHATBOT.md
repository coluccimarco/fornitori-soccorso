# Membro 3 — Chatbot a menu guidato

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 3<br>
**Indice:** obiettivo · conversazione · API · privacy · test

## Scopo e maturità

Flusso deterministico guidato da parole chiave, senza dipendenza obbligatoria da AI.
Dialogo in [`chatbot/`](../../chatbot/README.md) e API `chat/*` **IMPLEMENTATI**,
allineati al foglio `DialoghiChatbot` del docente ([H-007](../management/DECISION_LOG.md)).

## Dialogo

Stati: `START → WAIT_LOCATION → WAIT_PROBLEM → START`, con `AIUTO`, `ANNULLA` e `STATO`
validi in ogni stato. Conservare stato e contesto minimo, scadenza conversazione e
numero tentativi; non memorizzare PII non necessaria.

1. Il cliente scrive la targa, che viene normalizzata e cercata.
2. Il bot mostra nome e compagnia e chiede indirizzo o posizione.
3. Il bot chiede il problema e crea la missione in stato `nuova`.
4. `STATO` risponde sull'ultima richiesta aperta della chat, con il nome dell'autista
   se ha accettato.
5. Errori: input non valido, targa ignota, servizio indisponibile, rate limit.

## API e integrazione

Il bot non accede al DB e usa `/api/v1/chat/*` con `FORNITORI_CHAT_TOKEN`. La ricerca
`GET /api/v1/clienti?search=...` resta riservata alla centrale.

I dialoghi 1-9 del foglio sono test automatici in `chatbot/tests/test_dialogs.py`; il
10, con Gemini, è facoltativo e non implementato. Sequenza in
[`diagrams/chatbot_sequence.mmd`](../diagrams/chatbot_sequence.mmd).

**Dipendenze:** M1 auth/lookup/mission create; M5 provider e webhook; dataset dialoghi
del corso. **Acceptance:** dialoghi del docente superati, test macchina a stati e
handoff idempotente. **Riferimenti:** [regole privacy](../architecture/SECURITY_DESIGN.md),
[integration contracts](./INTEGRATION_CONTRACTS.md).

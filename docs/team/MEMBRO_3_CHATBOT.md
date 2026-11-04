# Membro 3 — Chatbot

**Responsabile:** Marco Colucci · **Versione:** 2.0 · **Data:** 04/11/2026

## Scopo

Flusso guidato da parole chiave, senza dipendenza da AI, in [`chatbot/`](../../chatbot/README.md).
Il dialogo segue il foglio `DialoghiChatbot` del docente, con le differenze indicate sotto.

## Dialogo

Stati: `START → WAIT_LOCATION → WAIT_INTERVENTION → WAIT_PROBLEM → START`, con `AIUTO`,
`ANNULLA` e `STATO` validi in ogni stato. Un dialogo inattivo scade dopo 30 minuti; 3 input
non validi lo fanno ricominciare.

1. Il cliente scrive la targa, che viene normalizzata e cercata.
2. Il bot mostra nome e compagnia e chiede indirizzo o posizione.
3. Il cliente sceglie riparazione o traino con i tasti Telegram; per il traino il backend
   assegna l'officina più vicina.
4. Il bot chiede il problema e crea la missione in stato `nuova`.
5. `STATO` risponde sull'ultima richiesta aperta della chat con una frase comprensibile,
   mai con lo stato interno, e con il nome dell'autista se ha accettato.
6. Errori gestiti: input non valido, targa ignota, servizio indisponibile, rate limit.

Il riconoscimento della targa vale 15 minuti. Se scade durante il dialogo, il bot lo rifà
una volta da solo e prosegue, senza far ripetere nulla al cliente.

## API

Il bot non accede al DB e usa `/api/v1/chat/*` con `FORNITORI_CHAT_TOKEN`. La ricerca
`GET /api/v1/clienti?search=...` resta riservata alla centrale.

## Limiti noti della demo

- **La sola targa identifica il cliente.** Chi la conosce vede nome e compagnia e può creare
  una richiesta. È una scelta della demo, allineata ai dialoghi del docente, e non è adatta
  a dati reali.
- I dialoghi 2-4 hanno un passo in più del foglio docente (scelta dell'intervento); il
  dialogo 7 usa frasi diverse per non mostrare gli stati interni.
- Il dialogo 10, con Gemini, è facoltativo e non implementato.
- I dialoghi in corso stanno in RAM: un riavvio li azzera.

**Riferimenti:** [privacy](../architecture/SECURITY_DESIGN.md),
[contratti](./INTEGRATION_CONTRACTS.md), [sequenza](../diagrams/chatbot_sequence.mmd).

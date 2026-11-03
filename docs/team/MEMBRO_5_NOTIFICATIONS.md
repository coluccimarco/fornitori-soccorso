# Membro 5 — Chat ingress e notifiche

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 5<br>
**Indice:** obiettivo · provider · eventi · webhook · retry · test

## Scopo e stato

Gestire ingresso richieste chatbot e messaggi di stato. Il backend scrive gli
eventi `MISSION_ACCEPTED` e `MISSION_COMPLETED` in `notifications_outbox` nella
stessa transazione delle transizioni. Il bot in `chatbot/` riceve richieste
Telegram e invia notifiche supportate (**IMPLEMENTATO**, vedi
[D-009](../DECISION_LOG.md)); WhatsApp e webhook in ingresso sono **PIANIFICATI**.
La CI verifica il software, non il provider Telegram dal vivo.

## Provider e funzioni

Valutare WhatsApp Business API/Twilio; Telegram solo se autorizzato dalla traccia.
Funzioni target: ricezione richiesta chatbot, notifica missione presa in carico con
nome autista, notifica completamento, logging esito e retry senza duplicati.
Implementare interfaccia adapter per non legare il dominio a un provider.

## Eventi e idempotenza

Eventi outbox implementati: `MISSION_ACCEPTED`, `MISSION_COMPLETED`. Il payload
identifica evento, missione e autista. Per le missioni create da chat `channel` è il
canale di origine e il destinatario è la chat; per quelle della centrale `channel` è
`internal` e il destinatario è il telefono cliente. Il bot legge la coda, invia e
registra l'esito con retry limitati; event key univoca impedisce duplicati.
Payload limita PII e include id missione/canale/destinatario e template parametri.

Esempio concettuale, non credenziale:

```json
{"eventKey":"mission-42-completed","type":"MISSION_COMPLETED",
 "missionId":42,"channel":"telegram","recipient":"<opaque-recipient>"}
```

Webhook provider deve verificare firma/secret, timestamp, replay e idempotenza prima
di accettare aggiornamenti. I POST `/api/v1/webhooks/telegram|whatsapp` in OpenAPI
sono **PIANIFICATI**.

## Sviluppo e test

Locale: fake adapter deterministico, nessun messaggio reale; segreti solo env/secret
store. Testare firma valida/non valida, callback duplicato, retry, provider 429/5xx,
errore permanente, outbox atomicità e PII nei log. Sandbox/provider reale solo con
autorizzazione e numeri di test.

**Dipendenze:** M1 outbox/eventi/autorizzazione; M3 dialogo e backend HTTP; provider
approvato. **Acceptance:** consegna idempotente osservabile, retry limitato, errori
tracciabili, segreti non versionati. **Risultato Sprint successivo:** adapter fake,
contratto webhook e spike provider. **Riferimenti:** [sicurezza](../06_SICUREZZA.md),
[API](../04_API_REST.md).

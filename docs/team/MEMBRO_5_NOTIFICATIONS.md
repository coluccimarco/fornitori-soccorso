# Membro 5 — Notifiche

**Responsabili:** Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi ·
**Versione:** 2.0 · **Data:** 04/11/2026

## Scopo

Avvisare il cliente su Telegram quando un autista prende in carico la richiesta e quando
l'intervento è concluso. WhatsApp e webhook in ingresso sono **fuori ambito**.

## Eventi

Il backend scrive `MISSION_ACCEPTED` e `MISSION_COMPLETED` in `notifications_outbox` nella
stessa transazione delle transizioni. La chiave `event_key` è univoca e impedisce duplicati.
Il payload contiene evento, codice missione e nome autista, non dati del cliente.

| Origine missione | Canale | Destinatario | Stato iniziale |
|---|---|---|---|
| Chat | Canale di origine | Chat del cliente | `PENDING` |
| Centrale | `internal` | Nessuno | `SKIPPED`: non viene inviata |

La chat del cliente non è nota per le missioni create dalla centrale, quindi queste non
generano messaggi e non conservano il telefono.

## Stato di invio

```text
PENDING → SENDING → SENT
             ↓
          PENDING (fallimento: riprova dopo 30 secondi)
             ↓
          FAILED (dopo 5 tentativi)
```

1. Il dispatcher del bot chiama `POST /api/v1/chat/notifiche/claim`: il backend riserva fino
   a 20 righe con `FOR UPDATE SKIP LOCKED`, quindi due dispatcher non ricevono la stessa riga.
2. Il dispatcher invia e registra l'esito con `POST /api/v1/chat/notifiche/{id}/esito`, riportando il `tentativo` ricevuto: un esito di una riserva vecchia viene ignorato.
3. Se manca l'esito, la riserva scade dopo 5 minuti: la riga torna disponibile e conta come
   tentativo.

**Limite:** se il bot si ferma tra l'invio a Telegram e la registrazione dell'esito, il
messaggio può ripartire una volta alla scadenza della riserva. Telegram non offre una chiave
anti-duplicati, quindi la consegna è "almeno una volta".

## Provider e canali

`NotificationDispatcher` riceve la funzione di invio del canale e non dipende da Telegram;
`InboundMessage` fa lo stesso per i messaggi in arrivo. Per aggiungere un canale serve un
adattatore con queste due interfacce. I segreti stanno solo nell'ambiente.

**Riferimenti:** [sicurezza](../architecture/SECURITY_DESIGN.md),
[API](../architecture/API_SPECIFICATION.md), [contratti](./INTEGRATION_CONTRACTS.md).

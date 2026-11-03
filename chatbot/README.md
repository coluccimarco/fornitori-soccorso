# Chatbot FORNITORI (Membro 3)

Dialogo guidato a menu in Python, senza dipendenze esterne (solo libreria standard,
Python 3.10 o superiore). Non accede al database e non tocca il backend Java: parla
con il backend solo tramite i tre endpoint `/api/v1/chat/*` di `api/openapi.yaml`.

**Stato:** dialogo, canale Telegram e collegamento al backend reale sono pronti e provati
da console con backend e PostgreSQL locali. La conversazione vera da Telegram è ancora da
provare, WhatsApp non è collegato e le due notifiche automatiche al cliente non partono
ancora.

## Provarlo

Dalla cartella `chatbot/`:

```bash
python3 -m fornitori_chatbot
```

La console usa dati inventati, stampati all'avvio. `/pos 41.2 16.6` simula la posizione
condivisa, `/esci` termina.

```bash
python3 -m unittest discover
```

## Collegarlo al backend reale

Backend e bot devono avere lo stesso `FORNITORI_CHAT_TOKEN`: generalo una volta con
`openssl rand -hex 32` e non scriverlo in file del repo. Il database deve includere
`database/05_chat_requests.sql`.

Nel terminale del backend, dalla radice del repo:

```bash
./mvnw spring-boot:run
```

In quello del bot, dalla cartella `chatbot/`:

```bash
FORNITORI_API_URL=http://localhost:8080 python3 -m fornitori_chatbot --http
```

Con il backend reale valgono le targhe e le polizze di `database/02_seed.sql`.

## Collegarlo a Telegram

1. Su Telegram apri **@BotFather**, invia `/newbot` e copia il token che ti restituisce.
2. Dalla cartella `chatbot/`, incolla il token al prompt (non viene mostrato) e avvia:

```bash
read -rs TELEGRAM_BOT_TOKEN && export TELEGRAM_BOT_TOKEN
```

```bash
python3 -m fornitori_chatbot --telegram
```

3. Apri il tuo bot su Telegram e premi **Avvia**. Per il backend reale aggiungi `--http`.

Il bot resta attivo finché il comando è in esecuzione. Usa il long polling, quindi non
serve ngrok né un indirizzo pubblico; risponde solo nelle chat private. Il token non va
mai scritto in file del repo o in chat.

## Struttura

| File | Contenuto |
|---|---|
| `engine.py` | Macchina a stati: `ChatEngine.handle(messaggio)` restituisce le risposte |
| `messages.py` | Tutti i testi mostrati al cliente |
| `backend.py` | Interfaccia verso il backend e tipi di errore |
| `fake_backend.py` | Backend finto per sviluppo e test |
| `http_backend.py` | Backend reale via HTTP |
| `telegram_channel.py` | Canale Telegram |
| `__main__.py` | Avvio da console o Telegram |

## Dialogo

`START → WAIT_PLATE → VERIFY_CUSTOMER → WAIT_LOCATION → WAIT_PROBLEM → CONFIRM →
SHOW_STATUS → END`. `AIUTO` e `ANNULLA` valgono in ogni stato.

- **Tentativi:** 3 input non validi chiudono il dialogo; 3 verifiche fallite bloccano
  la verifica per 15 minuti, anche dopo `ANNULLA`.
- **Scadenze:** un dialogo inattivo scade dopo 30 minuti; una chat con missione è
  ricordata per 24 ore.
- **Anti-duplicati:** `requestId` resta lo stesso nei tentativi dopo un errore di rete,
  e i `message_id` già visti vengono ignorati.
- **Privacy:** targa e dati raccolti si cancellano appena non servono, il numero di
  polizza non viene conservato, targa ignota e polizza errata hanno la stessa risposta.
- **Memoria:** le sessioni stanno in RAM, quindi un riavvio le azzera.

## Aggiungere un canale (Membro 5)

Per WhatsApp o per i webhook basta trasformare ogni messaggio in un `InboundMessage`
(modello: `telegram_channel.py`):

```python
engine = ChatEngine(HttpBackend(url, token))
risposte = engine.handle(InboundMessage(
    channel="telegram",       # oppure "whatsapp"
    chat_id="...",            # id della chat, non esce dal bot
    text="...",
    message_id="...",         # per scartare i callback duplicati
    latitude=None, longitude=None,
))
```

## Limiti noti

1. **Verifica del titolare:** il bot chiede il numero di polizza. La guida chiede una
   verifica "non prevedibile": va confermata (`docs/DECISION_LOG.md`, D-007).
2. **Traino:** il bot crea sempre `riparazione_sul_posto`. La centrale imposta traino e
   officina con `PATCH /api/v1/missioni/{id}/intervento`, ma serve il comando nell'interfaccia.
3. **Stato dopo la chat:** il bot risponde sullo stato solo dalla chat che ha creato la
   missione, entro 24 ore e finché non si riavvia. Dopo 60 minuti richiede la verifica.
4. **Notifiche al cliente:** presa in carico e conclusione non vengono inviate: nel backend
   gli eventi restano nell'outbox e il bot non è avvisato dei cambi di stato.

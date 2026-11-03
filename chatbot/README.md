# Chatbot FORNITORI (Membro 3)

Dialogo guidato a menu in Python, senza dipendenze esterne (solo libreria standard,
Python 3.10 o superiore). Non accede al database e non tocca il backend Java: parla
con il backend solo tramite i tre endpoint `/api/v1/chat/*` di `api/openapi.yaml`.

**Stato:** dialogo, backend finto, canale Telegram e test sono pronti. Il client HTTP è
provato solo contro un server di prova, perché gli endpoint `chat/*` non esistono ancora
nel backend. WhatsApp non è collegato.

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

Quando gli endpoint saranno disponibili:

```bash
FORNITORI_API_URL=http://localhost:8080 FORNITORI_CHAT_TOKEN=... python3 -m fornitori_chatbot --http
```

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

## Punti da concordare con Membro 1

1. **Verifica del titolare:** come `verificationChallenge` il bot chiede per ora il
   numero di polizza. La guida chiede una verifica "non prevedibile": va confermata.
2. **Traino:** `CreateMission` richiede `officinaId` per il traino, ma il bot non può
   leggere le officine. Per ora invia sempre `riparazione_sul_posto`, e nel contratto
   non c'è un endpoint con cui la centrale possa cambiare tipo o officina.
3. **Stato dopo la chat:** nessun endpoint trova la missione di un cliente, quindi il
   bot risponde sullo stato solo dalla chat che l'ha creata, entro 24 ore.
4. **Token di verifica:** OpenAPI lo descrive legato anche alla missione, ma viene
   emesso dal lookup, prima che la missione esista.
5. **Credenziale di servizio:** come si ottiene `FORNITORI_CHAT_TOKEN`.

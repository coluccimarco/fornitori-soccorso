# Chatbot FORNITORI

**Responsabile:** Marco Colucci (Membro 3)

Dialogo guidato in Python, senza dipendenze esterne (solo libreria standard, Python 3.10
o superiore), allineato ai dialoghi del docente. Non accede al database: parla
con il backend solo tramite gli endpoint `/api/v1/chat/*` di `api/openapi.yaml`.

**Stato:** dialogo, collegamento al backend reale e notifiche al cliente funzionano su Telegram, con backend
e PostgreSQL locali. WhatsApp e webhook sono fuori ambito.

## Provarlo

Dalla cartella `chatbot/`:

```bash
python3 -m fornitori_chatbot
```

La console usa dati inventati, stampati all'avvio. `/pos 41.2 16.6` simula la posizione
condivisa, `/esci` termina.

## Collegarlo al backend reale

Backend e bot devono avere lo stesso `FORNITORI_CHAT_TOKEN`: generalo una volta con
`openssl rand -hex 32` e non scriverlo in file del repo. Il database deve avere tutti gli script
`database/` applicati in ordine: vedi il [README principale](../README.md).

Nel terminale del backend, dalla radice del repo:

```bash
./mvnw spring-boot:run
```

In quello del bot, dalla cartella `chatbot/`:

```bash
FORNITORI_API_URL=http://localhost:8080 python3 -m fornitori_chatbot --http
```

Con il backend reale valgono le targhe di `database/02_seed.sql`, per esempio `PW 660 SW`.

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

## Test

Dalla cartella `chatbot/`:

```bash
python3 -m unittest discover
```

## Struttura

| File | Contenuto |
|---|---|
| `engine.py` | Macchina a stati: `ChatEngine.handle(messaggio)` restituisce le risposte |
| `messages.py` | Tutti i testi mostrati al cliente |
| `backend.py` | Interfaccia verso il backend e tipi di errore |
| `fake_backend.py` | Backend finto per sviluppo e demo da console |
| `http_backend.py` | Backend reale via HTTP |
| `telegram_channel.py` | Canale Telegram |
| `notifications.py` | Invio al cliente delle notifiche accodate dal backend |
| `__main__.py` | Avvio da console o Telegram |

## Dialogo

`START → WAIT_LOCATION → WAIT_INTERVENTION → WAIT_PROBLEM → START`. Il tipo di intervento
si sceglie con i tasti Telegram, oppure scrivendo 1 o 2. `AIUTO`, `ANNULLA` e `STATO` valgono in
ogni stato; un messaggio che non è una targa riceve il testo di aiuto.

- **Tentativi:** 3 input non validi fanno ricominciare il dialogo; il backend risponde
  429 dopo 5 targhe non trovate nella stessa sessione.
- **Scadenze:** un dialogo inattivo scade dopo 30 minuti.
- **Riconoscimento:** il backend autorizza una sola richiesta, per 15 minuti, dopo la targa; se scade durante il
  dialogo, il bot rifà il controllo una volta e prosegue.
- **Anti-duplicati:** `requestId` resta lo stesso nei tentativi dopo un errore di rete,
  e i `message_id` già visti vengono ignorati.
- **Stato:** `STATO` risponde con frasi comprensibili, senza mostrare gli stati interni della missione.
- **Memoria:** i dialoghi in corso stanno in RAM, quindi un riavvio li azzera. Le missioni
  e lo stato stanno nel backend e restano consultabili.

## Aggiungere un canale

WhatsApp e webhook sono fuori ambito. Per aggiungere un canale basta trasformare ogni messaggio in un
`InboundMessage` (modello: `telegram_channel.py`):

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

1. **Nessuna verifica del titolare:** come nei dialoghi del docente, basta la targa. Chi la
   conosce vede nome e compagnia e può creare una richiesta. È un limite noto della demo, non adatto a
   dati reali (`docs/management/DECISION_LOG.md`, D-007).
2. **Traino:** il backend assegna l'officina più vicina in linea d'aria, non per strada. La
   centrale può cambiarla dalla scheda missione, con **Aggiorna intervento**. La scelta con i tasti non è nei dialoghi del docente: i dialoghi 2-4
   hanno un passo in più.
3. **Stato:** `STATO` cerca per chat, non per numero di telefono, che Telegram non comunica.
4. **Indirizzi:** un indirizzo scritto diventa coordinate alla prima richiesta di percorso
   sulla mappa, non alla creazione della missione.
5. **Dialogo 10 del foglio:** la frase libera con Gemini è facoltativa e non implementata.
6. **Consenso:** i dialoghi del docente non lo prevedono, quindi il bot non lo chiede.
7. **Notifiche al cliente:** con `--telegram` il bot controlla ogni 5 secondi se ci sono
   messaggi di presa in carico e conclusione da inviare, e li riserva sul backend così che due bot non
   inviino lo stesso messaggio. Partono solo per le missioni create dal bot: per quelle create dalla
   centrale la chat del cliente non è nota. Se il bot è spento restano in coda e partono alla riaccensione.
   Se si ferma proprio tra l'invio e la registrazione dell'esito, il messaggio può ripartire una volta dopo 5 minuti.

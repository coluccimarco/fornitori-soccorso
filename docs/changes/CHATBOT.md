# Bot Telegram — contratto condiviso v1

> **Record storico:** branch già fuso in `main`. Descrive la situazione al momento del merge: lo stato attuale è in [`chatbot/README.md`](../../chatbot/README.md).

## Base Git

Branch: `feature/telegram-bot`.
Hash di partenza dopo `git fetch origin`: `d6ab902b26fb2072d8ec2ad93aeaacc634f70617`.
Il prompt attendeva `eb62c65`: differenza segnalata a Marco prima delle modifiche.
I due commit successivi riguardano soltanto l'aggiunta e la rimozione dei prompt;
la base del codice applicativo coincide con quella attesa.

## Modifiche

- Telefono sul posto validato con la regex del contratto; spazi rimossi prima dell'invio.
  La validazione viene ripetuta sul valore normalizzato per evitare un payload non valido.
- Lookup dopo il telefono, con prosecuzione anche per `DA_VERIFICARE` e cliente nullo.
- Cinque problemi tramite pulsanti inline o numeri 1–5; eliminati selezione intervento
  e descrizione libera. Il backend determina intervento e descrizione.
- JSON camelCase con targa, telefono e codice problema; lettura di `inVerifica`.
  Se manca `esito`, viene dedotto da `verificato`.
- Testi finali conformi al contratto, nuovi stati e notifica `MISSION_CANCELLED`.
  Nome autista ignorato nel rendering, anche se il backend lo fornisce.
- Notifiche sconosciute ignorate e confermate senza invio, evitando tentativi ripetuti.
- Conservati scadenza dialogo, contatore tentativi, idempotenza, deduplicazione messaggi
  e rinnovo singolo del lookup. Un rinnovo aggiorna il cliente nel payload se cambia l'esito polizza.
- Fake backend con polizze da verificare; tastiera con un pulsante per riga per le etichette lunghe.
- Ricontrollo: un HTTP 404 nel lookup resta un errore di servizio, senza essere trasformato
  in `DA_VERIFICARE`. Anche risposte prive del booleano `verificato` o con `esito` sconosciuto
  vengono rifiutate come errori di servizio; il fallback per backend senza `esito` resta attivo.

## Diagramma del dialogo

```text
START -- targa valida --> WAIT_PHONE
WAIT_PHONE -- telefono valido --> lookup
  VERIFICATA     --> conferma targa --> WAIT_LOCATION
  DA_VERIFICARE  --> attesa polizza --> WAIT_LOCATION
WAIT_LOCATION -- indirizzo / coordinate --> WAIT_PROBLEM
WAIT_PROBLEM -- pulsante / numero 1–5 --> creazione
  inVerifica=false --> attesa autista + codice --> START
  inVerifica=true  --> attesa centrale + codice --> START
  AddressNotFound --> WAIT_LOCATION
  errore temporaneo --> WAIT_PROBLEM (requestId conservato)
Ogni stato: AIUTO / STATO; ANNULLA --> START
Stati attivi: inattività 30 minuti --> nuovo dialogo
```

## Test

Da `chatbot/`, con Python 3.14.6 su Windows:

```text
python -m unittest discover
python -m compileall -q fornitori_chatbot
```

30 test superati. Compilazione completata senza errori.
Verificati flussi con polizza verificata e da verificare, telefono e tentativi,
tutti i codici da pulsante e numero, callback Telegram, testi esatti, assenza del nome
autista, nuovi stati, annullamento dialogo, scadenze, rinnovo del grant, errori di indirizzo,
retry con stesso requestId, serializzazione HTTP e notifiche sconosciute.
Il ricontrollo copre inoltre errori lookup HTTP e risposte malformate, ripresa del dialogo
dopo un errore e verifica del JSON sulla richiesta HTTP effettivamente costruita.
Su Windows si usa `python`, equivalente ai comandi `python3` richiesti dalla CI.
Prova reale backend/Telegram non eseguita: nessuna integrazione con il branch backend
e nessun token Telegram di test utilizzato. Non vengono avviati bot sul canale reale.

## Richieste di modifica al contratto

Nessuna.

## Impatti sugli altri branch

Fondere questo branch **dopo `feature/backend-operativita`**: il fallback di `esito`
consente di leggere vecchie risposte lookup ma non rende il nuovo payload di creazione
compatibile con il vecchio backend. Servono grant anche per `DA_VERIFICARE`, nuovi campi
missione, `inVerifica` e notifica di annullamento secondo il contratto condiviso.
Nessuna modifica richiesta al frontend. Nessun file backend/frontend modificato.

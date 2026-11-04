# Manuale utente FORNITORI

**Codice:** FOR-USER-001 · **Versione:** 2.0 · **Data:** 04/11/2026
**Destinatari:** responsabile centrale, autista e cliente
**Responsabili:** Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio, Marco Colucci

## Prima di iniziare

Il client web è presente in `src/main/resources/static/` ed è servito dal backend
Spring Boot. Per usare l'area riservata occorrono un ambiente avviato, database
caricato e credenziali autorizzate fornite dall'amministratore. Non sono incluse
credenziali demo in questa guida. Il percorso cliente usa il chatbot Telegram,
separato dalla pagina pubblica informativa.

## Responsabile centrale

### Accesso

1. Aprire l'URL dell'applicazione fornito dall'amministratore.
2. Selezionare **Area operatori**.
3. Inserire email e password autorizzate e premere **Accedi**.
4. Se l'accesso riesce, si apre **Panoramica**. L'errore di credenziali non valide
   viene mostrato sotto il modulo; non ripetere login con account non autorizzati.

### Panoramica e anagrafiche

- **Panoramica** mostra conteggi clienti, autisti, mezzi, compagnie e officine.
  **Aggiorna dati** ricarica le informazioni dal server.
- La navigazione laterale apre **Clienti**, **Autisti**, **Mezzi**, **Compagnie**,
  **Officine** e **Missioni**. Le pagine anagrafiche sono consultabili; la ricerca
  consente di filtrare e per i clienti cerca nome o targa.
- Per registrare un cliente aprire **Clienti → Nuovo cliente**. Compilare i campi
  obbligatori: nome e cognome, telefono, targa, compagnia assicurativa, numero e
  scadenza della polizza. Modello veicolo e città sono facoltativi. Premere
  **Salva cliente**; se il server accetta la richiesta la finestra si chiude e compare
  **Cliente aggiunto correttamente**. In caso di errore, il messaggio indica il problema, per esempio
  **Targa già presente**. Il telefono accetta cifre, un `+` iniziale e i separatori comuni.

### Creare e proporre una missione

1. Aprire **Missioni → + Nuova missione**.
2. Selezionare il cliente e il tipo d'intervento. Per **Traino**, selezionare anche
   l'officina; per la riparazione sul posto l'officina non è richiesta.
3. Selezionare il canale d'origine, inserire luogo dell'intervento e problema/note.
   Il valore del canale è un metadato: scegliere WhatsApp non attiva un provider
   WhatsApp.
4. Premere **Crea missione**. Se i dati sono accettati, la missione appare nella
   lista e il messaggio conferma che si può inviare una proposta.
5. Nella scheda missione scegliere un autista disponibile e premere
   **Invia proposta**, oppure **Proponi al più vicino** per lasciare la scelta al
   sistema. La scheda mostra lo stato e il conto alla rovescia.

### Consultare e correggere una missione

- I filtri **Operativa / Storico di riferimento** e **Tutti gli stati** selezionano le missioni.
- **Mostra segnalazioni** carica le note dell'autista.
- **Mostra mappa** apre il percorso e i km stimati dell'autista scelto o assegnato.
- **Mostra storico proposte** elenca autista, esito e data di ogni tentativo.
- **Aggiorna intervento** corregge tipo e officina, finché la missione è nuova, rifiutata o scaduta.
- **Correggi costo** rettifica una missione eseguita: serve un motivo, il costo calcolato resta visibile.

### Avvisi

La campanella in alto segnala le proposte rifiutate o scadute e si aggiorna ogni 15 secondi. **Apri missione**
porta alla scheda, **Segna come letti** azzera il contatore solo in questo browser.

## Autista

1. Accedere con un account autorizzato al ruolo autista.
2. In **Le mie missioni**, consultare proposta, luogo, tipo intervento, mappa,
   guadagno stimato e tempo residuo. Le nuove proposte compaiono da sole entro 15 secondi.
3. Premere **Accetta** oppure **Rifiuta**. Se accettata, la missione attiva compare
   nella stessa pagina, con nome e telefono del cliente.
4. Per informare la centrale, dopo aver accettato, compilare **Segnala alla centrale** e premere
   **Invia segnalazione**. L'esito è visibile nella lista delle note.
5. Per chiudere l'intervento, controllare i chilometri, proposti dalla stima del percorso, e
   premere **Chiudi missione · OK**. Solo dopo l'accettazione del server la scheda
   si aggiorna con conferma di chiusura.
6. Aprire **Storico e guadagni** per consultare le missioni completate e il totale.

Nome e telefono del cliente compaiono solo dopo l'accettazione. Se il servizio mappe non risponde, i
chilometri si possono inserire comunque.

## Cliente e chatbot

La pagina pubblica contiene informazioni e contatti; non invia direttamente una
richiesta al backend. Per il flusso bot occorre il canale Telegram predisposto dal
team:

1. Aprire **Soccorso Stradale Bot** e premere **Avvia**.
2. Inserire la targa e seguire le domande del dialogo: luogo, tipo di intervento e problema.
3. Usare `AIUTO` per le indicazioni, `ANNULLA` per uscire e `STATO` per consultare
   la richiesta associata alla chat, se presente.
4. Se il bot segnala indisponibilità, non inviare richieste ripetute; seguire le
   indicazioni ricevute o contattare la centrale.

Quando un autista accetta e quando l'intervento è concluso, il bot avvisa nella stessa chat. Il
riconoscimento basato sulla sola targa è un limite noto della demo: non verifica la titolarità del veicolo.
Il bot non è un servizio di emergenza. Consultare
[`chatbot/README.md`](../../chatbot/README.md) per prerequisiti e limiti del bot.

## Errori e sicurezza

`401` indica sessione/credenziale non valida; `403` permessi insufficienti; `429` troppi
tentativi: attendere un minuto prima di riprovare. Un errore `500` riporta un riferimento da
comunicare al team tecnico. Per errori di validazione o conflitto leggere il messaggio e verificare lo stato prima
di ripetere un'azione. Non condividere password, token o dati personali tramite
canali non autorizzati. Per emergenze stradali usare i canali ufficiali del gestore
stradale o i servizi di emergenza.

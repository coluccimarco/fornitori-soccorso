# Manuale utente FORNITORI

**Codice:** FOR-USER-001 · **Versione:** 1.1 · **Data:** 03/11/2026
**Destinatari:** responsabile centrale, autista e cliente

## Prima di iniziare

Il client web è presente in `src/main/resources/static/` ed è servito dal backend
Spring Boot. Per usare l'area riservata occorrono un ambiente avviato, database
caricato e credenziali autorizzate fornite dall'amministratore. Non sono incluse
credenziali demo in questa guida. Il percorso cliente usa il chatbot Telegram,
separato dalla pagina pubblica informativa. La prova su browser e l'invio Telegram
non sono stati ripetuti in questa revisione; le istruzioni descrivono i controlli
presenti nel codice.

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
  **Salva**; se il server accetta la richiesta la finestra si chiude e compare
  **Cliente aggiunto correttamente**. In caso di errore, correggere i dati indicati
  dal messaggio o chiedere all'amministratore.

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
   **Invia proposta**. La scheda mostra lo stato e il conto alla rovescia; il
   risultato dipende dalla risposta dell'autista e dalla scadenza.

Usare i filtri **Operativa / Storico di riferimento** e **Tutti gli stati** per
consultare le missioni. **Mostra segnalazioni** carica le note registrate per la
missione. Il client attuale non espone la modifica tipo intervento/rettifica costo
né una mappa; questi flussi non vanno eseguiti tramite schermate inventate.

## Autista

1. Accedere con un account autorizzato al ruolo autista.
2. In **Le mie missioni**, consultare proposta, luogo, tipo intervento, dettagli
   disponibili e tempo residuo.
3. Premere **Accetta** oppure **Rifiuta**. Se accettata, la missione attiva compare
   nella stessa pagina; se la proposta scade, aggiornare la vista.
4. Per informare la centrale, compilare **Segnala alla centrale** e premere
   **Invia segnalazione**. L'esito è visibile nella lista delle note.
5. Per chiudere l'intervento, inserire i chilometri effettivamente percorsi e
   premere **Chiudi missione · OK**. Solo dopo l'accettazione del server la scheda
   si aggiorna con conferma di chiusura.
6. Aprire **Storico e guadagni** per consultare le missioni completate e il totale.

Il client non mostra indirizzo/telefono del cliente oltre ai dati restituiti
dall'API; non cercare di aggirare i permessi o recuperare recapiti da altre aree.

## Cliente e chatbot

La pagina pubblica contiene informazioni e contatti; non invia direttamente una
richiesta al backend. Per il flusso bot occorre il canale Telegram predisposto dal
team:

1. Aprire il bot corretto e premere **Avvia**.
2. Inserire la targa e seguire le domande del dialogo su luogo e problema.
3. Usare `AIUTO` per le indicazioni, `ANNULLA` per uscire e `STATO` per consultare
   la richiesta associata alla chat, se presente.
4. Se il bot segnala indisponibilità, non inviare richieste ripetute; seguire le
   indicazioni ricevute o contattare la centrale.

Il bot basato sulla targa non verifica la titolarità del veicolo e non è un servizio
di emergenza o un canale production certificato. Consultare
[`chatbot/README.md`](../../chatbot/README.md) per prerequisiti e limiti del bot.

## Errori e sicurezza

`401` indica sessione/credenziale non valida; `403` permessi insufficienti. Per
errori di validazione o conflitto leggere il messaggio e verificare lo stato prima
di ripetere un'azione. Non condividere password, token o dati personali tramite
canali non autorizzati. Per emergenze stradali usare i canali ufficiali del gestore
stradale o i servizi di emergenza.

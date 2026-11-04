import { C, MEMBRI, pts, card, two, three, motivi, md, esc } from './lib.mjs';

const CL = 'Chiusura';

export default [
  {
    section: CL, owner: MEMBRI.team, title: 'Sicurezza e limiti noti',
    html: two(`<div>
      <div style="display:flex;gap:14px;margin-bottom:20px">
        <div class="card" style="--c:${C.teal};flex:1;text-align:center;padding:12px"><h3 style="font-size:42px;margin:0">16</h3><div style="font-size:18px">corrette</div></div>
        <div class="card" style="--c:${C.yellow};flex:1;text-align:center;padding:12px"><h3 style="font-size:42px;margin:0">2</h3><div style="font-size:18px">accettate per la demo</div></div>
        <div class="card" style="--c:#c0392b;flex:1;text-align:center;padding:12px"><h3 style="font-size:42px;margin:0">7</h3><div style="font-size:18px">ancora aperte</div></div>
      </div>
      ${motivi('Controlli attivi', ['BCrypt, CSRF, ruoli, limiti ai tentativi', 'CSP e corpo delle richieste limitato', '139 test su database isolato'])}
      </div>`,
    `<div>${pts([
      '<b>Sola targa</b> per la demo: V-01, aperta',
      '<b>WhatsApp</b> fuori ambito: canale Telegram',
      'HTTPS non provato su un server reale',
      'Dialogo in memoria: un riavvio lo azzera',
      'Provider mappe pubblici, senza garanzie',
    ])}<p class="big" style="font-size:22px;margin:12px 0 0;color:${C.blue}">Demo con dati sintetici: non pronta per dati reali.</p></div>`, '1fr 1fr'),
    notes: {
      voce: "Parliamo apertamente di sicurezza e limiti. Abbiamo fatto un audit del backend e tenuto un registro con 25 voci: 16 sono corrette, due sono accettate per la demo e sette restano aperte. Tra i controlli attivi ci sono BCrypt, il CSRF, i ruoli verificati dal server, i limiti ai tentativi, le intestazioni di sicurezza e 139 test su database isolato. Il limite principale è la voce V-01, ad alta gravità: la sola targa identifica il cliente. Chi conosce una targa può vedere nome e compagnia e creare una richiesta. È coerente con i dialoghi del docente, ma va bene solo con dati sintetici. Nell'audit è già descritta la soluzione: un secondo dato, come le ultime cifre della polizza. WhatsApp è fuori ambito, e usiamo Telegram. HTTPS non è stato provato su un server reale. Il dialogo sta in memoria e i provider delle mappe non offrono garanzie. La conclusione è chiara: è una demo robusta, ma non è pronta per dati reali.",
      semplice: "Sapere che cosa non è pronto è parte del lavoro. Il registro dice cosa è stato corretto, cosa è stato accettato e cosa resta aperto, con il motivo.",
      file: ['`docs/security/VULNERABILITY_REGISTER.md`', '`docs/security/BACKEND_SECURITY_AUDIT.md`', '`docs/security/SECURITY_TEST_REPORT.md`', '`docs/architecture/SECURITY_DESIGN.md`', '`docs/management/RISK_REGISTER.md`', '`docs/word/AUDIT_SICUREZZA.docx`'],
      scelta: {
        alt: "Aggiungere già ora un secondo fattore alla chat.",
        perche: "Il PDF del docente chiede solo di verificare che la targa corrisponda a un assicurato: cambiare il dialogo si allontanerebbe dai materiali. L'audit propone di introdurlo dietro una proprietà, da concordare.",
        limiti: "Voci aperte: V-01 sola targa; V-13 ruolo DB proprietario; V-15 HTTPS non provato; V-21 token statico; V-23 liste non paginate; V-24 GET delle mappe che scrive; V-25 nessun limite alle richieste aperte. Accettate: V-14 possibile doppio messaggio; V-22 limiti in memoria. Non verificati: Telegram reale nel rapporto di test, Windows, vulnerabilità delle dipendenze.",
      },
      domande: [
        ["Avete fatto un penetration test?", "No: l'audit è una verifica applicativa sui casi descritti e non una certificazione. Lo dice il documento stesso."],
        ["Qual è la cosa più grave?", "V-01: la targa rivela nome e compagnia. La soluzione è pronta sulla carta, ma va concordata con lei perché cambia il dialogo."],
        ["Il token del bot è al sicuro?", "Sta solo nelle variabili d'ambiente. È però statico, senza rotazione automatica (V-21)."],
      ],
    },
  },
  {
    section: CL, owner: MEMBRI.team, title: 'Scaletta della demo live',
    html: `<table class="t" style="font-size:21px"><tr><th style="width:70px">#</th><th style="width:280px">Dove</th><th>Cosa mostriamo</th></tr>
      <tr><td>1</td><td>Sito pubblico</td><td>RecuVeicol e il collegamento al bot</td></tr>
      <tr><td>2</td><td>Telegram</td><td>Targa, luogo, traino, problema: nasce la richiesta</td></tr>
      <tr><td>3</td><td>Centrale</td><td>Scheda missione, <b>Proponi al più vicino</b>, conto alla rovescia</td></tr>
      <tr><td>4</td><td>Autista (telefono)</td><td>Proposta e mappa, <b>Accetta</b>: compaiono i contatti</td></tr>
      <tr><td>5</td><td>Telegram</td><td>Notifica automatica con il nome dell'autista, poi <b>STATO</b></td></tr>
      <tr><td>6</td><td>Autista</td><td>Chiusura con i km, costo calcolato dal server</td></tr>
      <tr><td>7</td><td>Telegram e centrale</td><td>Notifica di conclusione, dashboard aggiornata</td></tr></table>
      <p class="note-s" style="margin-top:14px;font-size:19px"><b>Se Telegram non risponde:</b> bot da console sul backend reale. <b>Se le mappe non rispondono:</b> km inseriti a mano.</p>`,
    notes: {
      voce: "Adesso vi facciamo vedere il sistema. Seguiremo una missione per intero, in sette passi. Partiamo dal sito pubblico, che porta al bot. Su Telegram scriviamo la targa di un cliente dimostrativo, il luogo, scegliamo il traino e descriviamo il problema: nasce la richiesta. Passiamo alla centrale: la scheda è già lì, premiamo «Proponi al più vicino» e parte il conto alla rovescia. Poi l'autista, dal telefono, vede la proposta e la mappa, ma non i contatti del cliente; accetta, e i contatti compaiono. Torniamo su Telegram: entro pochi secondi arriva la notifica con il nome dell'autista, e con STATO vediamo la frase aggiornata. L'autista chiude la missione con i chilometri e il server calcola il costo. Infine arriva l'ultimo messaggio e la dashboard si aggiorna. Abbiamo anche un piano di riserva: se Telegram non risponde, usiamo il bot da console sul backend reale; se le mappe non rispondono, i km si inseriscono a mano.",
      semplice: "È il copione della demo. Tenere aperte tre finestre (centrale, autista, Telegram) ed eseguire i passi in ordine. Le notifiche richiedono il bot acceso con `--telegram`.",
      file: ['`docs/manuals/MANUALE_UTENTE.md`', '`chatbot/README.md`', '`README.md`', '`database/02_seed.sql` (targa d\'esempio PW 660 SW)'],
      scelta: {
        alt: "Video registrato al posto della demo dal vivo.",
        perche: "La demo dal vivo dimostra che il sistema funziona davvero. Le schermate di queste slide restano utilizzabili se qualcosa non si avvia.",
        limiti: "Servono Internet (Bootstrap, Leaflet, tile, Nominatim, OSRM, Telegram), database con script applicati fino al 10 e credenziali demo prese dai materiali del docente. Una missione di notte applica il +25%, una di giorno no.",
      },
      domande: [
        ["Cosa succede se Telegram non risponde?", "Mostriamo il dialogo dalla console con `--http`: il motore e il backend sono gli stessi, cambia solo il canale."],
        ["Posso provare io con una targa?", "Sì, purché presente nel seed e con polizza valida; le targhe non trovate vengono rifiutate e contate nel limite."],
      ],
    },
  },
  {
    section: CL, owner: MEMBRI.team, title: 'Sviluppi futuri',
    html: two(pts([
      '<b>Identità del cliente</b>: secondo dato o codice SMS',
      '<b>WhatsApp e webhook</b>: già progettati nel contratto',
      '<b>Gestione utenti</b> dall\'interfaccia',
      '<b>Messa in produzione</b>: HTTPS, ruoli DB separati, log',
      '<b>Qualità</b>: liste paginate, servizi più piccoli',
    ]), `<div style="margin-top:6px">${card('Da confermare col docente', pts(['Fascia notturna: 22:00 o 20:00', 'Ora di riferimento della tariffa', 'Canale: Telegram o anche WhatsApp', 'Identità: basta la targa?']), C.yellow)}</div>`, '1.15fr .85fr'),
    notes: {
      voce: "Che cosa faremmo dopo. La prima priorità è l'identità del cliente: chiedere un secondo dato oppure un codice SMS, chiudendo la voce V-01. Poi i canali: WhatsApp e i webhook in ingresso sono già descritti nel contratto OpenAPI, come operazioni progettate e non implementate, e il motore del bot è già separato dal canale. La terza è la gestione utenti dall'interfaccia, oggi fuori ambito perché un autista richiede anche un mezzo dedicato. Poi la messa in produzione: HTTPS provato davvero, un ruolo del database per le migrazioni e uno per l'applicazione, log centralizzati e backup. Infine la qualità del codice: liste paginate e un `MissionService` diviso in parti più piccole. A destra ci sono le decisioni che aspettano una conferma da parte sua, perché cambiano il comportamento: la fascia notturna, l'ora di riferimento della tariffa, il canale e il livello di identificazione richiesto. Alcune si modificano in pochi minuti.",
      semplice: "Tutti gli sviluppi vengono dai registri del progetto: non sono promesse generiche. Le quattro decisioni a destra dipendono dal docente.",
      file: ['`docs/security/BACKEND_SECURITY_AUDIT.md` (soluzione per V-01)', '`api/openapi.yaml` (operazioni `designed`)', '`docs/agile/PRODUCT_BACKLOG.md`', '`docs/management/DECISION_LOG.md` (decisioni da richiedere)', '`docs/architecture/TAD_FORNITORI.md` (§11)'],
      scelta: {
        alt: "Aggiungere altre funzioni prima di chiudere i limiti noti.",
        perche: "Prima le voci che impediscono l'uso con dati reali: R-01, R-06, R-12 e R-13 sono indicate come da risolvere per dichiarare l'idoneità.",
        limiti: "Nessuno di questi sviluppi è implementato. Il dialogo 10 con Gemini è facoltativo e non è nei piani obbligatori.",
      },
      domande: [
        ["Quanto ci vuole per cambiare la fascia notturna?", "Poco: i valori sono in `pricing_config`. Lo storico non viene ricalcolato: resta il costo salvato."],
        ["Da dove iniziereste per andare in produzione?", "Dal secondo dato per l'identità e da HTTPS: sono i requisiti minimi prima di trattare dati reali."],
      ],
    },
  },
  {
    section: CL, owner: MEMBRI.team, title: 'Conclusioni e domande',
    html: two(pts([
      'Un sistema completo: dal messaggio alla chiusura',
      'Regole e costi solo sul server',
      'Componenti separati, contratto unico',
      'Notifiche affidabili con outbox',
      'Limiti noti, scritti e dichiarati',
    ]), `<div class="center" style="flex-direction:column;background:${C.blue};border-radius:18px;color:#fff;height:400px">
      <div style="font-size:66px;font-weight:800">Domande?</div>
      <div style="font-size:23px;font-weight:600;margin-top:14px">Grazie per l'attenzione</div>
      <div style="font-size:19px;font-weight:500;margin-top:24px;opacity:.92">RecuVeicol · ITS Developer 4.0</div>
    </div>`, '1.1fr .9fr'),
    notes: {
      voce: "Per concludere. Abbiamo costruito un sistema completo, che porta una richiesta di soccorso dal primo messaggio del cliente fino alla chiusura con il costo calcolato. Tutte le regole, i permessi e i costi stanno sul server: il browser e il bot non decidono nulla. I componenti sono separati e collegati da un unico contratto, così ognuno può evolvere senza rompere gli altri. Le notifiche nascono nella stessa transazione della missione e si possono ricostruire in ogni momento. E soprattutto conosciamo i nostri limiti: sono scritti in un registro e li abbiamo dichiarati qui. Come team abbiamo imparato a lavorare su un contratto condiviso, a scegliere tecnologie con un motivo e a difendere le scelte con i test. Vi ringraziamo per l'attenzione e siamo a disposizione per le domande. Ognuno di noi può rispondere sulla propria parte, e abbiamo aperti il codice e i documenti, se volete vedere qualcosa nel dettaglio.",
      semplice: "Il messaggio finale in cinque punti. Se la domanda riguarda una parte precisa, risponde il responsabile di quella parte: sotto trovi le domande probabili, nell'appendice.",
      file: ['`README.md`', '`docs/00_INDICE_GENERALE.md`', '`docs/team/README.md`'],
      domande: [
        ["Qual è stata la parte più difficile?", "Da concordare nel team, in base all'esperienza di ciascuno. Tecnicamente i punti più delicati sono concorrenza delle assegnazioni e affidabilità delle notifiche."],
        ["Il progetto è terminato?", "Il repository indica lo Sprint 4 in chiusura, con l'accettazione del docente da acquisire e la prova Telegram da telefono da ripetere."],
      ],
    },
  },
];

/* ---------- Appendice: solo nella guida di studio ---------- */
export const glossario = [
  ['API REST', 'Modo di offrire funzioni tramite HTTP e JSON: ogni operazione ha un indirizzo e un verbo (GET, POST, PATCH).'],
  ['Audit (tabella eventi)', 'Registro delle azioni avvenute: `mission_events` conserva chi ha fatto cosa e quando.'],
  ['BCrypt', 'Funzione per salvare le password come hash lento, non reversibile.'],
  ['BigDecimal', 'Tipo Java per numeri decimali esatti, usato per i soldi.'],
  ['Bootstrap', 'Libreria CSS e JavaScript con griglia, modali e tabelle pronte.'],
  ['CDN', 'Rete di server pubblici da cui il browser scarica librerie come Bootstrap e Leaflet.'],
  ['CI (GitHub Actions)', 'Ripete in automatico build e test a ogni modifica.'],
  ['Claim (riserva)', 'Il dispatcher prenota una notifica, che non può essere presa da altri per 5 minuti.'],
  ['Controller', 'Classe che riceve la richiesta HTTP, valida l\'input e chiama il service.'],
  ['CSP', 'Content-Security-Policy: dice al browser da quali siti può caricare script, immagini e connessioni.'],
  ['CSRF', 'Attacco in cui un altro sito invia richieste con il tuo cookie. Si blocca con un token da allegare.'],
  ['Dispatcher', 'Parte del bot che preleva le notifiche dalla coda, le invia e registra l\'esito.'],
  ['DTO', 'Oggetto che descrive i dati in entrata e in uscita dalle API, separato dalla tabella.'],
  ['Entity', 'Classe Java che rappresenta una riga di una tabella.'],
  ['Fixture', 'Dato storico importato dal seed: `is_fixture=TRUE`, mai modificato dalle azioni live.'],
  ['Geocoding', 'Trasformare un indirizzo in coordinate. Lo fa Nominatim.'],
  ['GeoJSON', 'Formato JSON per punti e linee geografiche: lo produce OSRM e lo disegna Leaflet.'],
  ['Haversine', 'Formula della distanza in linea d\'aria tra due coordinate (`GeoDistance`).'],
  ['Hibernate / JPA', 'Strumenti che collegano classi Java e tabelle. In `validate` controllano lo schema senza modificarlo.'],
  ['HttpOnly', 'Attributo del cookie: JavaScript non lo può leggere.'],
  ['Idempotenza', 'Ripetere la stessa richiesta non produce un secondo effetto: lo garantisce il `requestId`.'],
  ['Indice parziale', 'Indice valido solo per le righe che rispettano una condizione: `uq_driver_active_live`.'],
  ['Leaflet', 'Libreria JavaScript open source per mappe interattive.'],
  ['Lock pessimistico', 'La riga viene bloccata finché la transazione non finisce: chi arriva dopo attende.'],
  ['Long polling', 'Il bot chiede a Telegram se ci sono messaggi, tenendo aperta la richiesta: non serve un indirizzo pubblico.'],
  ['Maven Wrapper', 'Script `mvnw` che usa la versione di Maven indicata nel repository.'],
  ['Nominatim', 'Servizio open source di geocoding basato su OpenStreetMap.'],
  ['OpenAPI', 'Formato standard che descrive le API: nel progetto è `api/openapi.yaml`.'],
  ['OSRM', 'Servizio open source che calcola percorsi su strada.'],
  ['Outbox', 'Tabella-coda: l\'evento viene scritto nella stessa transazione del dato e inviato dopo.'],
  ['Repository', 'Interfaccia Spring Data che esegue le query su una tabella.'],
  ['Scheduler', 'Attività ripetuta in automatico: `MissionExpiryScheduler` controlla le scadenze ogni 30 secondi.'],
  ['Seed', 'Dati iniziali di esempio, in `database/02_seed.sql`.'],
  ['Service', 'Classe con le regole di business e le transazioni.'],
  ['SKIP LOCKED', 'Clausola SQL che salta le righe già bloccate da altri: usata per la coda delle notifiche.'],
  ['Tile', 'Tassello d\'immagine che compone la mappa.'],
  ['Transazione', 'Gruppo di operazioni salvate tutte insieme o nessuna.'],
  ['Webhook', 'Il servizio esterno chiama un nostro indirizzo pubblico. Qui è fuori ambito: si usa il long polling.'],
];

export const domandePerMembro = [
  ['Membro 1 · Backend e database · Vincenzo Minervini', [
    ['Perché Spring Boot e non Node.js?', 'Spring porta già sessione, CSRF, validazione e JPA con lock pessimistici. Il repo non contiene un confronto formale: il motivo documentato è lo stack condiviso (D-001).'],
    ['Perché PostgreSQL e non MySQL?', 'Il vincolo «un autista, una missione attiva» è un indice parziale con WHERE, che MySQL non supporta in questa forma.'],
    ['Come evitate la doppia assegnazione?', 'Lock su missione e autista nel service, e indice unico `uq_driver_active_live` nel database. Il secondo riceve 409.'],
    ['Quando scade una proposta?', 'Dopo 10 minuti (`pricing_config`). Lo scheduler controlla ogni 30 secondi e il service ricontrolla alla risposta.'],
    ['Come si calcola il costo?', '50 + 1,20 × km effettivi; +25% se l\'ora di accettazione è tra le 22:00 e le 08:00, Europe/Rome, arrotondamento HALF_UP. Es.: 15,23 km = €68,28 di giorno, €85,35 di notte.'],
    ['Perché 22:00 se il PDF dice 20:00?', 'Scelta del team (D-HARD-03), da confermare col docente. Nello storico 6 missioni su 30 sono notturne; 3 lo sono solo con la vecchia fascia e non vengono ricalcolate.'],
    ['Che cos\'è is_fixture?', 'Segna le 30 missioni storiche: lo scheduler e le azioni live le ignorano, così i totali di riferimento restano stabili.'],
    ['Come testate?', '139 test Java su database `fornitori_e2e*`, che partono solo con `FORNITORI_TEST_DATABASE=isolated`. La pipeline li esegue in CI.'],
    ['Perché le GET delle mappe usano il CSRF?', 'Perché possono salvare coordinate e km stimati (V-24).'],
  ]],
  ['Membro 2 · Frontend · F. Valeriano, G. Cangellosi, M. Sancilio', [
    ['Perché niente React?', 'Poche schermate, nessuna build, nessun Node.js da installare. Il motivo è ricostruito: il repo registra la scelta (D-010) senza confronto.'],
    ['Come parla il frontend col backend?', 'Con `fetch` verso API sulla stessa origine, cookie di sessione e header `X-CSRF-TOKEN` sulle modifiche.'],
    ['Come si aggiorna la pagina?', 'Con un controllo ogni 15 secondi su avvisi e proposte; l\'area autista si ridisegna solo se qualcosa è cambiato.'],
    ['Perché l\'autista non vede subito i contatti?', 'È una regola del server: nome e telefono arrivano solo dopo l\'accettazione.'],
    ['Come proteggete dall\'XSS?', 'Escaping con `safe` su tutti i dati dinamici e Content-Security-Policy.'],
    ['Funziona senza Internet?', 'No: Bootstrap, Leaflet e i tile arrivano da servizi esterni.'],
  ]],
  ['Membro 3 · Chatbot · Marco Colucci', [
    ['Perché Telegram e non WhatsApp?', 'API gratuita e aperta, nessuna approvazione Meta, adatta alla demo; il long polling evita un indirizzo pubblico.'],
    ['Perché Python?', 'Separato dal backend, parla solo HTTP e usa la sola libreria standard. Il repo non motiva formalmente la scelta del linguaggio.'],
    ['Perché il bot non legge il database?', 'Requisito NFR-005: le regole restano nel backend, che le applica una volta sola.'],
    ['Come evitate una missione doppia?', '`requestId` uguale nei tentativi e impronta SHA-256: stesso invio, stesso risultato; dati diversi, 409.'],
    ['La targa basta a identificare?', 'No: è il limite V-01. Va bene solo con dati sintetici; la soluzione è nell\'audit.'],
    ['Cosa succede dopo un riavvio?', 'I dialoghi in corso si perdono, le missioni restano nel backend.'],
    ['Perché nessuna AI?', 'Il flusso guidato è prevedibile (D-005). Il dialogo 10 con Gemini è facoltativo e non implementato.'],
  ]],
  ['Membro 4 · Mappe · Marco Colucci', [
    ['Perché non Google Maps?', 'Richiede chiave e fatturazione. H-010: servizi pubblici senza chiavi e costi nel perimetro didattico.'],
    ['Cosa fa Nominatim, cosa fa OSRM?', 'Nominatim: indirizzo in coordinate. OSRM: percorso, km e geometria.'],
    ['Perché la chiamata passa dal backend?', 'Permessi e limiti in un solo punto, salvataggio dei risultati, e la CSP vieta al browser connessioni ad altri domini.'],
    ['Km stimati o effettivi?', 'Gli stimati sono proposti alla chiusura; il costo usa quelli effettivi.'],
    ['Se OSRM è giù?', 'Errore 503 chiaro; l\'autista inserisce i km a mano.'],
    ['Perché il più vicino in linea d\'aria?', 'Non richiede una chiamata esterna per ogni autista. Può non essere il migliore su strada: la centrale può scegliere a mano.'],
  ]],
  ['Membro 5 · Notifiche · M. Sancilio, F. Valeriano, G. Cangellosi', [
    ['Che cos\'è il pattern outbox?', 'L\'evento viene scritto in una tabella nella stessa transazione della missione e inviato in seguito da un altro processo.'],
    ['Perché non inviare direttamente?', 'Database e Telegram non condividono una transazione: un guasto produrrebbe dati incoerenti o messaggi persi.'],
    ['Come evitate i doppi invii?', '`FOR UPDATE SKIP LOCKED` tra dispatcher; `event_key` univoca; numero di tentativo nell\'esito.'],
    ['I doppi invii sono impossibili?', 'No: un crash tra invio ed esito può ripetere il messaggio una volta (V-14, accettata per la demo).'],
    ['Quando una notifica fallisce?', 'Riprova dopo 30 secondi; dopo 5 tentativi va in `FAILED` con l\'errore salvato.'],
    ['Perché le missioni della centrale non notificano?', 'La chat del cliente non è nota: la riga nasce `SKIPPED`, senza telefono.'],
  ]],
];

export const alternative = [
  ['Backend', 'Java 25 + Spring Boot', 'Node.js + Express', 'Sessione, CSRF, validazione e JPA già integrati. Con Node andrebbero scelte e combinate più librerie.', 'Ricostruita dal codice'],
  ['Build', 'Maven Wrapper', 'Gradle', 'Stessa versione per tutti con `mvnw`. Nessun confronto con Gradle nel repo.', 'Non documentata'],
  ['Database', 'PostgreSQL 18', 'MySQL', 'Indici parziali con WHERE (`uq_driver_active_live`, `uq_one_open_proposal`) non disponibili in questa forma.', 'Dal codice'],
  ['Database', 'Relazionale', 'NoSQL (MongoDB)', 'Relazioni e vincoli forti tra missioni, clienti e autisti; transazioni tra missione e outbox.', 'Ricostruita dal codice'],
  ['Schema', 'Script SQL numerati', 'ddl-auto di Hibernate', 'Modifiche esplicite e rileggibili; `validate` segnala gli script mancanti.', 'Documentata (README, TAD)'],
  ['Frontend', 'HTML, CSS, JavaScript', 'React', 'Niente build e niente Node (manuale tecnico). Il confronto con React non è registrato.', 'Ricostruita dal codice'],
  ['Mappa', 'Leaflet + OSM', 'Google Maps JS', 'Niente chiavi né fatturazione; disegna direttamente il GeoJSON.', 'Documentata (H-010, D-011)'],
  ['Percorsi', 'Nominatim e OSRM', 'Google Directions e Geocoding', 'Open source, senza chiavi né costi nel perimetro didattico.', 'Documentata (H-010)'],
  ['Canale', 'Telegram', 'WhatsApp Business API', 'API gratuita e aperta, nessuna approvazione Meta, adatta alla demo. WhatsApp è fuori ambito (D-009).', 'Brief del team; D-009 per l\'ambito'],
  ['Ingresso bot', 'Long polling', 'Webhook', 'Non serve un indirizzo pubblico né ngrok.', 'Documentata (H-009)'],
  ['Libreria bot', 'Sola libreria standard', 'python-telegram-bot', 'Nessuna dipendenza da installare. Nessun confronto registrato.', 'Dal codice'],
  ['Linguaggio bot', 'Python', 'Java nel backend', 'Bot indipendente, solo HTTP. Motivazione formale assente.', 'Non documentata'],
  ['Dialogo', 'Flusso a stati', 'AI / Gemini', 'Prevedibile e verificabile. Il dialogo con Gemini è facoltativo.', 'Documentata (D-005)'],
  ['Notifiche', 'Outbox su PostgreSQL', 'Invio diretto o Kafka/RabbitMQ', 'Atomicità con la missione, nessun servizio in più.', 'Outbox documentato (D-015); confronto ricostruito'],
  ['Autenticazione', 'Sessione + CSRF', 'JWT', 'Stessa origine, revoca immediata al logout, cookie HttpOnly.', 'Documentata (H-004)'],
  ['Denaro', 'BigDecimal, NUMERIC', 'double, FLOAT', 'Niente errori di arrotondamento; HALF_UP.', 'Documentata (H-003)'],
  ['Architettura', 'Monolite a livelli', 'Microservizi', 'Conservare monolite, livelli e API condivise.', 'Documentata (D-HARD-01)'],
];

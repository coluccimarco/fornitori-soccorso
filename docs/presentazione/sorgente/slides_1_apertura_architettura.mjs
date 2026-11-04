import { C, MEMBRI, pts, shot, card, two, three, motivi, svg, box, arrow, txt, zone, logo } from './lib.mjs';

const APE = 'Apertura', ARC = 'Architettura e scelte';

export default [
  {
    type: 'cover', section: APE, owner: MEMBRI.team, title: 'RecuVeicol',
    notes: {
      voce: "Buongiorno. Vi presentiamo RecuVeicol, il project work del corso ITS Developer 4.0. RecuVeicol è un sistema per gestire il soccorso stradale dal primo messaggio del cliente fino alla chiusura dell'intervento. Nel codice e nei documenti troverete anche il nome tecnico FORNITORI, che è il nome del repository: RecuVeicol è il marchio che compare sul sito pubblico. Nei prossimi minuti racconteremo quale problema abbiamo affrontato, come è fatta la soluzione e soprattutto perché abbiamo scelto proprio queste tecnologie. Ogni membro del team presenterà la parte di cui è responsabile. Poi faremo vedere il sistema funzionante, con una missione reale dall'inizio alla fine, su dati dimostrativi. Chiuderemo con i limiti che conosciamo, perché preferiamo dichiararli noi, e con le domande. Tutto ciò che mostriamo esiste nel codice: quando una scelta è ancora da confermare con lei, lo diremo apertamente.",
      semplice: "È la slide di apertura: nome, corso, data e team. RecuVeicol è il marchio, FORNITORI è il nome tecnico del repository e del pacchetto Java.",
      file: ['`README.md`', '`src/main/resources/static/index.html` (marchio RecuVeicol)'],
      domande: [
        ["Perché RecuVeicol se il repository si chiama fornitori?", "RecuVeicol è il marchio mostrato ai clienti sul sito pubblico; FORNITORI è il nome tecnico nato con il progetto. L'area riservata mostra ancora il logo «fornitori.»."],
        ["I dati che vediamo sono reali?", "No: sono dati sintetici presi dal seed del progetto. Il sistema è una demo e non è pronto per dati reali."],
      ],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'Team e ruoli',
    html: `<table class="t" style="font-size:25px"><tr><th style="width:170px">Membro</th><th style="width:190px">Area</th><th>Responsabili</th></tr>
      <tr><td>Membro 1</td><td>Backend</td><td>Vincenzo Minervini</td></tr>
      <tr><td>Membro 2</td><td>Frontend</td><td>Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio</td></tr>
      <tr><td>Membro 3</td><td>Chatbot</td><td>Marco Colucci</td></tr>
      <tr><td>Membro 4</td><td>Mappe</td><td>Marco Colucci</td></tr>
      <tr><td>Membro 5</td><td>Notifiche</td><td>Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi</td></tr></table>
      <p class="note-s" style="margin-top:26px;font-size:24px">Cinque aree, un contratto API condiviso: ogni componente parla agli altri solo tramite le API.</p>`,
    notes: {
      voce: "Il lavoro è diviso in cinque aree di responsabilità. Vincenzo Minervini è responsabile del backend, cioè del server, del database e del contratto delle API. Francesco Valeriano, Giuseppe Cangellosi e Marika Sancilio curano il frontend: la pagina pubblica, la centrale e l'area autista. Marco Colucci è responsabile del chatbot e delle mappe. Marika, Francesco e Giuseppe si occupano anche delle notifiche al cliente. La divisione non è solo organizzativa: si vede nel codice. Ogni area ha le proprie cartelle e i componenti comunicano soltanto attraverso API descritte in un unico file, `openapi.yaml`. Il bot, per esempio, non legge mai il database. Questo ci ha permesso di lavorare in parallelo: chi consumava un'API poteva rivedere il contratto prima che venisse implementato. Abbiamo lavorato per quattro sprint con uno Scrum adattato, e la tracciabilità dei requisiti è nei documenti.",
      semplice: "Chi fa cosa. Ogni area ha un responsabile e propri file; le aree si parlano tramite API e non toccano le tabelle degli altri.",
      file: ['`docs/team/README.md`', '`docs/team/INTEGRATION_CONTRACTS.md`', '`docs/agile/README.md`', '`docs/requirements/REQUIREMENTS_TRACEABILITY.md`'],
      domande: [
        ["Come vi siete coordinati?", "Con il contratto `api/openapi.yaml`: chi propone un cambio apre una issue, il Membro 1 aggiorna il contratto, i consumatori lo rivedono e poi si implementa."],
        ["Una persona segue due aree: è un rischio?", "Chatbot e mappe sono entrambe di Marco Colucci. I confini sono comunque separati nel codice, e le guide in `docs/team` permettono a un altro membro di riprendere il lavoro."],
      ],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'Il problema',
    html: two(pts([
      "Chi resta in panne vuole chiedere aiuto subito, anche dal telefono",
      "La centrale deve trovare un autista libero e vicino",
      "Se l'autista non risponde, la richiesta non deve fermarsi",
      "Il cliente vuole sapere chi sta arrivando",
      "Costi e storico devono restare verificabili",
    ]), `<div style="margin-top:6px">${svg(520, 400, [
      box(190, 10, 140, 70, 'Cliente', ['in panne'], 'yellow'),
      box(10, 165, 170, 80, 'Centrale', ['chi mando?'], 'blue'),
      box(340, 165, 170, 80, 'Autista', ['risponde?'], 'indigo'),
      box(165, 310, 190, 76, 'Missione', ['costo? storico?'], 'ink'),
      arrow(205, 82, 120, 160, '', { color: C.muted, dash: true }),
      arrow(315, 82, 400, 160, '', { color: C.muted, dash: true }),
      arrow(185, 205, 335, 205, '', { color: C.muted, dash: true, both: true }),
      arrow(120, 248, 200, 306, '', { color: C.muted, dash: true }),
      arrow(400, 248, 320, 306, '', { color: C.muted, dash: true }),
    ].join(''))}</div>`, '1.12fr .88fr'),
    notes: {
      voce: "Partiamo dal problema. Quando un automobilista resta fermo per strada ha bisogno di una cosa semplice: chiedere aiuto dal telefono e sapere chi arriva. Dall'altra parte c'è una centrale operativa che deve capire chi è il cliente, dove si trova e quale autista mandare, scegliendo tra chi è libero e vicino. Poi c'è l'autista, che può rifiutare o non rispondere: in quel caso la richiesta non deve restare ferma, e la centrale deve accorgersene. Infine, a intervento concluso, serve un costo calcolato con regole chiare e uno storico che permetta di ricostruire che cosa è successo. Il problema quindi non è una singola schermata: è coordinare tre persone diverse, su canali diversi, tenendo i dati coerenti anche quando più cose accadono nello stesso momento. Da qui nasce la nostra soluzione.",
      semplice: "Tre attori devono coordinarsi: il cliente chiede, la centrale assegna, l'autista interviene. Le difficoltà sono la mancata risposta, l'avviso al cliente e il costo corretto.",
      file: ['`docs/requirements/BUR_FORNITORI.md`', '`docs/requirements/FUD_FORNITORI.md`', '`docs/requirements/BUSINESS_RULES.md`'],
      domande: [
        ["Da dove vengono i requisiti?", "Dal PDF e dall'Excel del docente. Non sono nel repository: il BUR è una ricostruzione da confermare con lei."],
        ["Avete analizzato utenti reali?", "No: i dialoghi e i dati vengono dai materiali del corso. Non abbiamo svolto interviste con centrali o autisti."],
      ],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'La soluzione',
    html: two(pts([
      "<b>Chat Telegram</b>: il cliente apre la richiesta con la targa",
      "<b>Web centrale</b>: anagrafiche, missioni, proposta all'autista",
      "<b>Web autista</b>: accetta, vede la mappa, chiude con i km",
      "<b>Backend unico</b>: regole, costi e permessi sul server",
      "<b>Notifiche</b>: il cliente è avvisato in chat",
    ]), shot('sito_pubblico.png', 'Sito pubblico RecuVeicol: da qui si apre il bot', '--mh:400px'), '1.05fr .95fr'),
    notes: {
      voce: "La soluzione è un sistema con tre facce e un solo cervello. Il cliente non installa nulla: apre Telegram, scrive la targa e il bot lo guida con poche domande. La richiesta arriva alla centrale, che lavora in una pagina web: gestisce le anagrafiche, vede le missioni e propone l'intervento a un autista, anche con il pulsante «al più vicino». L'autista lavora nella stessa applicazione web, pensata anche per lo smartphone: vede la proposta, il conto alla rovescia e la mappa, accetta o rifiuta, e alla fine chiude la missione indicando i chilometri. Tutte le regole, cioè permessi, tariffe e scadenze, stanno sul server e mai nel browser. Quando l'autista accetta e quando l'intervento è concluso, il cliente riceve un messaggio automatico nella chat. Quello che vedete a destra è il sito pubblico, che spiega il servizio e porta direttamente al bot.",
      semplice: "Il cliente usa Telegram; centrale e autista usano il sito; le decisioni le prende sempre il backend, così nessuno può aggirarle dal browser.",
      file: ['`README.md`', '`src/main/resources/static/index.html`', '`chatbot/README.md`', '`docs/architecture/TAD_FORNITORI.md`'],
      domande: [
        ["Il cliente può chiedere soccorso dal sito?", "No: il sito è informativo e rimanda al bot. Le richieste entrano dalla chat o dalla centrale, che può inserirle a mano."],
        ["Perché un'unica applicazione web per centrale e autista?", "Condividono login e API; il server decide il ruolo e l'interfaccia mostra solo le sezioni di quel ruolo."],
      ],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'Attori e ruoli',
    html: `<div class="three">
      ${card('Cliente', pts(['Usa Telegram, senza login', 'Scrive targa, luogo e problema', 'Chiede lo stato, riceve gli avvisi']), C.yellow)}
      ${card('Centrale', pts(['Ruolo <b>RESPONSABILE</b>', 'Gestisce anagrafiche e missioni', 'Propone, corregge, rettifica il costo']), C.blue)}
      ${card('Autista', pts(['Ruolo <b>AUTISTA</b>', 'Accetta o rifiuta entro 10 minuti', 'Segnala, chiude e vede i guadagni']), C.indigo)}
      </div>
      <p class="big" style="margin-top:34px;text-align:center">I contatti del cliente sono visibili all'autista<br><b>solo dopo l'accettazione</b>.</p>`,
    notes: {
      voce: "Gli attori sono tre. Il cliente non ha un account: scrive al bot su Telegram e viene riconosciuto dalla targa. Può aprire una richiesta, chiedere lo stato scrivendo STATO e riceve gli avvisi automatici. La centrale corrisponde al ruolo RESPONSABILE: gestisce le anagrafiche, crea e assegna le missioni, vede dashboard e avvisi, corregge l'intervento e può rettificare un costo, ma solo indicando un motivo, che resta registrato. L'autista corrisponde al ruolo AUTISTA: vede soltanto le proprie missioni, ha dieci minuti per rispondere a una proposta, poi invia segnalazioni, chiude con i chilometri e consulta i propri guadagni. Un dettaglio a cui teniamo è la privacy: l'autista vede nome e telefono del cliente solo dopo aver accettato. I ruoli non sono un semplice nascondere pulsanti: è il server a controllarli a ogni richiesta, anche verificando che quella missione appartenga proprio a quell'autista.",
      semplice: "Due ruoli con login (centrale e autista) e un cliente senza login. Il controllo avviene sul server: nascondere un pulsante non basterebbe.",
      file: ['`src/main/java/it/exprivia/fornitori/config/SecurityConfig.java`', '`enums/UserRole.java`', '`service/MissionService.java` (metodi `requireRole`, `driverMissionDetail`)', '`docs/manuals/MANUALE_UTENTE.md`'],
      domande: [
        ["Un autista può vedere le missioni di un collega?", "No: il service verifica che la missione sia assegnata a lui, altrimenti risponde 403. Una vecchia proposta non dà accesso dopo una riassegnazione."],
        ["Come si creano nuovi utenti?", "Gli account arrivano dal seed. La gestione utenti da interfaccia è fuori ambito: un autista richiederebbe anche un mezzo dedicato."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Architettura generale',
    html: `<div class="center">${svg(1160, 520, [
      zone(345, 8, 465, 490, 'Il nostro sistema', C.blue),
      box(20, 60, 230, 86, 'Cliente', ['app Telegram'], 'yellow'),
      box(20, 215, 230, 86, 'Centrale e autista', ['browser'], 'yellow'),
      box(375, 52, 405, 92, 'Chatbot + dispatcher notifiche', ['Python, libreria standard'], 'teal'),
      box(375, 210, 405, 104, 'Backend Spring Boot', ['Java 25: API REST, sicurezza, regole', 'serve anche il frontend statico'], 'blue'),
      box(375, 395, 405, 92, 'PostgreSQL 18', ['dati, vincoli, outbox notifiche'], 'ink'),
      box(900, 52, 240, 92, 'Telegram', ['long polling'], 'light'),
      box(900, 210, 240, 104, 'Nominatim e OSRM', ['indirizzo → coordinate,', 'percorso e km'], 'light'),
      box(900, 400, 240, 92, 'OpenStreetMap', ['tile della mappa'], 'light'),
      arrow(250, 100, 370, 100, ''),
      arrow(785, 98, 895, 98, '', { both: true }),
      arrow(578, 148, 578, 205, 'API chat + token', { dx: 12, dy: 5, anchor: 'start' }),
      arrow(250, 258, 370, 258, ''),
      txt(145, 325, 'HTML, JSON,', { a: 'start', s: 15.5, w: 600, c: C.ink }),
      txt(145, 345, 'sessione + CSRF', { a: 'start', s: 15.5, w: 600, c: C.ink }),
      arrow(578, 318, 578, 390, 'JPA / SQL', { dx: 12, dy: 5, anchor: 'start' }),
      arrow(785, 262, 895, 262, ''),
      `<path d="M135 303 V505 H1020" fill="none" stroke="${C.violet}" stroke-width="2.4" stroke-dasharray="7 6"/>`,
      arrow(1020, 505, 1020, 496, '', { color: C.violet }),
      txt(145, 497, 'il browser scarica i tile', { a: 'start', c: C.violet, w: 600, s: 15.5 }),
    ].join(''))}</div>`,
    notes: {
      voce: "Questa è l'architettura. Al centro c'è il backend Spring Boot, l'unico componente che parla con il database PostgreSQL. A sinistra ci sono gli utenti. Centrale e autista usano il browser: il backend serve anche le pagine statiche, quindi tutto parte con un solo processo e le chiamate restano sulla stessa origine, con cookie di sessione e token CSRF. Il cliente usa Telegram. In alto c'è il bot Python: riceve i messaggi da Telegram con il long polling, quindi senza un indirizzo pubblico, e parla con il backend solo tramite le API chat, protette da un token. Lo stesso programma contiene il dispatcher che invia le notifiche. A destra ci sono i servizi esterni: Nominatim trasforma un indirizzo in coordinate, OSRM calcola percorso e chilometri, entrambi chiamati dal backend. I tile della mappa, invece, li scarica direttamente il browser da OpenStreetMap. Il punto chiave è che ogni freccia è un contratto esplicito.",
      semplice: "Un solo server centrale. Bot e browser non toccano mai il database: passano tutti dal backend. I servizi esterni sono usati solo per messaggi e mappe.",
      file: ['`docs/architecture/TAD_FORNITORI.md`', '`docs/diagrams/container.mmd`', '`config/SecurityConfig.java`', '`chatbot/fornitori_chatbot/http_backend.py`'],
      scelta: {
        alt: "Microservizi separati, oppure bot collegato direttamente al database.",
        perche: "Il registro decisioni (D-HARD-01) conferma monolite a livelli e API condivise. Il bot senza accesso al DB è il requisito NFR-005: le regole restano in un solo punto.",
        limiti: "Se il backend è spento si fermano web e bot, e non è stato provato un deploy reale con HTTPS. È accettabile per una demo; i messaggi in coda non si perdono perché stanno nel database.",
      },
      domande: [
        ["Perché non avete usato i microservizi?", "Per una demo con un solo team un monolite a livelli è più semplice da avviare e da testare. Il bot è già separato perché ha un linguaggio e un ciclo di vita propri."],
        ["Perché il frontend non è un'app separata?", "Spring Boot serve i file statici dalla stessa origine: niente CORS e cookie di sessione più semplici. Si avvia tutto con un comando."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Tecnologie: cosa e perché',
    html: `<table class="t" style="font-size:20px"><tr><th style="width:130px">Livello</th><th style="width:345px">Tecnologia</th><th style="width:150px">Linguaggio</th><th>Perché</th></tr>
      <tr><td>Frontend</td><td>HTML, CSS, Bootstrap 5.3, Leaflet 1.9</td><td>JavaScript</td><td>Nessuna build: lo serve Spring Boot</td></tr>
      <tr><td>Backend</td><td>Spring Boot 4.1.2, Security, Data JPA</td><td>Java 25</td><td>Sicurezza, validazione e dati già integrati</td></tr>
      <tr><td>Database</td><td>PostgreSQL 18, script numerati</td><td>SQL</td><td>Vincoli e transazioni proteggono le missioni</td></tr>
      <tr><td>Chatbot</td><td>Telegram Bot API, long polling</td><td>Python 3.10+</td><td>Solo libreria standard, nessun indirizzo pubblico</td></tr>
      <tr><td>Mappe</td><td>Nominatim, OSRM, OpenStreetMap</td><td>Java, JS</td><td>Open source, senza chiavi a pagamento</td></tr>
      <tr><td>Notifiche</td><td>Outbox su PostgreSQL + dispatcher</td><td>Java, Python</td><td>Nessun messaggio perso in silenzio</td></tr>
      <tr><td>Build</td><td>Maven Wrapper 3.9.9, GitHub Actions, OpenAPI 3.0.3</td><td>XML, YAML</td><td>Stessa build per tutti, in locale e in CI</td></tr></table>`,
    notes: {
      voce: "In questa tabella c'è tutto lo stack, livello per livello. Il frontend è fatto con HTML, CSS e JavaScript, con Bootstrap per il layout e Leaflet per la mappa: non serve alcuna compilazione. Il backend è in Java 25 con Spring Boot, che porta già sicurezza, validazione e accesso ai dati. Il database è PostgreSQL 18, gestito con script SQL numerati. Il chatbot è in Python e usa solo la libreria standard, quindi si avvia senza installare nulla. Per le mappe usiamo Nominatim e OSRM, servizi open source che non richiedono chiavi a pagamento. Le notifiche usano il pattern outbox: una tabella del database fa da coda e il bot le spedisce. Infine, Maven Wrapper garantisce a tutti la stessa versione di Maven, GitHub Actions ripete build e test a ogni modifica e OpenAPI descrive il contratto delle API. Nelle prossime slide giustifichiamo ogni scelta, una per livello, compreso quello che abbiamo scartato.",
      semplice: "Una mappa di tutte le tecnologie. Ricorda la regola: ogni riga ha un motivo pratico legato al progetto, non alla moda del momento.",
      file: ['`pom.xml`', '`.mvn/wrapper/maven-wrapper.properties`', '`.github/workflows/backend-ci.yml`', '`api/openapi.yaml`', '`src/main/resources/static/index.html`'],
      scelta: {
        alt: "L'elenco completo delle alternative è nell'appendice di questa guida.",
        perche: "Il repository registra lo stack come baseline (D-001) ma non un confronto formale per Java, Spring e PostgreSQL. I motivi in tabella si appoggiano su ciò che il codice usa davvero.",
        limiti: "Bootstrap e Leaflet arrivano da CDN: senza Internet l'interfaccia perde stile e mappa. Va quindi controllata la connessione prima della demo.",
      },
      domande: [
        ["Perché così tanti linguaggi?", "Java per le regole di business, Python per il bot, JavaScript perché è il linguaggio del browser. Comunicano in JSON, quindi ognuno resta sostituibile."],
        ["Ci sono package.json o requirements.txt?", "No. Il frontend non ha un package manager e il bot usa solo la libreria standard: le uniche dipendenze sono nel `pom.xml`."],
        ["Perché Java 25?", "È il target LTS condiviso dal team (H-005). `java.version` e `maven.compiler.release` sono impostati a 25."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.m1, title: 'Backend: Java, Spring Boot e Maven',
    html: two(`<div>${svg(560, 330, [
      box(10, 4, 540, 92, 'controller', ['riceve HTTP, valida il JSON'], 'indigo', { ts: 26, ss: 20 }),
      box(10, 120, 540, 92, 'service', ['regole, transazioni, permessi'], 'blue', { ts: 26, ss: 20 }),
      box(10, 236, 540, 92, 'repository + entity', ['JPA/Hibernate → PostgreSQL'], 'ink', { ts: 26, ss: 20 }),
      arrow(280, 97, 280, 118), arrow(280, 213, 280, 234),
    ].join(''))}<p class="note-s" style="margin:12px 0 0;text-align:center;font-size:21px">I DTO evitano di esporre le entità. Maven Wrapper: stessa versione per tutti.</p></div>`,
    `<div>${motivi('Perché Java e Spring Boot', [
      'Sicurezza integrata: sessione, CSRF, ruoli',
      'Validazione dichiarativa dei dati in ingresso',
      'JPA/Hibernate: lock e transazioni pronti',
    ])}<div style="height:22px"></div>${motivi('Perché Maven', [
      'Dipendenze in un unico <b>pom.xml</b>',
      'Il wrapper non richiede installazioni',
    ])}</div>`, '1fr 1fr'),
    notes: {
      voce: "Il backend è organizzato su tre livelli, ed è la cosa più importante da capire. Il controller riceve la richiesta HTTP e valida il JSON, senza logica. Il service contiene le regole e apre le transazioni: qui si decide se una missione può essere accettata. Il repository, con le entità JPA, parla con PostgreSQL. In mezzo ci sono i DTO, oggetti che definiscono ciò che entra ed esce, così la password o le entità non vengono mai esposte. Spring Boot ci dà tre cose che avremmo dovuto riscrivere: Spring Security per sessione, CSRF e ruoli; la validazione con annotazioni; JPA e Hibernate per transazioni e lock pessimistici, fondamentali quando due persone agiscono sulla stessa missione. Maven gestisce build e dipendenze in un unico file. Con il Maven Wrapper chiunque usa la stessa versione, la 3.9.9, senza installarla, e lo stesso comando vale anche nella pipeline.",
      semplice: "Tre strati, ognuno con un compito. Se cambia una regola tocchi il service; se cambia una query tocchi il repository. Spring evita di riscrivere sicurezza e accesso ai dati.",
      file: ['`pom.xml`', '`src/main/java/it/exprivia/fornitori/controller/`', '`service/MissionService.java`', '`repository/`', '`entity/`', '`dto/`', '`mvnw`'],
      scelta: {
        alt: "Node.js con Express; Python con Django; Gradle al posto di Maven.",
        perche: "Node e Django richiederebbero di scegliere e configurare a parte sicurezza e ORM. Nel repo non c'è un confronto formale: il motivo documentato è il target condiviso Java 25 (H-005, D-001), il resto deriva da ciò che il codice usa.",
        limiti: "`MissionService` è grande (715 righe) ed è debito tecnico noto. Le liste non sono paginate (V-23): con i dati della demo non è un problema.",
      },
      domande: [
        ["Perché non avete usato Node.js?", "Spring offre già sessione, CSRF, validazione e JPA in modo coerente e con lock pessimistici semplici da usare. Con Node avremmo dovuto assemblare più librerie."],
        ["Che differenza c'è tra entity e DTO?", "L'entity rispecchia la tabella; il DTO è ciò che viaggia in JSON. Separandoli non esponiamo per errore hash o colonne interne."],
        ["Cos'è il Maven Wrapper?", "Lo script `mvnw` scarica la versione di Maven indicata nel repository, così tutti compilano allo stesso modo."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.m1, title: 'Database: PostgreSQL e script numerati',
    html: two(`<div>${motivi('Perché un database relazionale', [
      'Chiavi esterne: niente missioni senza cliente',
      'Vincoli CHECK: stati e importi sempre validi',
      'Transazioni: missione e notifica insieme',
    ])}<div style="height:22px"></div>${motivi('Perché PostgreSQL', [
      '<b>Indice parziale</b>: un autista, una sola missione attiva',
      '<b>SKIP LOCKED</b> per la coda delle notifiche',
    ])}</div>`,
    `<div style="background:${C.canvas};border-radius:14px;padding:18px 22px">
      <h3 style="margin:0 0 10px;font-size:22px;font-weight:800">Script in ordine</h3>
      ${[['01', 'schema'], ['02', 'dati demo (seed)'], ['05-06', 'richieste da chat'], ['07', 'sicurezza'], ['08', 'stato di invio notifiche'], ['09', 'vincoli e indici'], ['10', 'fascia notturna 22:00']].map(([a, b]) => `<div style="display:flex;gap:14px;align-items:center;font-size:23px;font-weight:500;margin-bottom:10px"><span class="pill" style="min-width:76px;text-align:center">${a}</span>${b}</div>`).join('')}
      <p class="note-s" style="margin:12px 0 0">Hibernate solo in <b>validate</b>: se manca uno script, l'app non parte.</p></div>`, '1.08fr .92fr'),
    notes: {
      voce: "I dati di questo sistema sono fortemente collegati: una missione ha un cliente, un autista, forse un'officina, poi proposte, note ed eventi. Un database relazionale è la scelta naturale, perché le regole possono stare anche nel database. Le chiavi esterne impediscono una missione senza cliente; i vincoli CHECK impediscono uno stato inesistente o un costo negativo; le transazioni fanno sì che la missione accettata e la notifica vengano salvate insieme o non vengano salvate affatto. Di PostgreSQL usiamo in particolare gli indici parziali: un indice unico garantisce che un autista abbia al massimo una missione attiva, anche se due richieste arrivano nello stesso istante. E usiamo FOR UPDATE SKIP LOCKED per la coda delle notifiche. Lo schema nasce da script SQL numerati, da applicare in ordine: sono versionati e rileggibili. Hibernate non modifica mai le tabelle, si limita a controllarle all'avvio: se manca uno script, l'applicazione non parte.",
      semplice: "Il database è l'ultima difesa: anche con un bug nel codice, un autista non può avere due missioni attive. Gli script numerati sono la storia delle modifiche allo schema.",
      file: ['`database/01_schema.sql`', '`database/02_seed.sql`', '`database/08_cleanup_and_delivery.sql`', '`database/09_integrity_and_indexes.sql`', '`database/10_night_rate_22.sql`', '`src/main/resources/application.properties`', '`docs/architecture/DATABASE_DESIGN.md`'],
      scelta: {
        alt: "MySQL; un database NoSQL; schema generato da Hibernate.",
        perche: "MySQL non ha indici parziali con WHERE, usati da `uq_driver_active_live` e `uq_one_open_proposal`. NoSQL non offre lo stesso controllo su relazioni e vincoli. Lo schema generato nasconderebbe le modifiche: `ddl-auto=validate` le rende esplicite. Il repo non contiene un confronto formale: il motivo deriva dal codice.",
        limiti: "`fornitori_app` è proprietario delle tabelle (V-13, aperta): in produzione andrebbero separati ruolo delle migrazioni e ruolo applicativo. Gli script 03 e 04 non esistono: la numerazione ha un salto.",
      },
      domande: [
        ["Perché non avete usato MySQL?", "Il nostro vincolo «un autista, una missione attiva» è un indice parziale con WHERE, che MySQL non supporta in questa forma. Andrebbe spostato nel codice o simulato."],
        ["Perché non usare Flyway o Liquibase?", "Non sono nel `pom.xml`: gli script si applicano a mano con `psql`. È un miglioramento possibile; per la demo il controllo `validate` basta a segnalare gli script mancanti."],
        ["Come fate a non rovinare i dati storici?", "Le 30 missioni del seed hanno `is_fixture=TRUE`: lo scheduler e le azioni live le ignorano e non vengono ricalcolate."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.m2, title: 'Frontend: JavaScript senza framework e Leaflet',
    html: two(`<div>${motivi('Perché HTML, CSS e JavaScript', [
      'Nessuna compilazione né Node.js',
      'Servito da Spring Boot: stessa origine, sessione semplice',
      'Bootstrap per un layout pronto e responsive',
    ])}<div style="height:22px"></div>${motivi('Perché Leaflet', [
      'Libreria open source per mappe',
      'Disegna il percorso GeoJSON di OSRM',
      'Nessuna chiave a pagamento',
    ])}</div>`, shot('panoramica.png', 'Panoramica della centrale, schermata reale', '--mh:400px'), '1fr 1fr'),
    notes: {
      voce: "Per il frontend abbiamo usato HTML, CSS e JavaScript puro, con Bootstrap e Leaflet. Non c'è un framework come React e non c'è una fase di compilazione: i file stanno in `src/main/resources/static` e li serve direttamente Spring Boot. Questo ha tre vantaggi pratici. Primo, non serve installare Node.js: per avviare il progetto basta il comando del backend. Secondo, pagine e API hanno la stessa origine, quindi cookie di sessione e token CSRF funzionano senza configurare CORS. Terzo, Bootstrap ci dà un layout responsive, utile perché l'autista lavora dal telefono. Per la mappa usiamo Leaflet, una libreria open source che riceve direttamente la geometria GeoJSON calcolata da OSRM e la disegna sui tile di OpenStreetMap. Il prezzo di questa semplicità è un unico file `app.js` di quasi 1.400 righe: non è il massimo, ma è tutto in un posto e non dipende da build.",
      semplice: "Il browser riceve file pronti e li esegue così come sono. Niente passaggi intermedi: meno strumenti, avvio più semplice. In cambio il codice è meno modulare.",
      file: ['`src/main/resources/static/index.html`', '`static/app.js`', '`static/styles.css`', '`static/public-site.css`', '`docs/team/MEMBRO_2_FRONTEND.md`'],
      scelta: {
        alt: "React, Angular o Vue; Google Maps al posto di Leaflet.",
        perche: "Un framework aggiungerebbe Node.js e una build, che il manuale tecnico esclude esplicitamente. Il repo documenta la scelta (D-010) ma non il confronto con React: il motivo è ricostruito dalla struttura del codice.",
        limiti: "`app.js` è un file unico; gli aggiornamenti usano un controllo ogni 15 secondi; Bootstrap e Leaflet arrivano da CDN, con hash di integrità. Servono quindi Internet e un ridisegno manuale delle viste.",
      },
      domande: [
        ["Perché non avete usato React?", "Le schermate sono poche e guidate dal server. React avrebbe richiesto Node e una build; qui basta avviare il backend. Con più schermate lo rivaluteremmo."],
        ["Come si evita l'XSS senza framework?", "I dati dinamici passano da una funzione di escaping (`safe`), e la Content-Security-Policy limita gli script alle fonti ammesse."],
        ["Il sito funziona senza Internet?", "No: Bootstrap, Leaflet e i tile arrivano da servizi esterni. In locale si vede la pagina, ma senza stili e mappa."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.m3 + ' · ' + MEMBRI.m5, title: 'Chatbot e notifiche: Python, Telegram, outbox',
    html: `<div class="three">
      ${motivi('Perché Python', ['Solo libreria standard', 'Avvio immediato, senza installazioni', 'Dialogo a stati facile da leggere'])}
      ${motivi('Telegram, non WhatsApp', ['API gratuita e aperta', 'Nessuna approvazione Meta', 'Adatta alla demo: niente server pubblico'])}
      ${motivi('Perché l\'outbox', ['Evento salvato con la missione', 'Nessun messaggio perso in silenzio', 'Nessun doppio invio tra dispatcher'])}
      </div>
      <div style="margin-top:22px">${svg(1170, 130, [
      box(10, 25, 225, 80, 'Missione accettata', [], 'blue', { ts: 19 }),
      box(315, 25, 235, 80, 'Riga in outbox', ['stessa transazione'], 'ink', { ts: 19 }),
      box(630, 25, 235, 80, 'Bot riserva e invia', ['ogni 5 secondi'], 'teal', { ts: 19 }),
      box(945, 25, 215, 80, 'Telegram', ['al cliente'], 'yellow', { ts: 19 }),
      arrow(238, 65, 311, 65), arrow(553, 65, 626, 65), arrow(868, 65, 941, 65),
    ].join(''))}</div>
      <p class="note-s" style="text-align:center;margin:0;font-size:20px">Caso limite noto: se il bot si ferma tra invio ed esito, quel messaggio può ripartire una volta.</p>`,
    notes: {
      voce: "Tre scelte da giustificare. Il bot è in Python e usa solo la libreria standard: si avvia senza installare dipendenze e il dialogo si legge come una macchina a stati. Per il canale abbiamo scelto Telegram invece di WhatsApp. L'API di Telegram è gratuita e aperta, non serve l'approvazione di Meta e, con il long polling, non serve neppure un indirizzo pubblico: è ideale per una demo. Il canale WhatsApp resta fuori ambito. La terza scelta è il pattern outbox. Se il backend inviasse il messaggio mentre salva la missione, un guasto di Telegram bloccherebbe il lavoro, o la missione verrebbe annullata dopo che il messaggio è partito. Noi invece scriviamo l'evento in una tabella, nella stessa transazione della missione. Poi il bot riserva le righe e le invia. Così una notifica non si perde in silenzio e due dispatcher non ricevono la stessa riga. Resta un caso limite dichiarato: un crash tra invio ed esito può ripetere il messaggio.",
      semplice: "L'outbox è una lista di cose da spedire dentro il database. Prima si scrive «da inviare», poi qualcun altro spedisce e segna l'esito. Se il bot è spento, la lista aspetta.",
      file: ['`chatbot/fornitori_chatbot/engine.py`', '`chatbot/fornitori_chatbot/telegram_channel.py`', '`chatbot/fornitori_chatbot/notifications.py`', '`service/MissionService.java` (`addOutboxEvent`)', '`entity/NotificationOutbox.java`', '`docs/management/DECISION_LOG.md` (D-008, D-009, D-015, H-009)'],
      scelta: {
        alt: "Bot in Java dentro il backend o con la libreria python-telegram-bot; WhatsApp Business API; invio diretto senza outbox.",
        perche: "WhatsApp richiede approvazione Meta e un webhook pubblico. Il long polling è documentato in H-009. I motivi su API gratuita e approvazione Meta vengono dalla scelta del team: nel codice non ci sono prove. La scelta di Python non è motivata nei documenti: è il fatto che il bot non abbia dipendenze a sostenerla.",
        limiti: "Consegna «almeno una volta»: Telegram non offre chiavi anti-duplicati (V-14, accettata per la demo). Dopo 5 tentativi una riga va in FAILED: non è inviata, ma resta registrata con l'errore. Il PDF del docente può prevedere WhatsApp: va confermato.",
      },
      domande: [
        ["Perché non avete usato WhatsApp?", "Richiede account Business, approvazione Meta e un webhook pubblico, cioè costi e tempi fuori dalla demo. Il motore del bot è però separato dal canale: basta un adattatore."],
        ["Garantite che non ci siano duplicati?", "Tra dispatcher sì, grazie a SKIP LOCKED. Un doppio invio resta possibile solo se il bot si ferma tra invio ed esito. Lo dichiariamo."],
        ["Se Telegram è giù, la missione si blocca?", "No: la missione viene salvata comunque. La riga resta in coda, riprova dopo 30 secondi, fino a 5 tentativi."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Integrazione: componenti e contratti',
    html: two(`<div>${motivi('Componenti separati', [
      'Ognuno si può sviluppare e provare da solo',
      'Il bot ha anche un backend finto per la console',
      'Regole in un solo posto: il server',
    ])}<div style="height:22px"></div>${motivi('Nominatim e OSRM', [
      'Open source e gratuiti',
      'Nessuna chiave a pagamento',
      'Chiamati dal backend, con timeout',
    ])}</div>`, `<div>
      <div style="background:${C.ink};color:#fff;border-radius:12px;padding:18px 20px;font-family:JetBrains,monospace;font-size:18.5px;line-height:1.42">POST /api/v1/chat/lookup-targa<br>{ "sessionId": "…", "targa": "PW660SW" }<br><br>200 OK<br>{ "verificato": true, "clienteId": 1,<br>&nbsp;&nbsp;"clienteNome": "Lucia Gallo",<br>&nbsp;&nbsp;"compagniaNome": "Polizza Sicura…" }</div>
      <div style="display:flex;gap:14px;margin-top:18px">
        <div class="card" style="--c:${C.blue};flex:1;text-align:center;padding:14px"><h3 style="font-size:36px;margin:0">34</h3><div style="font-size:18px">operazioni attive</div></div>
        <div class="card" style="--c:${C.muted};flex:1;text-align:center;padding:14px"><h3 style="font-size:36px;margin:0">4</h3><div style="font-size:18px">progettate, fuori ambito</div></div>
        <div class="card" style="--c:${C.yellow};flex:1;text-align:center;padding:14px"><h3 style="font-size:24px;margin:7px 0 7px">OpenAPI</h3><div style="font-size:18px">unica fonte</div></div>
      </div></div>`, '1fr 1.08fr'),
    notes: {
      voce: "Come stanno insieme i pezzi. Abbiamo separato i componenti e li abbiamo collegati con API REST che scambiano JSON. Qui vedete un esempio reale: il bot manda sessione e targa, il backend risponde se la targa è riconosciuta, con nome e compagnia. Tutte le API sono descritte in `openapi.yaml`, che è la fonte ufficiale: contiene 38 operazioni, di cui 34 implementate e quattro solo progettate, cioè gestione utenti e webhook, che sono fuori ambito. Un test confronta il contratto con il codice. Il vantaggio della separazione si vede nel bot: ha un'interfaccia verso il backend con due implementazioni, una HTTP reale e una finta, così si può provare da console senza server. Per le mappe usiamo Nominatim e OSRM, open source e gratuiti, senza chiavi a pagamento. Li chiama sempre il backend, con timeout e limiti, così il browser non parla mai con loro e una loro caduta produce un errore chiaro.",
      semplice: "REST significa: richieste HTTP con dati JSON. Il contratto è l'accordo scritto tra chi offre l'API e chi la usa; se cambia, tutti lo devono rivedere.",
      file: ['`api/openapi.yaml`', '`docs/team/INTEGRATION_CONTRACTS.md`', '`docs/architecture/API_SPECIFICATION.md`', '`chatbot/fornitori_chatbot/backend.py`', '`chatbot/fornitori_chatbot/fake_backend.py`', '`service/NominatimGeocodingClient.java`', '`service/OsrmRoutingClient.java`', '`src/test/java/.../ContractAndProviderTests.java`'],
      scelta: {
        alt: "GraphQL o gRPC per le API; Google Maps o Mapbox per le mappe.",
        perche: "REST con JSON è leggibile da Java, Python e browser senza librerie extra. H-010 documenta la scelta: servizi pubblici senza chiavi e costi nel perimetro didattico.",
        limiti: "Nominatim e OSRM pubblici non garantiscono disponibilità (R-04) e ricevono gli indirizzi: adatto alla demo, da rivalutare per dati reali. Errori espliciti e chiusura sempre possibile.",
      },
      domande: [
        ["Perché non avete usato Google Maps?", "Richiede una chiave con fatturazione. Nominatim e OSRM bastano per una demo e sono open source; in produzione useremmo un provider con garanzie."],
        ["Che cosa succede se il contratto e il codice divergono?", "I test in `ContractAndProviderTests` confrontano i due. Il processo prevede di aggiornare OpenAPI prima di implementare."],
        ["Perché il browser non chiama direttamente OSRM?", "Il backend applica permessi e limiti di frequenza e salva i km stimati. La CSP ammette connessioni solo verso la stessa origine."],
      ],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: "Flusso end-to-end di una missione",
    html: `<div class="center">${svg(1170, 525, [
      txt(90, 22, 'Cliente', { w: 800, s: 19, c: C.ink }), txt(320, 22, 'Bot', { w: 800, s: 19, c: C.ink }),
      txt(585, 22, 'Backend + DB', { w: 800, s: 19, c: C.ink }), txt(840, 22, 'Centrale', { w: 800, s: 19, c: C.ink }), txt(1070, 22, 'Autista', { w: 800, s: 19, c: C.ink }),
      ...[90, 320, 585, 840, 1070].map((x) => `<line x1="${x}" y1="32" x2="${x}" y2="520" stroke="#c6cedb" stroke-width="2" stroke-dasharray="4 6"/>`),
      arrow(90, 62, 318, 62, '1 Targa, luogo, problema'),
      arrow(320, 98, 583, 98, '2 Lookup e creazione missione', {}),
      txt(600, 128, 'stato: nuova', { a: 'start', c: C.blue, w: 700 }),
      arrow(838, 160, 587, 160, '3 Proponi al più vicino', { dy: -8 }),
      txt(600, 188, 'stato: proposta · scadenza 10 min', { a: 'start', c: C.blue, w: 700 }),
      arrow(587, 222, 1068, 222, '4 Compare la proposta (entro 15 s)', { dx: 80 }),
      arrow(1068, 266, 587, 266, '5 Accetta', { dx: 150 }),
      txt(600, 295, 'stato: accettata + riga outbox, stessa transazione', { a: 'start', c: C.blue, w: 700 }),
      arrow(583, 330, 322, 330, '6 Bot riserva la notifica'),
      arrow(318, 368, 92, 368, '7 «Autista: Luca Bianchi»', { lc: C.ink }),
      arrow(1068, 408, 587, 408, '8 Chiude con i km', { dx: 150 }),
      txt(600, 437, 'stato: eseguita · costo calcolato dal server', { a: 'start', c: C.blue, w: 700 }),
      arrow(583, 472, 322, 472, '9 Bot riserva'),
      arrow(318, 505, 92, 505, '10 «Intervento concluso»'),
    ].join(''))}</div>`,
    notes: {
      voce: "Ecco una missione dall'inizio alla fine, ed è esattamente ciò che vedrete nella demo. Il cliente scrive al bot targa, luogo e problema. Il bot controlla la targa e crea la missione tramite l'API: lo stato è nuova. La centrale la vede e preme «Proponi al più vicino»: il backend sceglie l'autista libero più vicino, lo stato diventa proposta e parte una scadenza di dieci minuti. Entro quindici secondi la proposta compare nell'area dell'autista. L'autista accetta: nella stessa transazione la missione diventa accettata e nasce la riga di notifica. Il bot, che controlla ogni cinque secondi, la riserva e la invia: il cliente legge il nome dell'autista. Alla fine l'autista chiude indicando i chilometri, il server calcola il costo, e parte l'ultima notifica. Osservate che il bot e l'interfaccia non si parlano mai direttamente: passa tutto dal backend, e lo stato vive nel database.",
      semplice: "Segui i numeri dall'alto in basso. Gli stati della missione stanno sempre nel backend; bot e pagine web mostrano solo una vista di quello stato.",
      file: ['`docs/diagrams/mission_states.mmd`', '`docs/diagrams/chat_notifications_sequence.mmd`', '`service/ChatService.java`', '`service/NearestDriverService.java`', '`service/MissionService.java`', '`chatbot/fornitori_chatbot/notifications.py`'],
      domande: [
        ["Se l'autista non risponde?", "Uno scheduler controlla ogni 30 secondi le scadenze: la missione diventa scaduta e la centrale riceve un avviso. Il controllo vale anche nell'istante della risposta."],
        ["E se rifiuta?", "Lo stato diventa non accettata, parte l'avviso e la centrale ripropone. «Al più vicino» esclude chi ha già risposto; lo storico delle proposte resta."],
        ["La proposta arriva in tempo reale?", "Quasi: la pagina controlla ogni 15 secondi. Non usiamo WebSocket, per tenere semplice il client."],
      ],
    },
  },
];

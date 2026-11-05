import { C, MEMBRI, pts, shot, card, two, motivi, svg, box, arrow, txt, zone } from './lib.mjs';

const APE = 'Apertura', ARC = 'Architettura e scelte', CH = 'Chiusura';
const BE = 'Backend · Vincenzo Minervini';
const FE = 'Frontend · Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio';
const BOT = 'Chatbot · Marco Colucci';
const MAP = 'Mappe · Marco Colucci';
const NOT = 'Notifiche · Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi';

export default [
  {
    type: 'cover', section: APE, owner: MEMBRI.team, title: 'RecuVeicol',
    notes: {
      voce: "Buongiorno, vi presentiamo RecuVeicol, il nostro project work del corso ITS Developer 4.0. È un sistema che gestisce il soccorso stradale dal primo messaggio del cliente fino alla chiusura dell'intervento. Vedremo il problema, la soluzione, le tecnologie scelte e poi il sistema in funzione.",
      semplice: "RecuVeicol è il marchio; FORNITORI è il nome tecnico del repository.",
      file: ['`README.md`'],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'Team e ruoli',
    html: `<table class="t" style="font-size:25px"><tr><th style="width:190px">Area</th><th>Responsabili</th></tr>
      <tr><td>Backend</td><td>Vincenzo Minervini</td></tr>
      <tr><td>Frontend</td><td>Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio</td></tr>
      <tr><td>Chatbot</td><td>Marco Colucci</td></tr>
      <tr><td>Mappe</td><td>Marco Colucci</td></tr>
      <tr><td>Notifiche</td><td>Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi</td></tr></table>`,
    notes: {
      voce: "Il lavoro è diviso in cinque aree. Vincenzo cura il backend e il database. Francesco, Giuseppe e Marika curano il frontend e le notifiche. Marco cura il chatbot e le mappe. Ogni area ha i propri file e comunica con le altre solo tramite API.",
      semplice: "Ogni area ha un responsabile; le parti si parlano solo tramite API.",
      file: ['`docs/team/README.md`'],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'Il problema',
    html: pts([
      "Chi resta in panne vuole chiedere aiuto subito, dal telefono",
      "La centrale deve trovare un autista libero e vicino",
      "Il cliente vuole sapere quando arriva l'aiuto",
    ]),
    notes: {
      voce: "Chi resta fermo per strada vuole chiedere aiuto dal telefono e sapere che qualcuno sta arrivando. La centrale deve capire dove si trova il cliente e scegliere un autista libero e vicino. Se l'autista non risponde, la richiesta non deve fermarsi. Il problema è coordinare queste tre persone tenendo i dati coerenti.",
      semplice: "Cliente, centrale e autista devono coordinarsi su canali diversi.",
      file: ['`docs/requirements/BUR_FORNITORI.md`'],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'La soluzione',
    html: two(pts([
      "<b>Telegram</b>: il cliente apre la richiesta",
      "<b>Sito web</b>: la centrale controlla, l'autista interviene",
      "<b>Server unico</b>: regole e costi in un solo posto",
    ]), shot('sito_pubblico.png', 'Sito pubblico RecuVeicol', '--mh:400px'), '1.05fr .95fr'),
    notes: {
      voce: "Il cliente scrive al bot su Telegram, senza installare altro. Il server la propone all'autista più vicino e la centrale segue i casi critici. L'autista lavora dalla stessa applicazione web, anche da telefono: accetta, vede la mappa e chiude l'intervento. Tutte le regole stanno sul server, e il cliente riceve gli aggiornamenti in chat.",
      semplice: "Il cliente usa Telegram, centrale e autista il sito; decide sempre il server.",
      file: ['`README.md`', '`docs/architecture/TAD_FORNITORI.md`'],
    },
  },
  {
    section: APE, owner: MEMBRI.team, title: 'Attori e ruoli',
    html: `<div class="three">
      ${card('Cliente', pts(['Usa Telegram', 'Dà targa, telefono, luogo, problema']), C.yellow)}
      ${card('Centrale', pts(['Gestisce le anagrafiche', 'Segue i casi critici']), C.blue)}
      ${card('Autista', pts(['Accetta o rifiuta', 'Chiude con i chilometri']), C.indigo)}
      </div>`,
    notes: {
      voce: "Gli attori sono tre. Il cliente non ha un account e scrive al bot. La centrale gestisce anagrafiche e missioni. L'autista vede solo le proprie missioni: accetta o rifiuta entro dieci minuti e alla fine chiude con i chilometri. I ruoli sono controllati dal server a ogni richiesta.",
      semplice: "Centrale e autista hanno un login con un ruolo; il cliente usa solo il bot.",
      file: ['`config/SecurityConfig.java`', '`docs/manuals/MANUALE_UTENTE.md`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Architettura generale',
    html: `<div class="center">${svg(1160, 500, [
      zone(345, 8, 465, 480, 'Il nostro sistema', C.blue),
      box(20, 60, 230, 86, 'Cliente', ['app Telegram'], 'yellow'),
      box(20, 215, 230, 86, 'Centrale e autista', ['browser'], 'yellow'),
      box(375, 52, 405, 92, 'Chatbot e notifiche', ['Python'], 'teal'),
      box(375, 210, 405, 104, 'Backend e frontend', ['Java, Spring Boot'], 'blue'),
      box(375, 385, 405, 88, 'Database', ['PostgreSQL'], 'ink'),
      box(900, 52, 240, 92, 'Telegram', [], 'light'),
      box(900, 210, 240, 104, 'Nominatim, OSRM', ['indirizzi e percorsi'], 'light'),
      arrow(250, 100, 370, 100), arrow(785, 98, 895, 98, '', { both: true }),
      arrow(578, 148, 578, 205, 'API', { dx: 12, dy: 5, anchor: 'start' }),
      arrow(250, 258, 370, 258), arrow(578, 318, 578, 380), arrow(785, 262, 895, 262),
    ].join(''))}</div>`,
    notes: {
      voce: "Al centro c'è il backend, l'unico componente che parla con il database. Serve anche le pagine web a centrale e autista. Il bot Python riceve i messaggi da Telegram e chiama il backend tramite API. Per indirizzi e percorsi il backend usa due servizi esterni, Nominatim e OSRM.",
      semplice: "Un solo server centrale: bot e browser non toccano mai il database.",
      file: ['`docs/architecture/TAD_FORNITORI.md`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Tecnologie: cosa e perché',
    html: `<table class="t" style="font-size:21px"><tr><th style="width:150px">Livello</th><th style="width:330px">Tecnologia</th><th style="width:170px">Linguaggio</th><th>Perché</th></tr>
      <tr><td>Frontend</td><td>Bootstrap, Leaflet</td><td>JavaScript</td><td>Nessuna compilazione</td></tr>
      <tr><td>Backend</td><td>Spring Boot</td><td>Java 25</td><td>Sicurezza e dati integrati</td></tr>
      <tr><td>Database</td><td>PostgreSQL 18</td><td>SQL</td><td>Vincoli e transazioni</td></tr>
      <tr><td>Chatbot</td><td>Telegram</td><td>Python</td><td>Nessuna dipendenza</td></tr>
      <tr><td>Mappe</td><td>Nominatim, OSRM</td><td>Java, JavaScript</td><td>Open source e gratuiti</td></tr>
      <tr><td>Notifiche</td><td>Outbox e dispatcher</td><td>Java, Python</td><td>Messaggi in coda sicura</td></tr>
      <tr><td>Build</td><td>Maven, GitHub Actions</td><td>XML, YAML</td><td>Stessa build per tutti</td></tr></table>`,
    notes: {
      voce: "Questa tabella riassume lo stack. Il frontend non richiede compilazione. Spring Boot offre già sicurezza e accesso ai dati. PostgreSQL protegge i dati con vincoli e transazioni. Il bot usa solo la libreria standard di Python. Mappe gratuite e senza chiavi. Maven garantisce a tutti la stessa build.",
      semplice: "Ogni tecnologia ha un motivo pratico legato al progetto.",
      file: ['`pom.xml`', '`.github/workflows/backend-ci.yml`', '`api/openapi.yaml`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.m1, title: 'Backend: Java, Spring Boot e Maven',
    html: two(`<div>${svg(560, 330, [
      box(10, 4, 540, 92, 'controller', ['riceve le richieste'], 'indigo', { ts: 26, ss: 20 }),
      box(10, 120, 540, 92, 'service', ['applica le regole'], 'blue', { ts: 26, ss: 20 }),
      box(10, 236, 540, 92, 'repository', ['legge e scrive i dati'], 'ink', { ts: 26, ss: 20 }),
      arrow(280, 97, 280, 118), arrow(280, 213, 280, 234),
    ].join(''))}</div>`, motivi('Perché', [
      'Sicurezza già integrata',
      'Validazione dei dati in ingresso',
      'Maven: dipendenze in un solo file',
    ]), '1fr 1fr'),
    notes: {
      voce: "Il backend ha tre livelli. Il controller riceve le richieste, il service applica le regole, il repository parla con il database. Spring Boot ci evita di riscrivere sicurezza, validazione e accesso ai dati. Maven gestisce build e dipendenze, e il wrapper fa usare a tutti la stessa versione.",
      semplice: "Tre strati, ognuno con un solo compito.",
      file: ['`pom.xml`', '`controller/`', '`service/`', '`repository/`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.m1, title: 'Database: PostgreSQL e script numerati',
    html: two(motivi('Perché relazionale', [
      'Chiavi esterne: niente missioni senza cliente',
      'Vincoli: stati e importi sempre validi',
      'Transazioni: missione e notifica insieme',
    ]), motivi('Perché gli script numerati', [
      'Schema versionato e rileggibile',
      'Si ricostruisce in ordine da zero',
      'Se manca uno script, l\'app non parte',
    ]), '1fr 1fr'),
    notes: {
      voce: "I dati sono molto collegati, quindi abbiamo scelto un database relazionale. Le chiavi esterne e i vincoli impediscono dati incoerenti, e le transazioni salvano missione e notifica insieme. PostgreSQL ci permette anche di garantire che un autista abbia una sola missione attiva. Lo schema nasce da script SQL numerati.",
      semplice: "Il database è l'ultima difesa: rifiuta i dati non validi.",
      file: ['`database/`', '`docs/architecture/DATABASE_DESIGN.md`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.m2, title: 'Frontend: JavaScript e Leaflet',
    html: two(motivi('Perché', [
      'Nessuna compilazione né Node.js',
      'Pagine servite dal backend',
      'Leaflet: mappe open source',
    ]), shot('dashboard_operativa.png', 'Dashboard operativa della centrale', '--mh:400px'), '.8fr 1.2fr'),
    notes: {
      voce: "Il frontend usa HTML, CSS e JavaScript, con Bootstrap per il layout e Leaflet per la mappa. Non c'è compilazione: i file li serve direttamente Spring Boot, quindi per avviare tutto basta il backend. Bootstrap rende le pagine adatte anche al telefono, utile per l'autista.",
      semplice: "Il browser riceve file pronti: meno strumenti, avvio più semplice.",
      file: ['`static/index.html`', '`static/app.js`', '`static/styles.css`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.m3 + ' · ' + MEMBRI.m5, title: 'Chatbot e notifiche: Python, Telegram, outbox',
    html: `<div class="three">
      ${motivi('Python', ['Solo libreria standard', 'Avvio immediato'])}
      ${motivi('Telegram, non WhatsApp', ['API gratuita e aperta', 'Nessuna approvazione Meta', 'Adatta alla demo'])}
      ${motivi('Outbox', ['Evento salvato con la missione', 'Se il bot è spento, i messaggi aspettano'])}
      </div>`,
    notes: {
      voce: "Il bot è in Python e usa solo la libreria standard, quindi si avvia senza installare nulla. Abbiamo scelto Telegram perché l'API è gratuita e aperta, non serve l'approvazione di Meta ed è adatta alla demo. Con il pattern outbox la notifica viene salvata insieme alla missione e inviata subito dopo.",
      semplice: "L'outbox è una lista di messaggi da spedire, conservata nel database.",
      file: ['`chatbot/fornitori_chatbot/engine.py`', '`chatbot/fornitori_chatbot/notifications.py`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Integrazione: API e servizi esterni',
    html: two(motivi('API REST', [
      'Componenti separati, dati in JSON',
      'Un unico contratto: OpenAPI',
    ]), motivi('Nominatim e OSRM', [
      'Open source e gratuiti',
      'Nessuna chiave a pagamento',
    ]), '1fr 1fr'),
    notes: {
      voce: "I componenti sono separati e si scambiano dati in JSON tramite API REST, descritte in un unico file OpenAPI. Così ognuno può lavorare sulla propria parte senza rompere le altre. Per le mappe usiamo Nominatim e OSRM, open source e gratuiti, senza chiavi a pagamento.",
      semplice: "Il contratto è l'accordo scritto tra chi offre un'API e chi la usa.",
      file: ['`api/openapi.yaml`', '`docs/team/INTEGRATION_CONTRACTS.md`'],
    },
  },
  {
    section: ARC, owner: MEMBRI.team, title: 'Il flusso di una missione',
    html: `<div class="center">${svg(1170, 420, [
      box(10, 30, 260, 110, '1 Cliente', ['scrive al bot'], 'yellow', { ts: 25, ss: 20 }),
      box(455, 30, 260, 110, '2 Server', ['propone al più vicino'], 'blue', { ts: 25, ss: 20 }),
      box(900, 30, 260, 110, '3 Autista', ['accetta'], 'indigo', { ts: 25, ss: 20 }),
      box(900, 270, 260, 110, '4 Cliente', ['riceve l\'avviso'], 'yellow', { ts: 25, ss: 20 }),
      box(455, 270, 260, 110, '5 Autista', ['chiude con i km'], 'indigo', { ts: 25, ss: 20 }),
      box(10, 270, 260, 110, '6 Server', ['calcola il costo, avvisa'], 'teal', { ts: 25, ss: 20 }),
      arrow(274, 85, 451, 85), arrow(719, 85, 896, 85), arrow(1030, 144, 1030, 266), arrow(896, 325, 719, 325), arrow(451, 325, 274, 325),
    ].join(''))}</div>`,
    notes: {
      voce: "Ecco una missione completa. Il cliente scrive al bot e nasce la richiesta. Il server la propone all'autista più vicino, che ha dieci minuti per rispondere. Quando accetta, il cliente riceve un avviso, senza il suo nome. A fine intervento l'autista inserisce i chilometri, il server calcola il costo e parte l'ultimo avviso.",
      semplice: "Lo stato della missione vive nel backend; bot e pagine lo mostrano.",
      file: ['`docs/diagrams/mission_states.mmd`', '`service/MissionService.java`'],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'API e sicurezza',
    html: two(pts(['42 operazioni REST in JSON', 'Anagrafiche, missioni, chat, mappe']), pts(['Login con password cifrata', 'Protezione CSRF', 'Ruoli controllati dal server']), '1fr 1fr'),
    notes: {
      voce: "Il backend offre 42 operazioni REST per anagrafiche, missioni, chat e mappe. Per la sicurezza le password sono salvate con BCrypt, un token CSRF impedisce ad altri siti di agire al posto dell'utente e il server controlla il ruolo a ogni richiesta. Il bot ha un token di servizio separato.",
      semplice: "Sessione: chi sei. CSRF: sei stato davvero tu. Ruolo: cosa puoi fare.",
      file: ['`config/SecurityConfig.java`', '`controller/`', '`api/openapi.yaml`'],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'Logica delle missioni',
    html: `<div>${svg(1170, 120, [
      box(10, 20, 200, 80, 'nuova', [], 'soft'), box(330, 20, 200, 80, 'proposta', [], 'indigo'),
      box(650, 20, 200, 80, 'accettata', [], 'blue'), box(960, 20, 200, 80, 'eseguita', [], 'teal'),
      arrow(214, 60, 326, 60), arrow(534, 60, 646, 60), arrow(854, 60, 956, 60),
    ].join(''))}</div>
    <div class="three" style="margin-top:22px">
      ${card('Scadenza', '10 minuti per rispondere', C.yellow)}
      ${card('Tariffa', '€50 + €1,20 al km<br>+25% dalle 22:00 alle 08:00', C.blue)}
      ${card('Al più vicino', 'Automatico; dopo 3 esiti negativi decide la centrale', C.indigo)}
    </div>`,
    notes: {
      voce: "La missione passa da nuova a proposta, accettata ed eseguita; se l'autista rifiuta o non risponde passa al successivo. Dopo tre esiti negativi decide la centrale. La proposta vale dieci minuti. La tariffa è cinquanta euro più un euro e venti al chilometro, con il venticinque per cento in più di notte.",
      semplice: "La fascia 22:00-08:00 è una scelta del team; il PDF del docente indica 20:00-8:00.",
      file: ['`service/MissionService.java`', '`service/PricingService.java`', '`service/NearestDriverService.java`'],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'Database: schema semplificato',
    html: `<div class="center">${svg(1170, 420, [
      box(420, 150, 330, 110, 'missions', ['il centro di tutto'], 'blue', { ts: 26, ss: 19 }),
      box(20, 20, 260, 84, 'customers', ['clienti e targhe'], 'light', { ts: 22, ss: 18 }),
      box(455, 10, 260, 84, 'drivers', ['autisti e mezzi'], 'light', { ts: 22, ss: 18 }),
      box(890, 20, 260, 84, 'workshops', ['officine'], 'light', { ts: 22, ss: 18 }),
      box(20, 315, 260, 84, 'mission_proposals', ['ogni tentativo'], 'soft', { ts: 21, ss: 18 }),
      box(455, 315, 260, 84, 'mission_events', ['storico azioni'], 'soft', { ts: 22, ss: 18 }),
      box(890, 315, 260, 84, 'notifications_outbox', ['messaggi da inviare'], 'ink', { ts: 20, ss: 18 }),
      arrow(420, 170, 284, 100), arrow(585, 146, 585, 98), arrow(750, 170, 886, 100),
      arrow(420, 240, 284, 318), arrow(585, 264, 585, 311), arrow(750, 240, 886, 318),
    ].join(''))}</div>`,
    notes: {
      voce: "Al centro c'è la tabella delle missioni, collegata a cliente, autista e officina. Sotto ci sono le proposte, cioè ogni tentativo di assegnazione, lo storico delle azioni e la coda delle notifiche. In tutto le tabelle sono quattordici. Le trenta missioni storiche sono separate da quelle operative.",
      semplice: "La missione è il foglio centrale; le altre tabelle sono schede collegate.",
      file: ['`database/01_schema.sql`', '`docs/architecture/DATABASE_DESIGN.md`'],
    },
  },
  {
    section: FE, owner: MEMBRI.m2, title: 'Centrale operativa',
    html: `<div style="display:grid;grid-template-columns:1.1fr 1fr;gap:26px;align-items:center;width:100%">
      ${shot('missione_nuova_card.png', 'Missione rifiutata da 3 autisti: decide la centrale', '--mh:440px')}
      <div>${shot('dashboard_operativa.png', 'Dashboard operativa', '--mh:210px')}<div style="height:10px"></div>${shot('anagrafica_clienti.png', 'Anagrafiche', '--mh:210px')}</div>
    </div>`,
    notes: {
      voce: "La centrale vede subito la coda critica: polizze da verificare, missioni rifiutate da tre autisti, richieste di supporto. I riquadri aprono l'elenco filtrato. Dalla scheda può proporre, annullare o rettificare il costo, e una campanella segnala rifiuti, scadenze e supporto.",
      semplice: "Tutto ciò che è a schermo arriva dal server tramite API.",
      file: ['`static/index.html`', '`static/app.js`', '`controller/MissionController.java`'],
    },
  },
  {
    section: FE, owner: MEMBRI.m2, title: 'Area autista',
    html: `<div style="display:grid;grid-template-columns:auto auto 1fr;gap:28px;align-items:center;width:100%">
      ${shot('autista_mobile_proposta.png', 'Proposta', '--mh:474px')}
      ${shot('autista_mobile_attiva.png', 'Dopo aver accettato', '--mh:474px')}
      ${pts(['Pensata per il telefono', 'Accetta o rifiuta', 'Mappa e chilometri'])}</div>`,
    notes: {
      voce: "L'area autista è pensata per il telefono. Le nuove proposte compaiono da sole, con tempo residuo, mappa e guadagno stimato. Nome e telefono del cliente si vedono già dalla proposta, la targa dopo l'accettazione. A fine intervento l'autista conferma i chilometri e chiude la missione, poi trova il totale nello storico.",
      semplice: "L'autista vede solo le proprie missioni.",
      file: ['`static/app.js`', '`service/MissionService.java`'],
    },
  },
  {
    section: BOT, owner: MEMBRI.m3, title: 'Il dialogo con il cliente',
    html: two(`<div style="text-align:center">${shot('chatbot_dialogo_simulato.png', 'Testi reali del bot, grafica Telegram simulata', '--mh:470px')}</div>`,
      pts(['Targa e telefono sul posto', 'Posizione e problema', 'Nasce la missione']), '.9fr 1.1fr'),
    notes: {
      voce: "Il bot si chiama Soccorso Stradale Bot. Il cliente scrive la targa e il telefono di chi è sul posto. Poi il bot chiede dove si trova e qual è il problema; il tipo di intervento lo decide il backend. Se la polizza non è verificabile interviene la centrale. Alla fine mostra il codice. Scrivendo STATO il cliente può seguire la richiesta.",
      semplice: "Il bot raccoglie i dati nell'ordine giusto e li passa al backend.",
      file: ['`chatbot/fornitori_chatbot/engine.py`', '`chatbot/fornitori_chatbot/messages.py`', '`service/ChatService.java`'],
    },
  },
  {
    section: MAP, owner: MEMBRI.m4, title: 'Mappe: geocoding e percorso',
    html: two(shot('mappa_area.png', 'Deposito e officina a Ruvo, luogo di fermo a Terlizzi', '--mh:450px'),
      pts(['<b>Nominatim</b>: da indirizzo a coordinate', '<b>OSRM</b>: percorso e km', '<b>Leaflet</b>: disegna la mappa'], 'small'), '1.3fr .7fr'),
    notes: {
      voce: "Le mappe usano tre strumenti. Nominatim trasforma l'indirizzo in coordinate. OSRM calcola il percorso su strada e i chilometri stimati. Leaflet disegna tutto nella pagina. In questo traino l'autista parte da Ruvo, raggiunge il veicolo a Terlizzi e lo riporta all'officina di Ruvo.",
      semplice: "I km stimati vengono proposti alla chiusura; il costo usa quelli effettivi.",
      file: ['`service/MapService.java`', '`static/app.js`'],
    },
  },
  {
    section: NOT, owner: MEMBRI.m5, title: 'Notifiche: outbox e dispatcher',
    html: `<div class="center" style="height:auto">${svg(1170, 200, [
      box(10, 40, 250, 110, 'Autista accetta', [], 'indigo', { ts: 23 }),
      box(310, 40, 250, 110, 'Outbox', ['salvata con la missione'], 'ink', { ts: 23, ss: 17 }),
      box(610, 40, 250, 110, 'Dispatcher', ['controlla ogni 5 secondi'], 'teal', { ts: 23, ss: 17 }),
      box(910, 40, 250, 110, 'Cliente', ['riceve il messaggio'], 'yellow', { ts: 23, ss: 17 }),
      arrow(263, 95, 306, 95), arrow(563, 95, 606, 95), arrow(863, 95, 906, 95),
    ].join(''))}</div>
    <div style="margin-top:22px">${pts(['Tre avvisi: presa in carico, conclusione, annullamento', 'Due dispatcher non ricevono lo stesso messaggio'])}</div>`,
    notes: {
      voce: "Il cliente riceve un avviso quando un autista prende in carico la richiesta, quando l'intervento è concluso e se viene annullata, mai con il nome dell'autista. Il messaggio viene prima salvato nell'outbox, insieme alla missione. Poi il dispatcher del bot, ogni cinque secondi, prende i messaggi in coda, li invia e registra l'esito.",
      semplice: "Prima si scrive «da inviare», poi il bot spedisce e segna com'è andata.",
      file: ['`entity/NotificationOutbox.java`', '`chatbot/fornitori_chatbot/notifications.py`'],
    },
  },
  {
    section: CH, owner: MEMBRI.team, title: 'Scaletta della demo live',
    html: `<table class="t" style="font-size:23px"><tr><th style="width:70px">#</th><th style="width:300px">Dove</th><th>Cosa mostriamo</th></tr>
      <tr><td>1</td><td>Telegram</td><td>Nasce la richiesta</td></tr>
      <tr><td>2</td><td>Centrale</td><td>Coda critica e proposta automatica</td></tr>
      <tr><td>3</td><td>Autista</td><td>Proposta, mappa, accetta</td></tr>
      <tr><td>4</td><td>Telegram</td><td>Avviso di presa in carico</td></tr>
      <tr><td>5</td><td>Autista</td><td>Chiusura e costo</td></tr>
      <tr><td>6</td><td>Centrale</td><td>Riepilogo aggiornato</td></tr></table>`,
    notes: {
      voce: "Adesso vediamo il sistema. Su Telegram apriamo una richiesta con una targa dimostrativa. Il sistema la propone all'autista più vicino e la centrale la segue. L'autista, dal telefono, vede la mappa e accetta. Su Telegram arriva l'avviso. Poi chiudiamo con i chilometri e il server calcola il costo.",
      semplice: "Tieni aperte tre finestre: centrale, autista e Telegram.",
      file: ['`docs/manuals/MANUALE_UTENTE.md`', '`chatbot/README.md`'],
    },
  },
  {
    section: CH, owner: MEMBRI.team, title: 'Sviluppi futuri',
    html: pts([
      'Identificazione più forte del cliente',
      'Canale WhatsApp',
      'Gestione utenti dall\'interfaccia',
      'Messa in produzione',
    ]),
    notes: {
      voce: "I prossimi passi sono quattro. Rafforzare l'identificazione del cliente con un secondo dato. Aggiungere WhatsApp, già previsto nel contratto delle API. Gestire gli utenti dall'interfaccia. Infine portare il sistema in produzione, con HTTPS e una configurazione adatta a dati reali.",
      semplice: "Sono tutti sviluppi non ancora implementati, già indicati nei documenti.",
      file: ['`docs/agile/PRODUCT_BACKLOG.md`', '`api/openapi.yaml`'],
    },
  },
  {
    section: CH, owner: MEMBRI.team, title: 'Conclusioni',
    html: two(pts([
      'Dal primo messaggio alla chiusura',
      'Regole e costi solo sul server',
      'Componenti separati, un unico contratto',
    ]), `<div class="center" style="flex-direction:column;background:${C.blue};border-radius:18px;color:#fff;height:320px">
      <div style="font-size:58px;font-weight:800">Grazie</div>
      <div style="font-size:21px;font-weight:500;margin-top:18px">RecuVeicol · ITS Developer 4.0</div>
    </div>`, '1.1fr .9fr'),
    notes: {
      voce: "Abbiamo costruito un sistema che porta una richiesta di soccorso dal primo messaggio fino alla chiusura con il costo calcolato. Regole e costi stanno sul server, e i componenti sono separati e collegati da un unico contratto. Vi ringraziamo per l'attenzione.",
      semplice: "Il messaggio finale in tre punti.",
      file: ['`README.md`', '`docs/00_INDICE_GENERALE.md`'],
    },
  },
];

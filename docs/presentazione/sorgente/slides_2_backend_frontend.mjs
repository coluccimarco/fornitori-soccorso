import { C, MEMBRI, pts, shot, card, two, three, motivi, svg, box, arrow, txt, zone } from './lib.mjs';

const BE = 'Backend e database', FE = 'Frontend';

export const whySlide = (section, owner, a, b, c, notes) => ({
  section, owner, title: "Perché l'abbiamo fatto così",
  html: `<div class="three">
    ${card('Organizzazione dei file', pts(a), C.blue)}
    ${card('Pattern', pts(b), C.indigo)}
    ${card('Formato dei dati', pts(c), C.yellow)}
  </div>`,
  notes,
});

export default [
  {
    type: 'divider', section: BE, owner: MEMBRI.m1, member: 'Membro 1', title: 'Backend', who: 'Vincenzo Minervini',
    topics: 'API · sicurezza · logica delle missioni · tariffa notturna · scadenze · assegnazione al più vicino · database',
    notes: {
      voce: "Ora entriamo nelle singole parti, partendo dal cuore del sistema. Sono Vincenzo Minervini e sono responsabile del backend e del database. In questa sezione vedremo prima le API, cioè che cosa il backend offre agli altri componenti. Poi la sicurezza: come funzionano login, token CSRF e ruoli. Poi la logica delle missioni: gli stati, la tariffa notturna, le scadenze e l'assegnazione al più vicino. Chiuderemo con lo schema del database, semplificato, e con una slide che spiega perché il codice è organizzato così. Il messaggio di fondo è uno: il server non si fida mai del client. Costi, permessi e scadenze sono calcolati e controllati solo qui. Anche se qualcuno modificasse la pagina nel browser, non potrebbe accettare una missione di un altro autista né cambiare il proprio guadagno. Tutto questo è coperto da 139 test automatici su un database isolato.",
      semplice: "Il backend è il programma Java che custodisce regole e dati. Tutti gli altri componenti gli chiedono le cose tramite API.",
      file: ['`src/main/java/it/exprivia/fornitori/`', '`database/`', '`api/openapi.yaml`', '`docs/team/MEMBRO_1_BACKEND.md`', '`docs/security/SECURITY_TEST_REPORT.md`'],
      domande: [
        ["Avete dei test?", "Sì: 139 test Java su un database isolato e 6 test del chatbot, tutti superati. Girano solo su database `fornitori_e2e*`, perché cancellano i dati live."],
        ["Quanto è grande il backend?", "6 controller, 14 repository, 13 entità e i service: `MissionService` è il più grande, con 715 righe."],
      ],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'API: chi può chiamare cosa',
    html: `<table class="t" style="font-size:20.5px"><tr><th style="width:280px">Area</th><th style="width:90px">Op.</th><th>A cosa serve</th><th style="width:300px">Chi accede</th></tr>
      <tr><td>Autenticazione</td><td>4</td><td>Login, profilo, logout, CSRF</td><td>Pubblico, poi sessione</td></tr>
      <tr><td>Anagrafiche</td><td>6</td><td>Clienti, autisti, mezzi, compagnie, officine</td><td>Centrale</td></tr>
      <tr><td>Missioni e dashboard</td><td>16</td><td>Ciclo missione, note, costi, storico</td><td>Centrale o autista assegnato</td></tr>
      <tr><td>Assegnazione</td><td>2</td><td>Proposta al più vicino, avvisi</td><td>Centrale</td></tr>
      <tr><td>Chat e notifiche</td><td>5</td><td>Targa, missione, stato, invio</td><td>Bot, con token di servizio</td></tr>
      <tr><td>Mappe</td><td>1</td><td>Percorso e km stimati</td><td>Centrale o autista assegnato</td></tr></table>
      <p class="big" style="text-align:center;margin:26px 0 0;font-size:24px">Errori sempre uguali: <span style="font-family:JetBrains,monospace;font-size:22px;font-weight:400">{ status, error, message }</span></p>`,
    notes: {
      voce: "Il backend espone 34 operazioni REST, raggruppate in sei aree. L'autenticazione ne ha quattro: il token CSRF, il login, il profilo e il logout. Le anagrafiche ne hanno sei e sono riservate alla centrale. La parte più ampia è quella delle missioni: creazione, proposta, accettazione, rifiuto, chiusura, rettifica del costo, note, dashboard e storico dell'autista. Le API di assegnazione offrono la proposta automatica al più vicino e gli avvisi alla centrale. Le API chat sono usate solo dal bot, con un token di servizio. Infine c'è l'unica API delle mappe. Tutte rispondono in JSON, e gli errori hanno sempre la stessa forma: lo stato HTTP, un codice stabile e, quando serve, un messaggio per l'utente. Così il frontend può mostrare per esempio «Targa già presente» senza conoscere i dettagli interni. In caso di errore inatteso rispondiamo con un 500 generico e un riferimento da cercare nel log, senza mai mostrare eccezioni o SQL.",
      semplice: "Ogni riga è un gruppo di «sportelli». La colonna a destra dice chi può andarci. Il formato d'errore uguale per tutti semplifica il lavoro di chi usa l'API.",
      file: ['`api/openapi.yaml`', '`controller/MissionController.java`', '`controller/RegistryController.java`', '`controller/ChatController.java`', '`controller/AssignmentController.java`', '`common/ApiExceptionHandler.java`', '`common/ApiError.java`'],
      scelta: {
        alt: "Pagine generate dal server (Thymeleaf) o GraphQL.",
        perche: "Un'unica API REST serve sia il browser sia il bot, in JSON. Thymeleaf non sarebbe servito al bot. Nel repo non c'è un confronto formale: il motivo discende dall'avere due consumatori diversi.",
        limiti: "Liste non paginate (V-23). La GET delle mappe può scrivere sul database (V-24): è protetta da CSRF, ma cambiarla romperebbe il contratto.",
      },
      domande: [
        ["Perché i verbi in inglese, come /accept, in un'API italiana?", "È una disomogeneità del contratto: `missioni` è italiano, `accept`, `reject` e `complete` sono inglesi. Non cambia il funzionamento, ma non ha una motivazione documentata."],
        ["Come fa il bot a non passare dal login?", "Ha una catena di sicurezza separata su `/api/v1/chat/**`, stateless, con un token bearer confrontato a tempo costante."],
        ["Cosa succede se un'API riceve JSON enorme?", "`RequestBodyLimitFilter` rifiuta i corpi oltre 64 KiB con 413, prima di arrivare al controller."],
      ],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'Sicurezza: login, CSRF e ruoli',
    html: two(`<div>${svg(560, 420, [
      txt(95, 22, 'Browser', { w: 800, s: 19, c: C.ink }), txt(465, 22, 'Backend', { w: 800, s: 19, c: C.ink }),
      `<line x1="95" y1="32" x2="95" y2="415" stroke="#c6cedb" stroke-width="2" stroke-dasharray="4 6"/><line x1="465" y1="32" x2="465" y2="415" stroke="#c6cedb" stroke-width="2" stroke-dasharray="4 6"/>`,
      arrow(95, 75, 463, 75, '1 Chiedo il token CSRF'),
      arrow(465, 125, 97, 125, 'token', { color: C.blue, lc: C.blue }),
      arrow(95, 180, 463, 180, '2 Email, password + token'),
      arrow(465, 235, 97, 235, ['cookie di sessione HttpOnly,', 'nuovo token'], { color: C.blue, lc: C.blue, dy: -26 }),
      arrow(95, 300, 463, 300, '3 Azione + X-CSRF-TOKEN'),
      arrow(465, 365, 97, 365, ['il server controlla ruolo', 'e proprietà della missione'], { color: C.blue, lc: C.blue, dy: -26 }),
    ].join(''))}</div>`, pts([
      '<b>Password</b>: solo hash BCrypt',
      '<b>CSRF</b>: una pagina esterna non può agire al posto tuo',
      '<b>Ruoli</b>: RESPONSABILE e AUTISTA, verificati dal server',
      '<b>Limiti</b>: 20 login al minuto per IP, 10 falliti per account',
      '<b>Bot</b>: token di servizio separato',
    ], 'small'), '1.05fr .95fr'),
    notes: {
      voce: "Vediamo la sicurezza con il login. Il browser chiede prima un token CSRF, poi invia email, password e token. Se le credenziali sono corrette, riceve un cookie di sessione HttpOnly, che JavaScript non può leggere, e un nuovo token, perché al login cambiano sia l'identificativo di sessione sia il CSRF. Ogni azione successiva allega il token in un'intestazione. Il CSRF serve a questo: senza token, una pagina esterna aperta nello stesso browser non può far partire un'azione a tuo nome, anche se il cookie viene inviato. Le password sono salvate solo come hash BCrypt. I ruoli sono controllati su due livelli: Spring Security filtra per percorso, e il service verifica che la missione sia davvero di quell'autista. Ci sono limiti ai tentativi: 20 login al minuto per indirizzo IP e 10 falliti per account. Il bot non usa la sessione: ha una catena di sicurezza separata, con un token di servizio. Tutto questo è stato verificato con test specifici.",
      semplice: "La sessione dice «chi sei», il token CSRF prova «sei stato davvero tu a premere», il ruolo dice «cosa puoi fare». Sono tre controlli diversi.",
      file: ['`config/SecurityConfig.java`', '`controller/AuthController.java`', '`config/ChatServiceTokenFilter.java`', '`service/RequestRateLimiter.java`', '`docs/architecture/SECURITY_DESIGN.md`', '`docs/security/VULNERABILITY_REGISTER.md`'],
      scelta: {
        alt: "JWT nel browser invece della sessione.",
        perche: "H-004 documenta la scelta: sessione server-side, login JSON, cookie HttpOnly, BCrypt e CSRF. Un JWT salvato nel browser sarebbe leggibile da JavaScript e difficile da revocare; un logout con la sessione la invalida subito.",
        limiti: "Il cookie `Secure` è attivo solo nel profilo `prod` e HTTPS non è stato provato su un server reale (V-15). I limiti sono in memoria, per singola istanza (V-22).",
      },
      domande: [
        ["Perché non avete usato JWT?", "Il client è sulla stessa origine, quindi la sessione con cookie è più semplice e si revoca subito. Il token del bot è opaco, non un JWT."],
        ["Perché serve il CSRF se c'è il cookie HttpOnly?", "HttpOnly protegge dalla lettura del cookie via script. Il CSRF protegge da richieste inviate da altri siti, in cui il cookie partirebbe da solo."],
        ["Perché la GET delle mappe richiede il CSRF?", "Può salvare coordinate e km stimati: non è una lettura pura, quindi la proteggiamo come una modifica."],
      ],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'Logica delle missioni',
    html: `<div>${svg(1170, 240, [
      box(10, 80, 150, 72, 'nuova', [], 'soft'),
      box(255, 80, 170, 72, 'proposta', [], 'indigo'),
      box(530, 80, 170, 72, 'accettata', [], 'blue'),
      box(805, 80, 170, 72, 'eseguita', [], 'teal'),
      box(255, 175, 170, 60, 'scaduta', [], 'yellow', { ts: 19 }),
      box(500, 175, 200, 60, 'non accettata', [], 'yellow', { ts: 19 }),
      arrow(162, 116, 251, 116, 'proponi'),
      arrow(427, 116, 526, 116, 'accetta'),
      arrow(702, 116, 801, 116, 'chiude'),
      arrow(315, 154, 315, 171, '', { color: C.muted }),
      arrow(420, 154, 500, 180, '', { color: C.muted }),
      `<path d="M255 205 C190 205 215 130 252 128" fill="none" stroke="${C.muted}" stroke-width="2" stroke-dasharray="6 5"/>`,
      txt(140, 200, 'riproponi', { s: 15.5, w: 600 }),
      txt(90, 66, 'Solo le missioni live', { s: 15, c: C.muted }),
      txt(890, 170, 'costo finale', { s: 15.5, w: 600 }),
      txt(890, 190, 'e notifica', { s: 15.5, w: 600 }),
    ].join(''))}</div>
    <div class="three" style="margin-top:12px">
      ${card('Scadenze', pts(['Proposta valida 10 minuti', 'Scheduler ogni 30 secondi', 'Controllo anche alla risposta']), C.yellow)}
      ${card('Tariffa', pts(['€50 + €1,20 al km', '+25% dalle 22:00 alle 08:00', 'Conta l\'ora di accettazione']), C.blue)}
      ${card('Al più vicino', pts(['Autisti liberi, in linea d\'aria', 'Esclude chi ha già risposto', 'Se è occupato, passa al prossimo']), C.indigo)}
    </div>
    <p class="note-s" style="margin:9px 0 0;text-align:center">Fascia 22:00–08:00: scelta del team. Il PDF del docente indica 20:00–8:00, da confermare col docente.</p>`,
    notes: {
      voce: "La missione è una macchina a stati. Nasce nuova, la centrale la propone, poi può essere accettata, rifiutata o può scadere; nei due ultimi casi si può riproporre, e alla fine viene eseguita. Ogni passaggio prende un lock sulla riga, così due azioni contemporanee non si sovrappongono. Le scadenze: una proposta vale dieci minuti, valore nella tabella `pricing_config`. Uno scheduler controlla ogni trenta secondi, e il controllo viene ripetuto quando l'autista risponde. La tariffa: cinquanta euro di uscita più un euro e venti al chilometro, con il venticinque per cento in più nella fascia notturna. Vale l'ora di accettazione, nel fuso di Roma. Attenzione: abbiamo scelto la fascia dalle 22 alle 8, mentre il PDF indica dalle 20 alle 8, e lo vogliamo confermare con lei. Il più vicino: si calcola la distanza in linea d'aria tra il luogo del sinistro e la base dei mezzi, tra gli autisti liberi che non hanno già risposto.",
      semplice: "Una missione può trovarsi in un solo stato alla volta e passa solo per le frecce disegnate. Esempio reale della demo: 15,23 km di giorno = 50 + 1,20 × 15,23 = €68,28; la stessa missione di notte = €85,35.",
      file: ['`service/MissionService.java`', '`service/PricingService.java`', '`service/MissionExpiryScheduler.java`', '`service/NearestDriverService.java`', '`service/GeoDistance.java`', '`database/10_night_rate_22.sql`', '`docs/diagrams/mission_states.mmd`', '`docs/requirements/BUSINESS_RULES.md`'],
      scelta: {
        alt: "Calcolo del costo nel client; `double` invece di `BigDecimal`; più vicino per strada con OSRM.",
        perche: "Costo solo sul server, perché è un dato da non poter manipolare. `BigDecimal` con HALF_UP evita errori di arrotondamento (H-003). La linea d'aria non richiede chiamate esterne per ogni autista.",
        limiti: "La linea d'aria può scegliere un autista non ottimale per strada. Le 22:00 e il riferimento all'ora di accettazione vanno confermati (D-HARD-03). Nello storico 6 missioni su 30 sono notturne: 3 di queste lo sono solo con la vecchia fascia e non vengono ricalcolate. La rettifica del costo resta possibile, con motivo obbligatorio.",
      },
      domande: [
        ["Perché le 22:00 se il PDF dice 20:00?", "È una scelta del team, documentata in D-HARD-03 e da confermare con il docente. Il valore è in `pricing_config`: per cambiarlo basta aggiornare una riga."],
        ["Perché non avete usato la distanza stradale per il più vicino?", "Servirebbe una chiamata a OSRM per ogni autista, verso un servizio pubblico con limiti. Per scegliere bastano le coordinate; i km da pagare invece sono quelli effettivi."],
        ["Che succede se due centrali propongono lo stesso autista?", "Lock pessimistico su missione e autista, e indice unico parziale. Il secondo riceve 409; «al più vicino» passa al successivo."],
      ],
    },
  },
  {
    section: BE, owner: MEMBRI.m1, title: 'Database: schema semplificato',
    html: `<div class="center">${svg(1170, 500, [
      box(420, 175, 300, 124, 'missions', ['stato, intervento, luogo, km, costi', 'is_fixture: storico o live'], 'blue', { ss: 15.5 }),
      box(20, 30, 220, 72, 'customers', ['targa_norm univoca'], 'light', { ts: 19, ss: 16 }),
      box(20, 192, 220, 72, 'insurance_companies', ['compagnie'], 'light', { ts: 17, ss: 16 }),
      box(290, 30, 200, 72, 'drivers', ['1 utente, 1 mezzo'], 'light', { ts: 19, ss: 16 }),
      box(540, 30, 180, 72, 'vehicles', ['base e coordinate'], 'light', { ts: 19, ss: 16 }),
      box(770, 30, 180, 72, 'app_users', ['hash BCrypt, ruolo'], 'light', { ts: 19, ss: 16 }),
      box(830, 175, 320, 72, 'workshops', ['officine con coordinate'], 'light', { ts: 19, ss: 16 }),
      box(20, 370, 250, 86, 'mission_proposals', ['ogni tentativo e il suo esito'], 'soft', { ts: 18, ss: 16 }),
      box(300, 370, 200, 86, 'mission_notes', ['segnalazioni'], 'soft', { ts: 18, ss: 16 }),
      box(530, 370, 200, 86, 'mission_events', ['audit in JSONB'], 'soft', { ts: 18, ss: 16 }),
      box(760, 370, 390, 86, 'notifications_outbox', ['event_key unica, stato di invio'], 'ink', { ts: 18, ss: 16 }),
      arrow(420, 195, 240, 90), arrow(130, 106, 130, 188),
      arrow(500, 171, 420, 106), arrow(494, 66, 536, 66), arrow(726, 66, 766, 66, '', { both: false }),
      arrow(724, 225, 826, 215),
      arrow(450, 303, 170, 366), arrow(530, 303, 420, 366), arrow(590, 303, 625, 366), arrow(690, 303, 890, 366),
      txt(585, 490, '14 tabelle in totale: qui mancano pricing_config, chat_mission_requests, chat_lookup_grants', { s: 16.5 }),
    ].join(''))}</div>`,
    notes: {
      voce: "Questo è lo schema semplificato. Al centro c'è la tabella `missions`, che collega un cliente, eventualmente un autista e un'officina, e contiene stato, luogo, chilometri e costi. Il cliente ha una targa normalizzata univoca e una compagnia assicurativa. L'autista è legato a un solo utente e a un solo mezzo; il mezzo contiene le coordinate della base, che servono per scegliere il più vicino. Sotto vedete quattro tabelle figlie. `mission_proposals` conserva ogni tentativo, così sappiamo chi ha rifiutato e quando. `mission_notes` contiene le segnalazioni dell'autista. `mission_events` è l'audit, con i dettagli in JSON. `notifications_outbox` è la coda delle notifiche. In tutto le tabelle sono 14: mancano dal disegno la configurazione tariffaria e le due tabelle del chatbot. Una colonna da ricordare è `is_fixture`: separa le 30 missioni storiche fornite dal docente da quelle operative, così lo storico non viene mai alterato.",
      semplice: "Una missione è il «foglio» centrale; le altre tabelle sono le schede collegate. Non si cancella la storia: proposte ed eventi si aggiungono.",
      file: ['`database/01_schema.sql`', '`database/05_chat_requests.sql`', '`database/07_backend_hardening.sql`', '`docs/architecture/DATABASE_DESIGN.md`', '`docs/diagrams/database_er.mmd`', '`entity/Mission.java`'],
      scelta: {
        alt: "Storico in un'unica colonna JSON della missione; tabella separata per le missioni storiche.",
        perche: "`mission_proposals` e `mission_events` separate restano interrogabili e rendono esplicito l'audit. Il flag `is_fixture` è documentato in H-001: non alterare il riepilogo storico. Importi in NUMERIC e istanti in TIMESTAMPTZ.",
        limiti: "Importi del seed: 16 eseguite, totale €1.509,71. Alcune incoerenze del foglio Excel restano nelle righe `is_fixture` (R-08), isolate dalle regole live. Excel originale non presente nel repository.",
      },
      domande: [
        ["Perché NUMERIC e non FLOAT per il costo?", "FLOAT accumula errori di arrotondamento. NUMERIC(10,2) è esatto, e in Java corrisponde a BigDecimal."],
        ["Perché ci sono targa e targa_norm?", "`targa` conserva come è scritta, `targa_norm` è solo maiuscole e cifre e serve per la ricerca: «pw 660 sw» e «PW660SW» sono la stessa targa."],
        ["Perché uno stesso autista non può avere due missioni?", "Lo vieta `uq_driver_active_live`, indice unico sulle sole missioni live in stato proposta o accettata."],
      ],
    },
  },
  whySlide(BE, MEMBRI.m1,
    ['<b>controller</b>, <b>service</b>, <b>repository</b>', '<b>dto</b> separati dalle <b>entity</b>', 'Script SQL numerati'],
    ['Livelli con compiti distinti', 'Lock pessimistico sulle missioni', 'Clock iniettato, testabile nel tempo'],
    ['JSON in API, errori uniformi', 'NUMERIC e BigDecimal per il denaro', 'TIMESTAMPTZ, JSONB per l\'audit'],
    {
      voce: "Chiudo la parte backend con il perché. L'organizzazione dei file segue i livelli: chi cerca una regola sa di dover aprire `service`; chi cerca una query apre `repository`. I DTO stanno in un pacchetto a sé, così il formato pubblico non è legato alle tabelle. Gli script SQL sono numerati per ricostruire il database da zero, in ordine. Sul fronte dei pattern, i livelli separano le responsabilità. Il lock pessimistico evita che due azioni si sovrappongano sulla stessa missione, cosa fondamentale in un sistema dove due persone possono agire nello stesso secondo. Il tempo non viene letto direttamente: viene iniettato un Clock, e questo permette ai test di verificare una scadenza senza aspettare dieci minuti. Sui formati: JSON nelle API con errori uniformi, NUMERIC e BigDecimal per i soldi, TIMESTAMPTZ per gli istanti e JSONB per i dettagli dell'audit, che cambiano da evento a evento. Nulla di esotico: scelte standard e verificabili.",
      semplice: "Tutto è al suo posto, per chi legge il codice. I soldi sono calcolati con precisione esatta; il tempo si può simulare nei test.",
      file: ['`src/main/java/it/exprivia/fornitori/` (cartelle)', '`config/TimeConfiguration.java`', '`repository/MissionRepository.java` (`lockLiveById`)', '`entity/MissionEvent.java`', '`src/test/java/.../MissionLifecycleTests.java`'],
      scelta: {
        alt: "Lock ottimistico con versione; un'architettura esagonale; date in stringa.",
        perche: "D-HARD-01 conferma i livelli e correzioni mirate. Con il lock ottimistico il secondo utente otterrebbe comunque un errore dopo il tentativo; il pessimistico è più semplice da ragionare per transizioni brevi. Quest'ultimo confronto non è documentato: è una lettura del codice.",
        limiti: "`MissionService` concentra molte responsabilità ed è debito tecnico noto. Le mappe non sono transazionali di proposito, per non tenere aperta una transazione durante una chiamata esterna.",
      },
      domande: [
        ["Perché MapService non è @Transactional?", "Perché chiama servizi esterni che rispondono in secondi: una transazione aperta bloccherebbe risorse. I risultati si salvano in transazioni brevi."],
        ["Cosa cambieresti del backend?", "Dividerei `MissionService` per casi d'uso e aggiungerei la paginazione alle liste: sono i debiti già registrati in TAD e V-23."],
      ],
    }),
  {
    type: 'divider', section: FE, owner: MEMBRI.m2, member: 'Membro 2', title: 'Frontend', who: 'Francesco Valeriano · Giuseppe Cangellosi · Marika Sancilio',
    topics: 'Pagina pubblica · login · centrale operativa · ciclo della missione · area autista',
    notes: {
      voce: "Passiamo ora a ciò che gli utenti vedono. Noi siamo Francesco Valeriano, Giuseppe Cangellosi e Marika Sancilio, responsabili del frontend. L'applicazione web è una sola e offre tre esperienze: la pagina pubblica, che spiega il servizio e porta al bot; la centrale operativa, per chi coordina; l'area autista, pensata per lo smartphone. Tutto sta in una cartella di file statici, con un solo `index.html` e un solo `app.js`: la vista cambia in base a ciò che l'utente sceglie e al ruolo restituito dal server. Le prossime slide mostrano schermate reali catturate dal sistema in funzione, su dati dimostrativi. Vedrete prima la panoramica e le anagrafiche, poi il ciclo della missione con la proposta e gli avvisi, e infine l'area autista dal telefono. Una regola ha guidato tutto il lavoro: il frontend non calcola e non decide nulla, mostra e chiede soltanto. Ogni dato arriva dalle API e nessuna mappa o informazione è simulata.",
      semplice: "Il frontend è ciò che si vede nel browser. Le decisioni le prende sempre il backend; qui si disegnano i risultati e si raccolgono i comandi dell'utente.",
      file: ['`src/main/resources/static/`', '`docs/team/MEMBRO_2_FRONTEND.md`', '`docs/manuals/MANUALE_UTENTE.md`'],
      domande: [
        ["È una single page application?", "Di fatto sì: una sola pagina HTML con viste mostrate e nascoste da JavaScript, ma senza framework."],
        ["L'interfaccia è accessibile?", "L'HTML usa etichette e attributi ARIA, ma l'accessibilità non è stata verificata in modo formale: il rapporto dei test la segna come non verificata."],
      ],
    },
  },
  {
    section: FE, owner: MEMBRI.m2, title: 'Centrale: accesso e panoramica',
    html: `<div style="display:grid;grid-template-columns:1.32fr 1fr;gap:26px;align-items:center;width:100%">
      ${shot('panoramica.png', 'Panoramica: conteggi reali dalle anagrafiche', '--mh:400px')}
      <div>${shot('login.png', 'Login', '--mh:176px')}<div style="height:10px"></div>${shot('anagrafica_clienti.png', 'Anagrafiche con ricerca', '--mh:206px')}</div>
    </div>
    <p class="note-s" style="margin:12px 0 0;text-align:center;font-size:21px">Dati letti dalle API, mai simulati. Errori del server in chiaro, come «Targa già presente».</p>`,
    notes: {
      voce: "Partiamo dall'accesso. Il login usa email e password; dopo l'autenticazione il server restituisce il ruolo e l'interfaccia mostra solo le sezioni di quel ruolo. Nella schermata del login vedete il logo «fornitori.», il nome tecnico, che l'area riservata conserva ancora. La panoramica della centrale riassume i conteggi: 40 clienti, 8 autisti, 8 mezzi e 6 officine. Sono numeri reali, letti dalle anagrafiche attraverso le API. Da qui si entra nelle singole anagrafiche: clienti, autisti, mezzi, compagnie e officine. In basso a destra c'è la lista dei clienti, con la ricerca per nome o targa e l'inserimento di un nuovo cliente tramite una finestra guidata. Se il server rifiuta il dato, per esempio perché una targa esiste già, la pagina mostra il suo messaggio, senza inventare un testo. Ogni lista ha un indicatore di caricamento e uno stato vuoto, così l'utente capisce sempre che cosa sta succedendo. Il layout è responsive: si adatta da solo allo schermo.",
      semplice: "Tutto ciò che è a schermo arriva dal server. Se il server dice no, l'utente legge il motivo del server.",
      file: ['`static/index.html` (`loginView`, `dashboardPage`, `registryPage`)', '`static/app.js` (`loadRegistries`, `request`)', '`service/RegistryService.java`', '`static/styles.css`'],
      scelta: {
        alt: "Tabelle con un componente dedicato (DataTables) o un framework CSS diverso.",
        perche: "Bootstrap copre griglia, modali e tabelle senza altre dipendenze. Il repo non motiva Bootstrap rispetto ad altri framework CSS: è documentato solo come presente (TAD).",
        limiti: "Liste non paginate. Il marchio resta «fornitori.» nell'area riservata e RecuVeicol nel sito pubblico: da uniformare.",
      },
      domande: [
        ["Perché vedo «fornitori» nel login?", "Il marchio RecuVeicol è sul sito pubblico; l'area riservata mantiene il nome tecnico del progetto. Andrà allineato."],
        ["Come si cerca un cliente?", "La ricerca chiama `GET /api/v1/clienti?search=` e tratta il testo come tale: `%` e `_` non restituiscono tutti i clienti (V-16, corretta)."],
      ],
    },
  },
  {
    section: FE, owner: MEMBRI.m2, title: 'Centrale: il ciclo della missione',
    html: two(shot('missione_nuova_card.png', '1 · Missione nuova dal bot: si propone al più vicino', '--mh:420px'),
      `<div>${shot('missione_proposta_card.png', '2 · Proposta: conto alla rovescia di 10 minuti', '--mh:196px')}<div style="height:10px"></div>${shot('centrale_avvisi.png', '3 · Rifiuto: la campanella avvisa la centrale', '--mh:210px')}</div>`, '1fr .86fr')
      + `<p class="note-s" style="margin:12px 0 0;text-align:center;font-size:21px">Riproposta con storico. Intervento e officina correggibili. Costo rettificabile con motivo.</p>`,
    notes: {
      voce: "Ecco il lavoro quotidiano della centrale. La scheda a sinistra è una missione appena arrivata dal bot: mostra cliente, luogo, tipo di intervento e officina assegnata. Qui la centrale può scegliere un autista dall'elenco oppure premere «Proponi al più vicino», lasciando decidere il sistema. Può anche correggere tipo di intervento e officina, se quanto indicato dal cliente non è adatto. Appena proposta, in alto a destra, compare lo stato con il conto alla rovescia dei dieci minuti. Se l'autista rifiuta o la proposta scade, la campanella segnala l'avviso: il controllo avviene ogni quindici secondi. Dall'avviso si torna alla scheda e si ripropone: «Al più vicino» esclude chi ha già risposto, e ogni tentativo resta nello storico delle proposte. Quando la missione è eseguita, compare il campo per correggere il costo. La correzione richiede un motivo, che resta registrato, e il costo calcolato non viene perso.",
      semplice: "La centrale non deve ricordare nulla: la scheda mostra lo stato e le sole azioni possibili in quello stato. Il sistema rifiuta comunque quelle non ammesse.",
      file: ['`static/app.js` (`renderMissionList`, `loadAlerts`, `updateCountdowns`)', '`controller/MissionController.java`', '`controller/AssignmentController.java`', '`service/CentreAlertService.java`'],
      scelta: {
        alt: "WebSocket o Server-Sent Events per aggiornamenti in tempo reale.",
        perche: "Il polling ogni 15 secondi è documentato e basta per un processo in cui si risponde in minuti. Il repo non documenta il confronto con i WebSocket: il motivo è ricostruito (meno infrastruttura lato client e server).",
        limiti: "Avvisi fino a 15 secondi in ritardo. Lo stato «letto» degli avvisi è salvato solo nel browser di chi guarda, quindi non è condiviso tra operatori.",
      },
      domande: [
        ["Perché non WebSocket?", "Bastano aggiornamenti ogni 15 secondi e il polling non richiede altro server né gestione delle connessioni."],
        ["Può la centrale creare una missione da sola?", "Sì, con «Nuova missione»: seleziona cliente, intervento, officina se è un traino, canale, luogo e problema. Quelle missioni però non generano notifiche, perché la chat del cliente non è nota."],
        ["Il canale «WhatsApp» nel modulo invia messaggi?", "No: è solo un metadato di origine. WhatsApp non è integrato."],
      ],
    },
  },
  {
    section: FE, owner: MEMBRI.m2, title: "Area autista: dal telefono",
    html: `<div style="display:grid;grid-template-columns:auto auto 1fr;gap:24px;align-items:center;width:100%">
      ${shot('autista_mobile_proposta.png', 'Proposta: contatti nascosti', '--mh:474px')}
      ${shot('autista_mobile_attiva.png', 'Dopo aver accettato', '--mh:474px')}
      <div>${pts(['Proposta con tempo residuo', 'Guadagno stimato e mappa', '<b>Accetta</b> o <b>Rifiuta</b>', 'Segnalazioni alla centrale', 'Chiusura con km proposti'], 'small')}
      ${shot('autista_storico.png', 'Storico e guadagni', '--mh:150px')}</div></div>`,
    notes: {
      voce: "L'area autista è pensata per chi è in strada, quindi per lo smartphone. Le nuove proposte compaiono da sole: la pagina controlla ogni quindici secondi e si ridisegna solo se qualcosa è cambiato. Nella prima schermata vedete una proposta: il tempo residuo, il luogo, l'officina, il guadagno stimato e la mappa. Nome e telefono del cliente non ci sono ancora: compare la scritta «visibili dopo l'accettazione». Questo non è un effetto grafico: è il server a non inviare quei dati finché l'autista non accetta. Nella seconda schermata la missione è stata accettata e compaiono i contatti del cliente. L'autista può inviare una segnalazione alla centrale e, a fine intervento, chiudere la missione. Il campo dei chilometri è precompilato con la stima del percorso, ma l'autista inserisce quelli effettivi. Se il servizio mappe non risponde, può inserirli comunque. In basso, lo storico mostra le missioni concluse e il totale dei guadagni.",
      semplice: "L'autista vede solo le sue missioni. I dati personali del cliente arrivano dal server solo dopo l'accettazione.",
      file: ['`static/app.js` (`driverActiveCard`, `pollDriverMissions`, `loadDriverHistory`)', '`service/MissionService.java` (`driverMissionDetail`, `complete`)', '`controller/MissionController.java` (`/autista/*`)', '`static/styles.css`'],
      scelta: {
        alt: "Un'app nativa Android/iOS.",
        perche: "Una pagina responsive non richiede installazioni né pubblicazione negli store, e riusa lo stesso login. Il repo documenta l'ottimizzazione per smartphone, non un confronto con le app native.",
        limiti: "Senza app non ci sono notifiche push: l'autista deve tenere aperta la pagina. La mappa richiede connessione.",
      },
      domande: [
        ["Come sa l'autista di avere una proposta?", "La pagina la controlla ogni 15 secondi. Non ci sono notifiche push: è un limite noto."],
        ["Può chiudere la missione con km inventati?", "Il server accetta solo valori non negativi, ma i km sono dichiarati dall'autista. La centrale vede anche i km stimati e può rettificare il costo con un motivo."],
        ["Può mandare note prima di accettare?", "No: il server risponde 409 (V-12, corretta)."],
      ],
    },
  },
  whySlide(FE, MEMBRI.m2,
    ['Un <b>index.html</b>, un <b>app.js</b>', '<b>styles.css</b> per l\'area riservata', '<b>public-site.css</b> per il sito'],
    ['Viste mostrate e nascoste via JS', 'Stato in memoria, aggiornato dalle API', 'Polling ogni 15 secondi, ridisegno se cambia'],
    ['JSON dalle API, stessa origine', 'Cookie di sessione + X-CSRF-TOKEN', 'Dati dinamici sempre con escaping'],
    {
      voce: "Perché abbiamo fatto il frontend così? Per i file: tutto sta in `static`, con un solo HTML e un solo JavaScript, e due fogli di stile separati, uno per l'area riservata e uno per il sito pubblico. È una struttura che si capisce in pochi minuti e che si avvia con il backend. Il pattern è semplice: un oggetto di stato in memoria, riempito dalle API, e funzioni che lo disegnano. Le viste si mostrano e si nascondono via JavaScript, senza ricaricare la pagina. Gli aggiornamenti avvengono con un controllo ogni quindici secondi, e nell'area autista si ridisegna solo se qualcosa è cambiato, per non far perdere il testo che si sta scrivendo. Il formato dei dati è il JSON delle API, sulla stessa origine, con cookie di sessione e il token CSRF nell'intestazione. Tutti i dati dinamici passano da una funzione di escaping, perché un nome cliente non deve poter diventare codice eseguito nella pagina.",
      semplice: "Pochi file, un unico modo di lavorare: chiedi dati, salvali, ridisegna. L'escaping trasforma i caratteri speciali in testo innocuo.",
      file: ['`static/app.js` (`state`, `safe`, `request`)', '`static/index.html`', '`static/styles.css`', '`static/public-site.css`', '`src/test/java/.../ContractAndProviderTests.java`'],
      scelta: {
        alt: "Dividere il JavaScript in moduli; usare un framework reattivo.",
        perche: "Senza build, più file significherebbero più richieste e più ordine da gestire. Il repo non documenta una motivazione esplicita per il file unico: è una conseguenza della scelta senza build.",
        limiti: "`app.js` di 1.391 righe è poco modulare: una crescita delle schermate renderebbe opportuno dividerlo o introdurre un framework.",
      },
      domande: [
        ["Dove si mette un nuovo controllo?", "In `index.html` il markup, in `app.js` l'azione che chiama l'API. Le regole restano nel backend."],
        ["Cosa fa `safe`?", "Sostituisce i caratteri HTML speciali prima di inserire i dati in pagina: è la difesa contro l'XSS, insieme alla Content-Security-Policy."],
      ],
    }),
];

import { C, MEMBRI, pts, shot, card, two, motivi, svg, box, arrow, txt } from './lib.mjs';
import { whySlide } from './slides_2_backend_frontend.mjs';

const CH = 'Chatbot', MA = 'Mappe', NO = 'Notifiche';

export default [
  {
    type: 'divider', section: CH, owner: MEMBRI.m3, member: 'Membro 3', title: 'Chatbot', who: 'Marco Colucci',
    topics: 'Soccorso Stradale Bot (@Fornitori_Soccorso_Bot) · dialogo guidato · riconoscimento targa · creazione della missione',
    notes: {
      voce: "Sono Marco Colucci e ho curato il chatbot. Su Telegram il bot si chiama Soccorso Stradale Bot, con username Fornitori Soccorso Bot. È il punto di contatto con il cliente, che non deve installare nulla oltre a Telegram e non deve registrarsi. Il dialogo è guidato: poche domande precise, una alla volta, come previsto dai dialoghi forniti nel corso. Abbiamo scelto un flusso deterministico, senza intelligenza artificiale: il cliente è in difficoltà e deve ricevere risposte prevedibili. Vedremo il dialogo vero e proprio, poi come il bot riconosce la targa e crea la missione. Sottolineo fin da ora un limite che riprenderemo: la sola targa identifica il cliente. È in linea con i dialoghi del docente, ma non è adatto a dati reali, ed è la voce aperta V-01 del registro di sicurezza. Il bot non vede mai il database: fa tutto chiamando il backend, con un token di servizio.",
      semplice: "Il bot è un programma che chiede le informazioni nell'ordine giusto e le passa al backend. Non decide le regole e non conserva i dati delle missioni.",
      file: ['`chatbot/README.md`', '`chatbot/fornitori_chatbot/`', '`docs/team/MEMBRO_3_CHATBOT.md`', '`docs/management/DECISION_LOG.md` (D-005, D-007)'],
      domande: [
        ["Perché non c'è l'intelligenza artificiale?", "Il dialogo 10 con Gemini è facoltativo e non è implementato. Un flusso a parole chiave è prevedibile e verificabile (D-005)."],
        ["Serve Telegram per provarlo?", "No: con `python3 -m fornitori_chatbot` si usa la console e un backend finto; con `--http` si usa il backend reale."],
      ],
    },
  },
  {
    section: CH, owner: MEMBRI.m3, title: 'Il dialogo con il cliente',
    html: two(`<div style="text-align:center">${shot('chatbot_dialogo_simulato.png', 'Testi reali, prodotti dal bot. Telegram simulato nella grafica.', '--mh:458px')}</div>`,
      `<div>${pts(['Una domanda alla volta', 'Posizione condivisa o indirizzo scritto', 'Riparazione o traino, con i tasti', '<b>AIUTO</b>, <b>ANNULLA</b>, <b>STATO</b> in ogni momento', 'Linguaggio semplice, mai stati interni'], 'small')}
      <div style="margin-top:14px">${svg(600, 96, [
      box(0, 18, 125, 60, 'Targa', [], 'soft', { ts: 19 }), box(158, 18, 125, 60, 'Luogo', [], 'teal', { ts: 19 }), box(316, 18, 125, 60, 'Intervento', [], 'teal', { ts: 19 }), box(474, 18, 125, 60, 'Problema', [], 'teal', { ts: 19 }),
      arrow(127, 48, 154, 48), arrow(285, 48, 312, 48), arrow(443, 48, 470, 48),
    ].join(''))}</div>
      <p class="note-s" style="margin:6px 0 0">Dialogo inattivo per 30 minuti: scade. 3 risposte non valide: ricomincia.</p></div>`, '.82fr 1.18fr'),
    notes: {
      voce: "Questo è un dialogo reale: i testi li ha prodotti il nostro bot collegato a un backend di prova. Solo la grafica di Telegram è simulata. Il cliente scrive un saluto e il bot si presenta. Scrive la targa e il bot risponde con nome e compagnia, poi chiede dove si trova: si può condividere la posizione oppure scrivere via e città. Poi il bot chiede che intervento serve, con due tasti: riparazione sul posto o traino. Infine chiede di descrivere il problema, crea la richiesta e mostra il codice. In ogni momento il cliente può scrivere AIUTO, ANNULLA o STATO. Tecnicamente è una macchina a stati, con quattro stati in `engine.py`, e tutti i testi stanno in `messages.py`, separati dalla logica. Il dialogo scade dopo trenta minuti di inattività e, dopo tre risposte non valide, riparte. STATO non mostra mai gli stati interni: traduce in una frase comprensibile, per esempio «stiamo cercando un autista disponibile».",
      semplice: "Il bot ricorda a che punto è ogni chat e interpreta il messaggio in base a quel punto. Così la stessa parola può voler dire cose diverse a seconda del momento.",
      file: ['`chatbot/fornitori_chatbot/engine.py`', '`chatbot/fornitori_chatbot/messages.py`', '`chatbot/fornitori_chatbot/telegram_channel.py`', '`docs/diagrams/chatbot_sequence.mmd`'],
      scelta: {
        alt: "Linguaggio naturale con AI; dialogo salvato nel database; libreria python-telegram-bot.",
        perche: "Il foglio DialoghiChatbot prevede un flusso guidato. La tabella `chat_sessions` è stata rimossa dallo script 08: i dialoghi stanno in memoria. La sola libreria standard elimina dipendenze esterne.",
        limiti: "Un riavvio del bot azzera i dialoghi in corso (R-07), non le missioni. I dialoghi 2-4 hanno un passo in più del foglio docente: la scelta dell'intervento.",
      },
      domande: [
        ["Se il bot si riavvia durante un dialogo?", "Il cliente ricomincia. Le missioni già create e il loro stato restano nel backend e si possono consultare con STATO."],
        ["Perché ci sono tasti se è una chat?", "Riducono gli errori nella scelta: premere un tasto equivale a scrivere il suo valore. Si può comunque scrivere 1 o 2."],
        ["Questo dialogo è stato provato su Telegram?", "Lo screenshot è simulato. Il repository segna la prova reale da telefono come da ripetere: prima della demo va eseguita."],
      ],
    },
  },
  {
    section: CH, owner: MEMBRI.m3, title: 'Riconoscimento della targa e creazione',
    html: `<div class="center" style="height:auto">${svg(1170, 348, [
      box(10, 18, 200, 80, 'Scrive la targa', ['«pw 660 sw»'], 'yellow', { ts: 19 }),
      box(330, 18, 250, 80, 'Bot: normalizza', ['PW660SW'], 'teal', { ts: 19 }),
      box(700, 18, 460, 80, 'POST /chat/lookup-targa', ['cliente trovato e polizza valida?'], 'blue', { ts: 19 }),
      arrow(212, 58, 326, 58), arrow(582, 58, 696, 58),
      box(700, 128, 460, 72, 'Autorizzazione per 15 minuti', ['una sola richiesta, stessa sessione e cliente'], 'ink', { ts: 18.5, ss: 15.5 }),
      arrow(930, 100, 930, 124),
      box(330, 128, 250, 72, 'Luogo, intervento, problema', [], 'teal', { ts: 15.5 }),
      arrow(696, 164, 584, 164, ''),
      txt(640, 214, 'nome e compagnia', { s: 15, w: 600, c: C.ink }),
      box(330, 245, 250, 86, 'POST /chat/missioni', ['con requestId stabile'], 'blue', { ts: 18 }),
      arrow(455, 202, 455, 241),
      box(700, 245, 460, 86, 'Missione in stato «nuova»', ['Traino: officina più vicina in linea d\'aria'], 'blue', { ts: 19, ss: 15.5 }),
      arrow(582, 288, 696, 288),
      box(10, 245, 200, 86, 'Codice richiesta', ['al cliente'], 'yellow', { ts: 18.5 }),
      arrow(326, 288, 214, 288),
    ].join(''))}</div>
    <div class="three" style="margin-top:12px">
      <div class="note-s" style="font-size:20px"><b>5</b> targhe non trovate: blocco (429)<br><b>10</b> ricerche al minuto per sessione</div>
      <div class="note-s" style="font-size:20px"><b>requestId</b> uguale nei tentativi: nessuna doppia missione</div>
      <div class="note-s" style="font-size:20px;color:#a33">La targa <b>non prova l'identità</b>: limite demo V-01</div>
    </div>`,
    notes: {
      voce: "Vediamo che cosa succede dietro le quinte. Il cliente scrive la targa, anche in minuscolo o con spazi: il bot la normalizza e la manda al backend. Il backend cerca il cliente e controlla che la polizza non sia scaduta. Se è tutto a posto, restituisce nome e compagnia e crea un'autorizzazione: per quindici minuti, per quella sessione e quel cliente, si può aprire una sola richiesta. Se scade durante il dialogo, il bot rifà da solo il controllo una volta. Raccolti luogo, intervento e problema, il bot chiama la creazione della missione con un requestId. Questo è il nostro antiduplicato: se la rete cade e il bot ritenta, l'identificativo è lo stesso e non nasce una seconda missione. Se viene riusato con dati diversi, il backend risponde 409. Per il traino il backend sceglie l'officina più vicina, trasformando l'indirizzo in coordinate. Contro i tentativi a caso, dopo cinque targhe non trovate il backend blocca la sessione.",
      semplice: "La targa apre una porta per poco tempo e per una sola richiesta. Il requestId è come il numero di una raccomandata: se la rispedisci, non diventa una seconda lettera.",
      file: ['`service/ChatService.java`', '`service/ChatLookupLimiter.java`', '`service/NearestWorkshopResolver.java`', '`repository/ChatLookupGrantRepository.java`', '`chatbot/fornitori_chatbot/engine.py` (`normalize_plate`)', '`chatbot/fornitori_chatbot/http_backend.py`', '`database/07_backend_hardening.sql`'],
      scelta: {
        alt: "Verifica con un secondo dato o con un codice SMS; il cliente sceglie l'officina.",
        perche: "D-007: come nei dialoghi del docente, basta la targa. Il PDF chiede solo di verificare che la targa corrisponda a un assicurato. H-008: il cliente in panne non sa quale officina scegliere, quindi la assegna il backend.",
        limiti: "**V-01 aperta:** chi conosce una targa vede nome e compagnia e può creare una richiesta. L'audit propone di non mostrare più quei dati e aggiungere le ultime 4 cifre della polizza o un codice SMS. Officina scelta in linea d'aria, correggibile dalla centrale. I limiti sono in memoria.",
      },
      domande: [
        ["Chiunque può vedere nome e compagnia con una targa?", "Sì, è il limite V-01, dichiarato e ad alta gravità. È adatto solo a dati sintetici. Per dati reali serve un secondo fattore: la soluzione è già descritta nell'audit."],
        ["Perché non chiedete il numero di telefono?", "Telegram non lo comunica al bot. Per questo lo stato si cerca per chat e non per numero."],
        ["Cosa impedisce di provare migliaia di targhe?", "5 targhe non trovate per sessione, 10 ricerche al minuto per sessione, 100 al minuto globali. Una targa trovata non azzera il contatore (V-03, corretta)."],
      ],
    },
  },
  whySlide(CH, MEMBRI.m3,
    ['<b>engine.py</b>: macchina a stati', '<b>messages.py</b>: tutti i testi', '<b>telegram_channel.py</b>: solo il canale'],
    ['Macchina a stati del dialogo', 'Backend come interfaccia: HTTP o finto', 'InboundMessage: il canale è sostituibile'],
    ['JSON verso il backend, con token', 'Targa normalizzata per la ricerca', 'requestId e impronta SHA-256'],
    {
      voce: "Perché il bot è fatto così? I file separano tre preoccupazioni. `engine.py` contiene la logica del dialogo; `messages.py` contiene tutti i testi, così si correggono senza toccare la logica; `telegram_channel.py` conosce solo Telegram. Questo porta al primo pattern: il messaggio in arrivo viene trasformato in un `InboundMessage` neutro. Il motore non sa da dove arriva, quindi aggiungere WhatsApp significherebbe scrivere un adattatore, senza toccare il dialogo. Il secondo pattern è l'interfaccia verso il backend, `BackendClient`, con due implementazioni: quella HTTP e quella finta, usata per provare il bot da console senza server. Il terzo è ovviamente la macchina a stati. Sui formati: JSON con token bearer verso il backend, targa normalizzata per la ricerca e, per gli antiduplicati, un requestId con un'impronta SHA-256 della richiesta, che non contiene i dati in chiaro. Se il requestId viene riusato con dati diversi, l'impronta non coincide e il backend rifiuta.",
      semplice: "Ogni file ha un solo mestiere. Puoi cambiare i testi, il canale o il backend separatamente, senza toccare il resto.",
      file: ['`chatbot/fornitori_chatbot/engine.py`', '`chatbot/fornitori_chatbot/messages.py`', '`chatbot/fornitori_chatbot/backend.py`', '`chatbot/fornitori_chatbot/fake_backend.py`', '`chatbot/fornitori_chatbot/__main__.py`', '`service/ChatService.java` (`fingerprint`)'],
      scelta: {
        alt: "Scrivere i testi nel codice; bot accoppiato a Telegram; webhook invece del long polling.",
        perche: "Il long polling evita un indirizzo pubblico (H-009). Il bot risponde solo nelle chat private. La separazione tra canale e motore è documentata nel README del chatbot.",
        limiti: "Il dialogo è in RAM. La prova da telefono è segnata come da ripetere (B-10). I 6 test del chatbot coprono le notifiche, non l'intero dialogo.",
      },
      domande: [
        ["Perché Python se il backend è Java?", "Il bot è separato e parla solo HTTP, quindi può usare un altro linguaggio. Nei documenti non c'è una motivazione formale: il vantaggio verificabile è che gira con la sola libreria standard."],
        ["Cosa serve per aggiungere WhatsApp?", "Un adattatore che crea `InboundMessage` e una funzione di invio per il dispatcher, oltre a webhook firmati e all'account Business: per questo è fuori ambito."],
      ],
    }),
  {
    type: 'divider', section: MA, owner: MEMBRI.m4, member: 'Membro 4', title: 'Mappe', who: 'Marco Colucci',
    topics: 'Geocoding con Nominatim · percorso con OSRM · km stimati · mappa Leaflet',
    notes: {
      voce: "Le mappe sono la mia seconda area, e rispondono a tre domande pratiche. Dove si trova il cliente, se ha scritto soltanto un indirizzo? Qual è il percorso dalla base dell'autista al cliente e, per un traino, fino all'officina? Quanti chilometri stimiamo? Per rispondere usiamo due servizi open source e gratuiti: Nominatim trasforma un indirizzo in coordinate, cioè fa il geocoding, e OSRM calcola il percorso su strada. Il risultato viene disegnato nel browser con Leaflet sui tile di OpenStreetMap. Un principio ha guidato questa parte: le mappe aiutano, ma non devono mai bloccare il lavoro. Se un servizio esterno non risponde, l'autista può comunque chiudere la missione inserendo i chilometri. E non inventiamo mai una posizione: se l'indirizzo non si trova, lo diciamo. Vedremo prima il flusso nel backend e poi come appare sullo schermo, con una schermata reale.",
      semplice: "Geocoding: da «Via Bovio, Terlizzi» a latitudine e longitudine. Routing: dal punto A al punto B, seguendo le strade. Leaflet: il disegno sulla pagina.",
      file: ['`docs/team/MEMBRO_4_MAPS.md`', '`controller/MapController.java`', '`service/MapService.java`', '`docs/management/DECISION_LOG.md` (D-011, D-016, H-010)'],
      domande: [
        ["Che differenza c'è tra km stimati ed effettivi?", "Gli stimati vengono da OSRM e sono proposti alla chiusura; gli effettivi li dichiara l'autista e sono quelli usati per il costo."],
        ["Quali dati escono dal sistema?", "L'indirizzo del sinistro va a Nominatim. Va bene per una demo con dati sintetici, ma è da rivalutare per dati reali."],
      ],
    },
  },
  {
    section: MA, owner: MEMBRI.m4, title: 'Geocoding e percorso',
    html: `<div class="center" style="height:auto">${svg(1170, 340, [
      box(10, 120, 200, 96, 'Browser', ['GET /maps/route', '+ X-CSRF-TOKEN'], 'yellow', { ts: 19, ss: 15 }),
      box(300, 110, 260, 116, 'MapService', ['permessi', 'punti del percorso', 'salva coordinate e km'], 'blue', { ss: 15.5 }),
      box(690, 18, 470, 92, 'Nominatim: indirizzo → coordinate', ['solo se mancano · Puglia · 1 richiesta al secondo'], 'light', { ts: 19, ss: 15.5 }),
      box(690, 222, 470, 104, 'OSRM: percorso su strada', ['base → sinistro → officina (se traino)', 'km e geometria GeoJSON'], 'light', { ts: 19, ss: 15.5 }),
      arrow(212, 168, 296, 168),
      arrow(562, 140, 686, 80),
      txt(700, 140, 'solo la prima volta', { a: 'start', s: 16, w: 600, c: C.ink }),
      arrow(562, 196, 686, 268),
    ].join(''))}</div>
    <div class="three" style="margin-top:6px">
      ${card('Esiti chiari', '<b>422</b> indirizzo non trovato<br><b>503</b> servizio non disponibile', C.blue)}
      ${card('Provider protetti', 'Timeout 3 s e 8 s<br>Niente redirect, max 1 MiB', C.indigo)}
      ${card('Il lavoro continua', 'I km si possono sempre inserire a mano', C.teal)}
    </div>`,
    notes: {
      voce: "Quando si apre una mappa, il browser chiama un'unica API: `maps/route`, indicando la missione e, se serve, l'autista. `MapService` controlla prima i permessi: l'autista vede solo le proprie missioni. Poi compone i punti: la base del mezzo, il luogo del sinistro e, solo per un traino, l'officina. Se il luogo non ha ancora coordinate, chiede a Nominatim di trasformare l'indirizzo e le salva sulla missione, così la conversione avviene una volta sola. La ricerca è limitata alla Puglia e rispetta il limite di una richiesta al secondo. Poi chiede a OSRM il percorso, che torna con i chilometri e la geometria in GeoJSON. I km stimati si salvano solo per l'autista assegnato. Gli errori sono espliciti: 422 se l'indirizzo non esiste, 503 se il servizio è giù. I client hanno timeout di tre e otto secondi, non seguono redirect e leggono al massimo un mebibyte. In nessun caso il lavoro si ferma: i km si inseriscono a mano.",
      semplice: "Il backend fa da intermediario. Il browser chiede «il percorso della missione 31», il backend fa il resto e restituisce un risultato pronto da disegnare.",
      file: ['`service/MapService.java`', '`service/NominatimGeocodingClient.java`', '`service/OsrmRoutingClient.java`', '`service/MapProviders.java`', '`service/ProviderHttp.java`', '`dto/MapDtos.java`'],
      scelta: {
        alt: "Google Maps Geocoding e Directions; Mapbox; calcolo diretto dal browser.",
        perche: "H-010 documenta la scelta: servizi pubblici senza chiavi e costi. Il tramite del backend applica permessi e limiti e può salvare i risultati, mentre la CSP vieta al browser connessioni ad altri domini.",
        limiti: "Servizi pubblici senza garanzia di disponibilità (R-04). Fuori dalla Puglia un indirizzo risulta «non trovato» (è configurabile con `fornitori.maps.viewbox`). L'indirizzo va a un servizio esterno.",
      },
      domande: [
        ["Perché non avete usato Google Maps?", "Richiede chiave e fatturazione e invia comunque i dati a un terzo. Nominatim e OSRM sono open source e, se serve, si possono ospitare in proprio."],
        ["Perché la geocodifica avviene alla prima mappa?", "Non serve bloccare la creazione della missione con una chiamata esterna, salvo il traino da chat, dove serve subito per scegliere l'officina."],
        ["Perché la GET scrive sul database?", "Salva coordinate e km stimati la prima volta. Per questo è protetta dal CSRF; è un'anomalia registrata come V-24."],
      ],
    },
  },
  {
    section: MA, owner: MEMBRI.m4, title: 'La mappa sullo schermo: Leaflet',
    html: two(shot('mappa_area.png', 'Schermata reale: base a Molfetta, cliente a Terlizzi, officina a Ruvo', '--mh:450px'),
      `<div>${pts(['Tile di <b>OpenStreetMap</b>, percorso <b>GeoJSON</b> di OSRM', 'Segnaposto per base, cliente e officina', 'Fonte e km stimati sempre indicati', 'Autista: si apre da sola. Centrale: su richiesta'], 'small')}
      <div class="card" style="--c:${C.violet};margin-top:14px;text-align:center"><h3 style="font-size:34px;margin:0">15,23 km</h3><div>stimati da OSRM per questa missione</div></div></div>`, '1.25fr .75fr'),
    notes: {
      voce: "Ecco il risultato sullo schermo, ripreso dal sistema in funzione. La missione è un traino: l'autista parte dalla sua base a Molfetta, raggiunge il cliente a Terlizzi e porta l'auto a un'officina a Ruvo di Puglia. La linea viola è la geometria restituita da OSRM. Leaflet la riceve così com'è, in formato GeoJSON, e la disegna sui tile di OpenStreetMap. I tile sono le piccole immagini quadrate che compongono la mappa, e li scarica direttamente il browser: infatti la politica di sicurezza dei contenuti ammette per le immagini quel solo dominio. Sotto la mappa, l'elenco numerato dei punti. Sopra, una frase ricorda la fonte e che i chilometri sono una stima: quelli effettivi si confermano alla chiusura. Nell'area autista la mappa si carica da sola; nella centrale compare premendo «Mostra mappa», scegliendo eventualmente un autista per confrontare il percorso. Senza autista assegnato, si vede solo il punto del cliente.",
      semplice: "Leaflet è il «foglio da disegno» della mappa nel browser. Riceve punti e linea dal backend e non calcola nulla da solo.",
      file: ['`static/app.js` (`drawMissionMap`)', '`static/index.html` (Leaflet da CDN)', '`config/SecurityConfig.java` (CSP)', '`docs/team/MEMBRO_4_MAPS.md`'],
      scelta: {
        alt: "Google Maps JavaScript API; MapLibre o OpenLayers.",
        perche: "Leaflet è leggero, gratuito e disegna direttamente il GeoJSON di OSRM, senza chiavi. Il repo documenta Leaflet come scelta (D-011), non il confronto con MapLibre o OpenLayers.",
        limiti: "I tile pubblici non sono pensati per un uso intensivo. Serve Internet. Se Leaflet non si carica, la pagina scrive «Mappa non caricata: controlla la connessione».",
      },
      domande: [
        ["Cosa succede se OSRM è giù durante la demo?", "La pagina mostra un errore chiaro e l'autista può inserire i km a mano: la chiusura non dipende dalla mappa."],
        ["Perché l'autista vede il percorso prima di accettare?", "Per valutare la distanza e il guadagno stimato. I dati personali del cliente restano nascosti fino all'accettazione."],
      ],
    },
  },
  whySlide(MA, MEMBRI.m4,
    ['<b>MapController</b>: una sola API', '<b>MapService</b>: compone il percorso', 'Un client per ciascun provider'],
    ['Interfacce per i provider, sostituibili', 'Backend come intermediario', 'Nessuna transazione durante le chiamate'],
    ['GeoJSON per la geometria', 'Coordinate NUMERIC(10,6)', 'BigDecimal per i km stimati'],
    {
      voce: "Perché le mappe sono fatte così? Sul fronte file, c'è un solo controller con una sola API, un service che compone il percorso e un client per ciascun provider. `MapProviders` definisce le interfacce `GeocodingClient` e `RoutingClient`: per passare a un altro servizio basta una nuova implementazione, e questo ci permette anche di usare un provider simulato nei test. Il backend fa da intermediario: i permessi sono applicati in un solo punto, e la chiave, se un giorno servisse, non finirebbe mai nel browser. Un pattern importante è che `MapService` non è transazionale, di proposito: i servizi esterni possono metterci secondi e non vogliamo tenere aperta una transazione mentre aspettiamo. I risultati vengono salvati poi, in transazioni brevi. Sui formati: GeoJSON, che è lo standard capito sia da OSRM sia da Leaflet, quindi non serve conversione; coordinate NUMERIC con sei decimali e BigDecimal per i chilometri.",
      semplice: "Il provider si può cambiare senza riscrivere il resto. GeoJSON è un linguaggio comune tra chi calcola il percorso e chi lo disegna.",
      file: ['`service/MapProviders.java`', '`service/MapService.java`', '`service/ProviderHttp.java`', '`dto/MapDtos.java`', '`src/test/java/.../ContractAndProviderTests.java`'],
      scelta: {
        alt: "Chiamare i provider direttamente dentro MissionService; polilinee compresse invece di GeoJSON.",
        perche: "Separare i client rende il resto indipendente dal provider. `overview=full&geometries=geojson` chiede a OSRM un formato che Leaflet disegna senza decodifica.",
        limiti: "Limite di una richiesta al secondo solo per istanza. Il controllo «officina più vicina» e «autista più vicino» usa la linea d'aria, non il percorso su strada.",
      },
      domande: [
        ["Come testate senza chiamare davvero i servizi?", "I test usano provider simulati e controllano anche valori non validi, per esempio una distanza negativa (V-20, corretta)."],
        ["Perché coordinate con sei decimali?", "Sei decimali corrispondono a una precisione di circa 10 cm, più che sufficiente per una posizione stradale."],
      ],
    }),
  {
    type: 'divider', section: NO, owner: MEMBRI.m5, member: 'Membro 5', title: 'Notifiche', who: 'Marika Sancilio · Francesco Valeriano · Giuseppe Cangellosi',
    topics: 'Outbox · dispatcher Telegram · stato di invio · consegna senza duplicati tra dispatcher',
    notes: {
      voce: "Siamo Marika Sancilio, Francesco Valeriano e Giuseppe Cangellosi e presentiamo le notifiche. Lo scopo è semplice: avvisare il cliente su Telegram in due momenti, quando un autista prende in carico la richiesta e quando l'intervento è concluso. Il problema vero è l'affidabilità. Che cosa succede se Telegram non risponde, se il bot è spento o se ci sono due bot accesi? Non vogliamo perdere messaggi senza accorgercene e non vogliamo inviarli due volte. La nostra risposta si divide in due parti. Nel backend c'è l'outbox: una coda di notifiche dentro il database, scritta insieme alla missione. Nel bot c'è il dispatcher: ogni cinque secondi riserva le notifiche da inviare, le spedisce e registra l'esito. Vedrete anche come trattiamo il caso limite, che dichiariamo apertamente. Le notifiche partono solo per le missioni create dal bot: per quelle create dalla centrale, la chat del cliente non è nota.",
      semplice: "Le notifiche sono due messaggi al cliente. La parte difficile è inviarli in modo affidabile anche quando qualcosa si rompe.",
      file: ['`docs/team/MEMBRO_5_NOTIFICATIONS.md`', '`entity/NotificationOutbox.java`', '`chatbot/fornitori_chatbot/notifications.py`', '`database/08_cleanup_and_delivery.sql`'],
      domande: [
        ["Perché non partono per le missioni della centrale?", "Telegram può scrivere solo a chi ha aperto una chat con il bot, e di quei clienti la chat non è nota. La riga nasce `SKIPPED` e non conserva il telefono."],
        ["Il cliente può disattivare gli avvisi?", "No: il bot invia solo i due messaggi legati alla sua richiesta. Il consenso non è previsto nei dialoghi del docente (limite dichiarato nel README)."],
      ],
    },
  },
  {
    section: NO, owner: MEMBRI.m5, title: 'Outbox: la notifica nasce con la missione',
    html: `<div class="center" style="height:auto">${svg(1170, 300, [
      `<rect x="10" y="10" width="640" height="280" rx="14" fill="${C.canvas}" stroke="${C.blue}" stroke-width="2" stroke-dasharray="7 6"/>`,
      txt(30, 40, 'Una sola transazione: tutto salvato, o niente', { a: 'start', c: C.blue, w: 800, s: 19 }),
      box(35, 62, 190, 90, 'L\'autista', ['accetta'], 'indigo'),
      box(285, 62, 340, 90, 'missions', ['stato = accettata'], 'blue'),
      box(285, 176, 340, 90, 'notifications_outbox', ['MISSION_ACCEPTED · PENDING'], 'ink', { ts: 19 }),
      arrow(228, 100, 281, 100), arrow(228, 125, 281, 210),
      box(760, 62, 400, 90, 'Backend risponde subito', ['Telegram non è ancora coinvolto'], 'light', { ts: 19 }),
      arrow(628, 106, 756, 106),
      box(760, 176, 400, 90, 'Il dispatcher invia dopo', ['anche se ora il bot è spento'], 'teal', { ts: 19 }),
      arrow(628, 220, 756, 220),
    ].join(''))}</div>
    <div class="three" style="margin-top:14px">
      ${card('event_key univoca', 'Lo stesso evento non si può registrare due volte', C.blue)}
      ${card('Payload minimo', 'Evento, codice, nome autista: nessun dato del cliente', C.indigo)}
      ${card('Canale di origine', 'Telegram: PENDING. Centrale: SKIPPED', C.yellow)}
    </div>`,
    notes: {
      voce: "Il pattern outbox risolve un problema classico. Quando l'autista accetta, dobbiamo fare due cose: aggiornare la missione e avvisare il cliente. Ma il database e Telegram sono due sistemi diversi, e non esiste una transazione che li comprenda entrambi. Se inviassimo prima il messaggio e poi il salvataggio fallisse, il cliente verrebbe avvisato di una cosa mai avvenuta. Se salvassimo e poi Telegram fosse giù, il messaggio andrebbe perso. La soluzione è scrivere la notifica in una tabella dello stesso database, nella stessa transazione della missione. O sono salvate entrambe o nessuna delle due. L'autista riceve subito la risposta, senza aspettare Telegram, e l'invio avviene dopo. La colonna `event_key` è univoca, quindi lo stesso evento non può generare due righe. Il payload contiene l'evento, il codice della missione e il nome dell'autista: nessun dato del cliente. Le missioni della centrale non hanno destinatario, quindi la riga nasce già come SKIPPED.",
      semplice: "Invece di spedire subito, si mette la lettera nella cassetta della posta interna insieme alla modifica. Il postino passerà dopo. La cassetta è nel database, quindi non si perde.",
      file: ['`service/MissionService.java` (`addOutboxEvent`, `accept`, `complete`)', '`entity/NotificationOutbox.java`', '`database/01_schema.sql`', '`database/08_cleanup_and_delivery.sql`', '`docs/diagrams/chat_notifications_sequence.mmd`'],
      scelta: {
        alt: "Invio diretto a Telegram dal service; una coda esterna come RabbitMQ o Kafka.",
        perche: "D-015 e H-006 documentano l'outbox nella transazione di dominio. Una coda esterna aggiungerebbe un servizio da installare e non risolverebbe da sola l'atomicità con il database. Quest'ultimo confronto è ricostruito: non è nel registro.",
        limiti: "Il bot deve controllare periodicamente la coda (ogni 5 secondi), quindi c'è un piccolo ritardo. La tabella cresce: non c'è una politica di pulizia, fuori ambito per la demo.",
      },
      domande: [
        ["Perché non inviare direttamente il messaggio?", "Un errore di Telegram bloccherebbe l'accettazione o lascerebbe i dati incoerenti. Con l'outbox i due passaggi sono separati, e il messaggio non si perde."],
        ["Perché non Kafka o RabbitMQ?", "Sono un'infrastruttura in più da gestire. Avendo già PostgreSQL, una tabella con SKIP LOCKED basta per questi volumi."],
        ["Se il bot resta spento per ore?", "Le righe restano in `PENDING` e partono alla riaccensione."],
      ],
    },
  },
  {
    section: NO, owner: MEMBRI.m5, title: 'Dispatcher: consegna senza duplicati',
    html: `<div style="display:grid;grid-template-columns:1fr auto;gap:30px;height:100%"><div>${svg(760, 280, [
      box(10, 20, 190, 76, 'PENDING', ['da inviare'], 'yellow', { ss: 15 }),
      box(290, 20, 200, 76, 'SENDING', ['riservata 5 minuti'], 'indigo', { ss: 15 }),
      box(580, 20, 170, 76, 'SENT', ['esito ok'], 'teal', { ss: 15 }),
      box(290, 190, 200, 76, 'FAILED', ['dopo 5 tentativi'], 'ink', { ss: 15 }),
      box(10, 190, 190, 76, 'SKIPPED', ['missione centrale'], 'soft', { ss: 15 }),
      arrow(203, 46, 286, 46, 'claim', { dy: -9 }),
      arrow(493, 58, 576, 58, 'esito', { dy: -9 }),
      arrow(290, 82, 203, 82, '', { color: C.muted, dash: true }),
      arrow(390, 99, 390, 186, '', { color: C.muted }),
      txt(205, 124, 'errore: riprova dopo 30 s', { s: 16, w: 600 }),
    ].join(''))}
    <div style="margin-top:14px">${pts([
        '<b>SKIP LOCKED</b>: due dispatcher non ricevono la stessa riga',
        '<b>Tentativo</b> nell\'esito: un esito vecchio è ignorato',
        '<b>Riserva 5 minuti</b>: un bot caduto non blocca la coda',
        '<b>Limite</b>: crash tra invio ed esito, il messaggio può ripartire una volta',
      ], 'small')}</div></div>${shot('chatbot_stato_notifiche_simulato.png', 'Testi reali, grafica simulata', '--mh:400px')}</div>`,
    notes: {
      voce: "Il dispatcher è la seconda metà. Ogni cinque secondi il bot chiama l'API di claim. Il backend, nella stessa transazione, seleziona fino a venti righe con FOR UPDATE SKIP LOCKED e le passa a SENDING. SKIP LOCKED significa: salta le righe che un altro sta già prendendo. Così due dispatcher non ricevono mai la stessa riga: l'abbiamo provato con quattro in parallelo. Il bot invia e registra l'esito, riportando il numero di tentativo. Se un bot in ritardo manda un esito vecchio, viene ignorato, così non altera una riserva più recente. Se l'invio fallisce, la riga riprova dopo trenta secondi; dopo cinque tentativi passa a FAILED, con l'errore salvato. Se il bot si ferma, la riserva scade dopo cinque minuti e la riga torna disponibile. Resta un solo caso che non possiamo escludere: se il bot si ferma proprio tra l'invio e l'esito, il messaggio può ripartire una volta, perché Telegram non offre una chiave anti-duplicati. Lo dichiariamo come limite accettato per la demo.",
      semplice: "Prima prenoto la lettera, poi la spedisco, poi segno com'è andata. Se sparisco dopo aver prenotato, dopo cinque minuti qualcun altro può riprenderla.",
      file: ['`chatbot/fornitori_chatbot/notifications.py`', '`repository/NotificationOutboxRepository.java` (`lockClaimable`)', '`service/ChatService.java` (`claimNotifications`, `reportNotification`)', '`src/test/java/.../ChatAndNotificationTests.java`', '`chatbot/tests/test_notifications.py`'],
      scelta: {
        alt: "Consegna «esattamente una volta»; gestire la coda senza stato SENDING.",
        perche: "L'esattamente una volta non è ottenibile senza una chiave anti-duplicati del provider: per questo D-015 dichiara «almeno una volta in caso di crash». La riserva persistente è documentata in D-015 e nel contratto d'integrazione.",
        limiti: "**V-14, accettata per la demo:** messaggio duplicato possibile in caso di crash. Nel processo vivo, il dispatcher ricorda le notifiche inviate con esito non registrato e non le rispedisce. FAILED non è inviata: va letta su `ultimo_errore`.",
      },
      domande: [
        ["Quindi i duplicati sono impossibili?", "Tra dispatcher sì. Resta possibile un doppio invio se il bot si ferma tra invio ed esito: è un compromesso consapevole, preferibile a perdere il messaggio."],
        ["Perché «almeno una volta» e non «al massimo una volta»?", "Meglio un avviso ripetuto che un cliente che non sa che l'autista sta arrivando."],
        ["Chi controlla le notifiche in FAILED?", "Oggi nessuna interfaccia le mostra: si leggono su `notifications_outbox.ultimo_errore`. È un possibile miglioramento."],
      ],
    },
  },
  whySlide(NO, MEMBRI.m5,
    ['<b>NotificationOutbox</b> nel backend', '<b>notifications.py</b> nel bot', 'API claim ed esito separate'],
    ['Transactional outbox', 'Riserva con scadenza (lease)', 'Dispatcher indipendente dal canale'],
    ['Payload JSONB minimo', 'Stati in maiuscolo, con CHECK', 'tentativo per legare l\'esito'],
    {
      voce: "Ricapitoliamo perché abbiamo strutturato così le notifiche. I file seguono la divisione delle responsabilità: nel backend, l'entità `NotificationOutbox` e la logica di claim ed esito; nel bot, soltanto `notifications.py`. Il bot non tocca il database: passa da due API dedicate. I pattern sono tre. Il transactional outbox garantisce che evento e dato nascano insieme. La riserva con scadenza, che in inglese si chiama lease, evita che un bot caduto blocchi per sempre la coda. E il dispatcher è indipendente dal canale: riceve una funzione di invio e non sa nulla di Telegram, quindi un nuovo canale richiede solo un adattatore. Sui formati: il payload è in JSONB e contiene il minimo, per non duplicare dati personali in una tabella di servizio. Gli stati sono controllati da un vincolo CHECK nel database. Infine, il numero di tentativo lega l'esito alla riserva corretta. Tutto questo è testato anche con dispatcher concorrenti.",
      semplice: "Il database è il punto di verità anche per le notifiche. Il bot è un semplice esecutore che può fermarsi e ripartire senza danni.",
      file: ['`entity/NotificationOutbox.java`', '`chatbot/fornitori_chatbot/notifications.py`', '`controller/ChatController.java`', '`database/09_integrity_and_indexes.sql`', '`docs/team/MEMBRO_5_NOTIFICATIONS.md`'],
      scelta: {
        alt: "Dispatcher dentro il backend Java.",
        perche: "Il bot è già il processo che parla con Telegram e possiede il token: tenere lì il dispatcher evita di dare il token al backend. Il repository descrive la soluzione ma non questo confronto, quindi il motivo è ricostruito.",
        limiti: "Il dispatcher parte solo con `--telegram`. Se il processo del bot è fermo, non parte nessun invio, anche se il backend funziona.",
      },
      domande: [
        ["Perché il dispatcher sta nel bot?", "Così il token Telegram resta solo nel bot, e il backend non deve conoscere il provider."],
        ["Come aggiungereste gli SMS?", "Con un nuovo canale e una funzione di invio. I valori ammessi in `channel` vanno estesi: oggi sono `telegram`, `whatsapp` e `internal`."],
      ],
    }),
];

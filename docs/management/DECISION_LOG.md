# Registro decisioni consolidato

**Codice:** FOR-DEC-001 · **Versione:** 2.0 · **Data:** 04/11/2026
Questo registro consolida la baseline corrente e conserva in appendice il contesto
dei record storici, con ID storici `H-*`. Non attribuisce approvazioni a persone
non identificate; le decisioni di business restano da confermare col docente dove
indicato.

| ID | Tema | Decisione/baseline documentata | Stato | Fonte |
|---|---|---|---|---|
| D-001 | Stack backend | Java 25, Spring Boot 4.1.2, Maven Wrapper, PostgreSQL 18/compatibile | Implementato; versione da valutare solo con change | `pom.xml`, README |
| D-002 | Tariffa | Seed: €50 uscita, €1,20/km, supplemento 25%; calcolo Java in `Europe/Rome` | Implementato nel servizio; regola docente e timestamp restano da confermare | `database/02_seed.sql`, `PricingService.java`, report Sprint 2 |
| D-003 | Timeout | Seed 10 minuti; scheduler applicativo gestisce scadenze live | Implementato; intervallo effettivo configurabile | SQL seed, `MissionExpiryScheduler.java` |
| D-004 | Storico | `is_fixture` distingue righe didattiche dal lavoro live | Implementato | Schema SQL e servizi missione |
| D-005 | Canale chatbot | Flusso a menu deterministico, senza AI obbligatoria | Implementato per console/Telegram; Gemini facoltativo non implementato | `chatbot/README.md` |
| D-006 | Segreti | Token chatbot fornito tramite variabile d'ambiente condivisa col backend | Implementato come configurazione; rotazione/secret store di produzione non documentati | README chatbot e proprietà backend |
| D-007 | Identità chatbot | La sola targa identifica il cliente, come nei dialoghi del docente | Limite noto della demo | `chatbot/README.md` |
| D-008 | Telegram | Bot a long polling, notifiche implementate nel modulo | Implementato | `chatbot/README.md` |
| D-009 | WhatsApp e webhook | Non integrati | Fuori ambito | OpenAPI `designed`, guide del team |
| D-010 | Frontend | Client HTML/CSS/JavaScript servito da Spring Boot per responsabile e autista | Implementato | `src/main/resources/static/` |
| D-011 | Mappe | `GET /api/v1/maps/route`, Nominatim, OSRM e mappa Leaflet | Implementato | `MapController`, `MapService`, `app.js` |
| D-012 | Docente | File PDF/Excel non presenti nel repository | Richiesta di acquisizione aperta | Repository |
| D-013 | Gestione utenti | Non esposta: un utente autista richiede anche autista e mezzo dedicati, gli account arrivano dal seed | Fuori ambito | OpenAPI `designed` |
| D-014 | Notifiche della centrale | Nessun invio: la chat del cliente non è nota; righe `SKIPPED` senza telefono | Implementato | `MissionService`, `08_cleanup_and_delivery.sql` |
| D-015 | Stato di invio | Riserva persistente `PENDING → SENDING → SENT` con scadenza; consegna almeno una volta in caso di crash | Implementato | `ChatService`, `NotificationDispatcher` |
| D-016 | Mappe, codici di risposta | Vale il comportamento del codice: 404 per autista inesistente, 200 col solo sinistro senza autista | Implementato | `MapService`, OpenAPI |

## Decisioni da richiedere

- Conferma dei requisiti obbligatori/facoltativi, dataset e acceptance dal docente.
- Quale timestamp determina tariffa notturna e quale versione delle regole è normativa.
- Acceptance della UI e necessità di WhatsApp e di un'autenticazione più forte del chatbot.
- Policy privacy/provider esterni Nominatim/OSRM e owner del contract source.

## Registro storico consolidato

I record seguenti provengono dal vecchio `docs/DECISION_LOG.md`; gli ID `H-001`–
`H-010` preservano l'ordine originale D-001–D-010. Sono stati mantenuti per
tracciabilità e non prevalgono sulla baseline corrente sopra. In particolare,
H-006 sul canale outbox è stato aggiornato dal successivo H-009.

### H-001 — Storico docente e missioni live (ID originale D-001)

- **Decisione:** importare lo storico con `is_fixture=TRUE`; disponibilità e
  scheduler operativo esaminano esclusivamente righe `is_fixture=FALSE`.
- **Motivo:** non alterare il riepilogo storico né marcare come scadute missioni
  già aperte alla data del 28/10/2026.
- **Conseguenza:** le due assegnazioni aperte dell'autista 3 restano intatte; la
  disponibilità live non si basa sullo stato seed.
- **Stato:** adottata nel modello e nel seed; flusso live implementato in Sprint 2.

### H-002 — Fascia tariffaria notturna (ID originale D-002)

- **Decisione applicata, da confermare col docente:** usare il timestamp di
  accettazione della missione; fascia 22:00–08:00 (dal 04/11/2026, prima 20:00–08:00), moltiplicatore 1,25.
- **Fonte dichiarata nel record storico:** il foglio Excel precisa l'ora di
  accettazione; il PDF indica 20:00–8:00 ma non il timestamp da usare. L'inizio alle 22:00 è una
  scelta del team che si discosta dal PDF.
- **Stato:** implementata in Sprint 2; conferma di business ancora aperta.

### H-003 — Rounding (ID originale D-003)

- **Decisione:** memorizzare costi con `NUMERIC(10,2)` e importi Java con
  `BigDecimal`; non arrotondare chilometri prima del calcolo.
- **Rounding:** arrotondamento finale `HALF_UP` a due decimali.
- **Stato:** adottata.

### H-004 — Autenticazione (ID originale D-004)

- **Decisione:** sessione server-side, JSON per login, cookie sessione HttpOnly,
  BCrypt e CSRF in sessione.
- **Stato:** implementata; credenziali demo in chiaro non vengono riportate nei
  file versionati.

### H-005 — Versione Java nel POM (ID originale D-005)

- **Decisione:** Java 25 LTS; `java.version` e `maven.compiler.release` sono 25.
  Maven Wrapper resta 3.9.9.
- **Motivo:** target condiviso. Spring Boot 4.1.2 è mantenuto senza cambio major.
- **Stato:** adottata.

### H-006 — Canale outbox notifiche (ID originale D-006, superato da H-009)

- **Decisione storica:** scrivere eventi `MISSION_ACCEPTED` e `MISSION_COMPLETED`
  nell'outbox nella stessa transazione di dominio; usare `channel=internal` finché
  M1/M5 non concordano il mapping dei provider.
- **Stato registrato all'epoca:** applicata nel backend Sprint 2; dispatcher e
  contratto provider indicati fuori perimetro.

### H-007 — Autenticazione e verifica chatbot (ID originale D-007)

- **Decisione storica:** il chatbot usa `FORNITORI_CHAT_TOKEN`; senza token
  configurato le API chat rifiutano le richieste. Il cliente era riconosciuto
  tramite targa con polizza valida, mostrando nome e compagnia; lo stato veniva
  cercato per chat.
- **Motivo dichiarato:** allineamento al foglio `DialoghiChatbot` del docente;
  Telegram non comunica il numero del cliente.
- **Conseguenza/limite:** chi conosce una targa poteva vedere nome e compagnia e
  creare richieste. Il limite di 5 targhe non trovate per sessione era in RAM e si
  azzerava al riavvio.
- **Stato:** applicata nella baseline del record; il limite privacy resta da
  riesaminare e il requisito originale non è disponibile nell'audit corrente.

### H-008 — Traino sulle missioni da chatbot (ID originale D-008)

- **Decisione aggiornata:** il cliente sceglie riparazione o traino con i tasti del bot.
  Per il traino il backend assegna l'officina più vicina in linea d'aria alla posizione,
  convertendo l'indirizzo se serve; la centrale può correggere con
  `PATCH /api/v1/missioni/{id}/intervento` prima della proposta.
- **Motivo:** il cliente in panne sa se serve il traino, ma non quale officina scegliere.
- **Conseguenza:** i dialoghi 2-4 del foglio docente hanno un passo in più. La distanza
  non segue le strade. Senza coordinate né indirizzo valido il traino non viene creato.
- **Stato:** applicata; la centrale corregge l'officina dalla scheda missione.

### H-009 — Notifiche al cliente (ID originale D-009; aggiorna H-006)

- **Decisione:** le missioni create da chat salvano la chat del cliente e gli
  eventi usano il canale d'origine; il bot legge `GET /api/v1/chat/notifiche`,
  invia e registra l'esito. Dopo 5 invii falliti la notifica passa a `FAILED`.
- **Motivo:** Telegram può scrivere solo a chi ha aperto una chat; long polling
  evita la necessità di un indirizzo pubblico.
- **Conseguenza:** le missioni create dalla centrale non inviano messaggi. Consegna almeno una volta: un
  crash dopo l'invio può ripetere il messaggio.
- **Stato:** applicata per Telegram; WhatsApp fuori ambito.

### H-010 — Geocodifica e percorso (ID originale D-010)

- **Decisione:** il backend fa da tramite verso Nominatim e OSRM; la geocodifica
  avviene alla prima richiesta di percorso, non alla creazione della missione.
- **Motivo:** servizi pubblici senza chiavi/costi nel perimetro didattico.
- **Conseguenza:** indirizzi inviati a terzi; provider pubblici senza SLA e con
  limiti; precisione dipendente da OpenStreetMap. Se i provider non rispondono,
  non si inventano km e la chiusura resta possibile.
- **Stato:** implementata, con mappa Leaflet nella UI.

## Aggiornamenti del 04/11/2026

**D-HARD-01:** conservare monolite, layer e API condivise; correzioni mirate e migrazioni additive.
**D-HARD-02:** CSRF rinnovato al login e richiesto anche sulla GET route; pratica chat vincolata al lookup per
15 minuti e impronta per i retry. L'autorizzazione non è una prova di identità.
**D-HARD-03:** notte dalle 22:00 incluse alle 08:00 escluse (prima dalle 20:00; lo storico non viene ricalcolato), Europe/Rome, ora di accettazione, BigDecimal/HALF_UP.
Criterio operativo da convalidare col docente.

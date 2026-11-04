# Registro decisioni consolidato

**Codice:** FOR-DEC-001 · **Versione:** 1.1 · **Data:** 03/11/2026 · **Owner:** M1
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
| D-007 | Identità chatbot | README segnala che la verifica basata sulla targa non prova la titolarità | Limite noto; soluzione/approvazione da definire | `chatbot/README.md` |
| D-008 | Telegram | Bot a long polling, notifiche implementate nel modulo | Dichiarato provato su Telegram dal README/commit; CI non è test provider reale | `chatbot/README.md`, history Git |
| D-009 | WhatsApp | Provider e relativo webhook non integrati | Pianificato | OpenAPI `designed`, guide team |
| D-010 | Frontend | Client HTML/CSS/JavaScript servito da Spring Boot per responsabile e autista | Presente nel codice; browser E2E NOT RUN e flussi incompleti | `src/main/resources/static/`, run CI |
| D-011 | Mappe | `GET /api/v1/maps/route`, Nominatim e OSRM | API implementata e testata in CI; provider live e vista browser NOT RUN | `MapController`, `MapService`, OpenAPI |
| D-012 | Docente | File PDF/Excel non disponibili all'audit | Richiesta di acquisizione aperta | Inventory working tree |

## Decisioni da richiedere

- Conferma dei requisiti obbligatori/facoltativi, dataset e acceptance dal docente.
- Quale timestamp determina tariffa notturna e quale versione delle regole è normativa.
- Acceptance della UI e necessità della mappa visuale, WhatsApp e autenticazione
  più forte del chatbot.
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
- **Stato:** adottata nel modello e nel seed; flusso live implementato in Sprint 2
  e verificato in PostgreSQL CI.

### H-002 — Fascia tariffaria notturna (ID originale D-002)

- **Decisione applicata, da confermare col docente:** usare il timestamp di
  accettazione della missione; fascia 20:00–08:00, estremi inclusi, moltiplicatore 1,25.
- **Fonte dichiarata nel record storico:** il foglio Excel precisa l'ora di
  accettazione; il PDF definisce la fascia ma non il timestamp da usare.
- **Stato:** implementata in Sprint 2; conferma di business ancora aperta.

### H-003 — Rounding (ID originale D-003)

- **Decisione:** memorizzare costi con `NUMERIC(10,2)` e importi Java con
  `BigDecimal`; non arrotondare chilometri prima del calcolo.
- **Rounding:** arrotondamento finale `HALF_UP` a due decimali, verificato con i
  casi tariffari diurni/notturni.
- **Stato:** adottata e testata a livello unitario.

### H-004 — Autenticazione (ID originale D-004)

- **Decisione:** sessione server-side, JSON per login, cookie sessione HttpOnly,
  BCrypt e CSRF in sessione.
- **Stato:** implementata; credenziali demo in chiaro non vengono riportate nei
  file versionati.

### H-005 — Versione Java nel POM (ID originale D-005)

- **Decisione:** Java 25 LTS; `java.version` e `maven.compiler.release` sono 25.
  Maven Wrapper resta 3.9.9.
- **Motivo:** target condiviso e verifica riuscita con Temurin 25.0.5. Spring Boot
  4.1.2 è mantenuto senza cambio major.
- **Stato:** target compilato e `clean verify` superato su Java 25 il 03/11/2026.

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
- **Stato registrato:** API applicata; mancava il comando nella UI della centrale
  al momento del record. La baseline successiva ha frontend, ma alcuni flussi
  restano incompleti.

### H-009 — Notifiche al cliente (ID originale D-009; aggiorna H-006)

- **Decisione:** le missioni create da chat salvano la chat del cliente e gli
  eventi usano il canale d'origine; il bot legge `GET /api/v1/chat/notifiche`,
  invia e registra l'esito. Dopo 5 invii falliti la notifica passa a `FAILED`.
- **Motivo:** Telegram può scrivere solo a chi ha aperto una chat; long polling
  evita la necessità di un indirizzo pubblico.
- **Conseguenza:** le missioni create dalla centrale restano `internal` e non
  inviano messaggi. Consegna almeno una volta: un crash dopo invio può ripetere
  il messaggio.
- **Stato registrato:** applicata per Telegram; WhatsApp da collegare.

### H-010 — Geocodifica e percorso (ID originale D-010)

- **Decisione:** il backend fa da tramite verso Nominatim e OSRM; la geocodifica
  avviene alla prima richiesta di percorso, non alla creazione della missione.
- **Motivo:** servizi pubblici senza chiavi/costi nel perimetro didattico.
- **Conseguenza:** indirizzi inviati a terzi; provider pubblici senza SLA e con
  limiti; precisione dipendente da OpenStreetMap. Se i provider non rispondono,
  non si inventano km e la chiusura resta possibile.
- **Stato aggiornato:** API backend implementata e testata in CI; provider live
  e visualizzazione mappa browser non verificati.

## Aggiornamento hardening e prove del 04/11/2026

**D-HARD-01 è implementata, autorizzazione nella richiesta 04/11:** conservare
monolite/layer e API condivise; correzioni mirate, migrazione 07 additiva e niente
nuove infrastrutture. Commit/push solo feature/backend; PR senza merge finale.
**D-HARD-02 è implementata, consumer da revisionare:** CSRF rinnovato al login e
richiesto anche su GET route con effetti legacy; pratica chat vincolata a lookup
15 minuti e fingerprint retry. Il grant non è una prova di identità.
**D-HARD-03 è criterio operativo esplicitato dal committente, fonte docente da
convalidare:** notte20:00 inclusa–08:00 esclusa, Europe/Rome, ora di accettazione,
BigDecimal/HALF_UP. Supera l'ambiguità testuale storica H002 per questa revisione,
non costituisce approvazione del docente sul PDF/Excel assente.
**D-HARD-04 è gate aperto:** pubblicare Draft PR per titolarità chatbot/outbox e
configurazione produzione non risolte. Non dichiarare il progetto completato
soltanto sulla base dei test verdi.

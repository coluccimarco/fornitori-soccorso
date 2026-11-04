# Audit iniziale backend — FORNITORI

**Fase:** 1 — Git e analisi del progetto
**Data:** 04/11/2026, Europe/Rome
**Responsabile backend:** Minervini Vincenzo (assegnazione comunicata dal committente)
**Branch:** `feature/backend`
**Baseline analizzata / SHA di main integrato:** `5a16da8533e755cebdec605216633abccf7add1f`
**Stack mantenuto:** Java 25 LTS, Spring Boot 4.1.2, Maven Wrapper 3.9.9, PostgreSQL 18.

## 1. Esito e perimetro

Il branch backend è sincronizzato con `origin/main` tramite fast-forward. Il backend
compila con Java 25 e passa **60 test Java, senza errori, failure o skip**, con
PostgreSQL 18.6 temporaneo. Passano anche **64 test Python**. Non emerge un blocco
di compilazione o di inizializzazione dello schema sulla baseline esaminata.

Il ciclo missione principale è presente, ma questi risultati non certificano
completezza funzionale, sicurezza per dati reali o compatibilità browser completa.
Le criticità principali riguardano identità del richiedente chatbot, contratti
di validazione, scadenze nel client, affidabilità delle notifiche e documentazione
che attribuisce ai test verifiche più ampie di quelle effettivamente codificate.

Sono stati esaminati tutti i Markdown nelle otto directory richieste, il workbook
`docs/quality/registri.xlsx` (tutti gli otto fogli), i 13 sorgenti Mermaid,
`api/openapi.yaml`, i sei script SQL, README, 57 sorgenti Java applicativi,
10 file di test Java, frontend statico, modulo Python e quattro suite Python,
POM, proprietà applicative, wrapper, `.gitignore` e workflow CI. Per i diagrammi
sono stati controllati anche XML SVG e presenza/intestazioni/dimensioni PNG;
non si dichiara una revisione grafica o di impaginazione. I DOCX derivati sono
stati validati strutturalmente dal tool esistente, senza rigenerarli.

Le etichette IMPLEMENTATO nei documenti sono state confrontate con controller,
servizi, repository, schema e test. Le fonti didattiche PDF/Excel originali non
risultano nel checkout: il workbook dei registri non è il dataset originale.
BR, UC e NFR rimangono una baseline locale da convalidare col docente. Le priorità
di questo audit sono proposte tecniche, non approvazioni di nuovi requisiti.

## 2. Git, checkpoint e conservazione del lavoro

Eseguiti `git status`, `git branch -avv`, `git remote -v`,
`git fetch origin --prune`, `git log -5 --oneline` e controlli di ancestry/diff.

| Elemento | Evidenza / risultato |
|---|---|
| Remote origin | `https://github.com/coluccimarco/fornitori-soccorso.git` |
| Branch iniziale | `documentazione`, SHA `d4543e7bb53c670c996ed4478c43baeeb7708b11` |
| Modifiche iniziali | Solo `src/main/resources/application.properties`, riga password datasource; index senza modifiche staged |
| File non tracciati iniziali | Nessuno secondo `git ls-files --others --exclude-standard`; file ignorati non rimossi |
| Backend prima della sincronizzazione | Branch esistente `feature/backend`, SHA `d4c9dead3d6833ff3b64055c6101677fd01e1a68` |
| Main remoto dopo fetch | `5a16da8533e755cebdec605216633abccf7add1f` |
| Integrazione | `git merge-base --is-ancestor feature/backend origin/main` riuscito; `git switch feature/backend`; `git merge --ff-only origin/main` riuscito |
| Conflitti / nuovi commit | Nessun conflitto; nessun nuovo commit, neppure di merge |
| Branch finale | `feature/backend`, HEAD uguale a `origin/main`, avanti di 19 commit rispetto a `origin/feature/backend` |
| Main locale | Rimasto a `25c613b1b4a958c25ee2fcc99535eca912e05300` |
| Pubblicazione | Nessun push, Pull Request o merge sul branch `main` |

La modifica locale conteneva un valore sensibile: non è stata inserita in stash,
commit, patch in chiaro o audit. Prima di ripristinare il file dalla baseline è
stato creato e verificato un checkpoint **cifrato Windows DPAPI CurrentUser fuori
dal repository**, con manifest di branch, SHA e hash del file:

```text
%TEMP%\fornitori-backend-checkpoint-2eef6aa0ec274d00b7916e9aacc60fa3\
  application.properties.dpapi
  manifest.json
  RECOVERY.txt
  Recover-Configuration.ps1
```

Il recupero richiede lo stesso utente Windows, una sessione con accesso DPAPI e
una destinazione nuova esplicita. Il file locale sensibile resta nel checkpoint;
nel checkout rimane la configurazione di `origin/main`, basata su environment.
Il backup in `%TEMP%` va conservato prima delle pulizie dei file temporanei.
Non occorre recuperare nulla per i file non tracciati, perché inizialmente assenti.
Le prime limitazioni sandbox su `.git` e DPAPI sono state superate tramite accesso
approvato; la verifica del checkpoint è avvenuta prima del ripristino del file.

Il confronto finale dei file applicativi e dei componenti condivisi con
`origin/main` deve rimanere vuoto. Il solo nuovo deliverable della fase è questo
audit, lasciato non committato. Frontend, chatbot, documentazione preesistente,
SQL, test e configurazioni non sono stati corretti durante l'analisi.

## 3. Verifiche realmente eseguite

| Verifica sulla baseline | Esito e limiti |
|---|---|
| JDK | Temurin `25.0.5+8` da `C:\Users\utente\.jdks\temurin-25.0.5+8`; il Java predefinito era 17, senza cambiare POM o impostazioni globali |
| Maven `clean verify`, senza datasource | BUILD SUCCESS: 60 contabilizzati, 31 eseguiti, 29 skip; questo primo risultato non prova il DB |
| Schema/seed PostgreSQL isolato | PASS dei sei script in ordine, con `ON_ERROR_STOP=1`; PostgreSQL 18.6, database nuovo `fornitori_audit`, loopback porta 55437 |
| Baseline SQL | 10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti, 6 officine, 30 missioni fixture; 16 eseguite, 6 pending, 4 scadute, 4 rifiutate, EUR 1.509,71; assertion totali per autista PASS |
| Maven `verify`, con datasource isolato | BUILD SUCCESS: **60 eseguiti, 0 failure, 0 error, 0 skip** |
| Python, `python -m unittest discover` in `chatbot/` | **64 PASS**, Python 3.14.6; server locali/fake, nessun messaggio inviato a Telegram |
| JavaScript | `node --check src/main/resources/static/app.js`: PASS sintattico |
| Countdown JS | Harness Node sulla funzione originale: 6 richieste di refresh da una sola invocazione, arresto artificiale alla sesta; riproduce il difetto F-04 senza browser |
| Documentazione | `python tools/validate_docs.py`: PASS strutturale; 52 Markdown incluso questo audit (51 nella baseline), 34 operation OpenAPI, 9 DOCX, 8 fogli, 13 sorgenti/26 rendering; non valida la veridicità dei contenuti |
| Browser E2E / provider live | NOT RUN: nessuna dichiarazione di collaudo UI, Telegram, WhatsApp, Nominatim o OSRM reali |
| CI remota | Run `41656606659` su `f763d53` citato dai documenti storici; non interrogato né rieseguito in questa fase. Non attribuito allo SHA corrente |

Le 60 prove Java si distribuiscono così: `ApiSecurityTests` 14,
`ChatApiSecurityTests` 6, `ChatPostgresIntegrationTests` 13, `ChatSupportTests` 2,
`FornitoriApplicationTests` 1, `MapPostgresIntegrationTests` 12,
`MapProviderClientsTests` 7, `MissionPostgresIntegrationTests` 3,
`MissionServiceTests` 1, `PricingServiceTests` 1. Alcuni metodi contengono più
assertion: conteggio dei test e copertura dei requisiti sono concetti distinti.

Il database operativo e le sue credenziali non sono stati usati. L'istanza
effimera aveva credenziale casuale, autenticazione SCRAM e ascolto solo locale;
il file temporaneo della password è stato rimosso e il server arrestato.
Due tentativi preliminari dell'harness Windows hanno richiesto correzioni
all'attesa di avvio e alla gestione dei NOTICE di psql, senza modificare il
progetto. Il tentativo conclusivo è quello riportato come PASS.

Report JUnit: `target/surefire-reports/`. Log e riepilogo locale:
`%TEMP%\fornitori-backend-audit-20261104\` (`maven-postgres.log`, `schema.log`,
`test-summary.json`, harness `run-isolated-postgres.ps1`). Non aggiungere log o
cluster di test al repository. Il cluster residuo è fermo; l'harness di bootstrap
richiede una directory dati nuova a ogni riesecuzione.

Comandi Maven usati, con JDK 25 selezionato per la sola shell:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dmaven.repo.local=C:\Users\utente\.m2\repository' clean verify
# Seconda esecuzione: datasource del database isolato in environment
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dmaven.repo.local=C:\Users\utente\.m2\repository' verify
```

## 4. Matrice requisito → implementazione → prova

Nomi delle classi senza suffisso `.java`; repository abbreviati con `Repo` solo
per leggibilità (corrispondono alle classi `*Repository`). **Presente** significa
codice individuato, **parziale** indica una lacuna concreta, **non implementato**
indica assenza del percorso runtime. PASS si riferisce alle prove sopra, non
all'acceptance del requisito. Il frontend browser non è stato collaudato E2E.

| Requisito | Frontend / consumer | Controller | Service | Repository | Database | Test | Stato |
|---|---|---|---|---|---|---|---|
| BR-001; UC-002/003 — anagrafiche e cliente | `app.js`: registri, ricerca, form cliente | `RegistryController` | `RegistryService` | Customer, Driver, Vehicle, InsuranceCompany, Workshop Repo | `customers`, `drivers`, `vehicles`, `insurance_companies`, `workshops` | `ApiSecurityTests` usa service mock; nessuna suite reale creazione/ricerca cliente | Presente; validazione/UI incompatibili F-03 |
| BR-002; UC-001 — accesso e ruoli | login/logout/me/csrf same-origin | `AuthController`, `SecurityConfig` | `AuthService`, `DatabaseUserDetailsService` | AppUser, Driver Repo | `app_users`, `drivers` | `ApiSecurityTests` PASS; autenticazione mock, manca login completo DB/browser | Presente; hardening e casi negativi mancanti F-09 |
| BR-003; UC-004 — creazione/dettaglio live | form missione, filtro live/reference | `MissionController` | `MissionService.create/get/list` | Mission, Customer, Workshop, MissionEvent Repo | `missions`, `mission_events`, FK, `is_fixture` | `MissionPostgresIntegrationTests` PASS | Presente; nessuna idempotenza centrale su retry ambiguo |
| BR-004; UC-005 — proposta/storico | select autista e POST proposte; storico tentativi non esposto | `MissionController.propose` | `MissionService.propose` | Mission, Driver, MissionProposal, PricingConfig Repo | `mission_proposals`, indici unici parziali | test concorrenza due missioni/un autista e riproposizione PASS | Presente backend; storico persistito, senza API di lettura dedicata |
| BR-005; UC-006 — risposta/scadenza | accept/reject, countdown | `MissionController` | `MissionService.accept/reject/expireDueProposals`, scheduler | Mission, MissionProposal, Driver Repo | deadline, lock pessimisti, `uq_driver_active_live` | `MissionServiceTests`, `MissionPostgresIntegrationTests` PASS | Parziale: reject oltre deadline non controllato F-05; refresh UI F-04 |
| BR-006; UC-007/008 — completamento/prezzo | form km e riepilogo | `MissionController.complete` | `MissionService`, `PricingService` | Mission, MissionProposal, PricingConfig, Event, Outbox Repo | km/costi `NUMERIC(10,2)`, date e outbox | pricing e flusso missione PASS | Presente; regola 08:00 da riconciliare F-06, precisione/limiti F-03 |
| BR-007; UC-008 — rettifica motivata | Nessun comando web `/cost` | `MissionController.adjustCost` | `MissionService.adjustCost` | Mission, MissionEvent Repo | costo calcolato/finale, motivo, attore, evento | rettifica in `MissionPostgresIntegrationTests` PASS | Backend presente, flusso UI incompleto F-07 |
| BR-008; UC-009 — dashboard/storico autista | vista missioni con aggregati; storico personale | `MissionController` | `dashboard`, `driverMissions`, `driverHistory` | Mission, Driver Repo | fixture/live e costi finali | happy path aggregati e permessi campione PASS | Presente; no paginazione, test isolamento completi mancanti |
| BR-009; UC-004/010 — richiesta e stato chat | Python `ChatEngine` + `HttpBackend`, Telegram/console | `ChatController` | `ChatService`, limiter, `MissionService.createFromChat` | Customer, InsuranceCompany, ChatMissionRequest, Mission, Driver Repo | `chat_mission_requests` da 05/06; `chat_sessions` non usata dal runtime | 13 integrazione chat, 6 security, 2 limiter e suite Python PASS | Presente; identità e idempotenza incomplete F-01/F-02 |
| BR-010; UC-011 — notifiche cliente | `NotificationDispatcher`, `TelegramApi`; nessuna UI outbox | `ChatController.notifications/reportNotification` | `MissionService.addOutboxEvent`, `ChatService` | NotificationOutbox, ChatMissionRequest Repo | `notifications_outbox`, event key unica | integrazione outbox e `test_notifications.py` PASS | Parziale: Telegram per missioni chat; nessun dispatch internal; concorrenza/recovery F-08 |
| BR-011; UC-012 — route/km stimati | `app.js` non invoca route, nessuna mappa | `MapController` | `MapService`, Nominatim/OSRM, salvataggi MissionService | Mission, Driver, Vehicle, Workshop, AppUser Repo | coordinate origine e km stimati | 12 integrazione mappe + 7 client HTTP locale PASS | API presente; UI assente, provider reali NOT RUN; F-10 |
| BR-012 — WhatsApp | Nessun adapter WhatsApp; enum non è integrazione | Webhook solo OpenAPI `designed` | Nessun servizio canale | Infrastruttura chat/outbox condivisa, nessun adapter dedicato | canale ammesso dallo schema | Nessun test provider/webhook WhatsApp | Non implementato; scope da confermare |
| BR-013 — UI centrale/autista | HTML/CSS/JS presenti | Auth, Registry, Mission; Map non consumato | Servizi sopra | Repository sopra | Tabelle sopra | asset pubblici MVC e sintassi JS PASS; browser NOT RUN | Parziale: F-03/F-04/F-07 e disponibilità driver obsoleta |
| NFR-001 — credenziali fuori dai DTO | Solo `CurrentUser`/DTO, mai entity nel JS | Controller restituiscono DTO | mapping espliciti | hash letto solo da UserDetailsService | hash BCrypt seed | assertion password assente su login mock | Controllo presente; non è una verifica esaustiva di tutti i payload/log |
| NFR-002 — autorizzazione/CSRF | header dal CSRF endpoint | due security chain; auth, missioni e chat | ruolo/ownership missione, chat a token | utenti/proposte | identità utente/driver | campione MVC PASS; assenza CSRF operatore non testata esplicitamente | Presente con lacune di prova e policy F-09/F-10 |
| NFR-003 — PostgreSQL/tipi | Input numerici/data | DTO validati parzialmente | transazioni JPA | repository PostgreSQL | sei script, Hibernate validate | schema/seed/assert + 29 test DB/context PASS | Presente; check NULL e migrazioni F-11 |
| NFR-004 — assegnazioni concorrenti | conflitto HTTP gestito genericamente | `MissionController` | lock missione poi driver | lock pessimisti | unicità driver live e proposta | due proposte concorrenti PASS | Presente nel caso testato; altre gare da aggiungere |
| NFR-005 — bot solo via API | `HttpBackend`, nessun driver SQL | `ChatController` | `ChatService` | solo backend accede al DB | persistita dal backend | HTTP stub Python e integrazione Java PASS | Riscontrato; Python → Spring reale non testato insieme |
| NFR-006 — segreti/errori integrazione | messaggi fallback, token da environment | error handler + token filter | timeout provider, errori espliciti | outbox errori persistiti | hash/metadata, non token configurazione | test Python token non esposto e errori provider PASS | Parziale: configurazione locale iniziale messa in sicurezza; hardening F-09 |

## 5. Rilievi e riproducibilità

### F-01 — P0 prima di dati reali: identità chatbot non verificata

Evidenza: [ChatService.java](../../src/main/java/it/exprivia/fornitori/service/ChatService.java),
metodi `lookup` e `createMission`, e
[ChatLookupLimiter.java](../../src/main/java/it/exprivia/fornitori/service/ChatLookupLimiter.java).
La lookup restituisce nome, compagnia e ID a chi conosce una targa con polizza
valida. La creazione blocca il cliente per ID, ma non verifica che `sessionId`
abbia effettuato una lookup valida, né ricontrolla la polizza. I test di creazione
chat passano anche senza lookup preliminare: il commento di `createFromChat` che
parla di identità già verificata è più forte del controllo implementato.

Le API richiedono comunque il token di servizio: **non sono pubblicamente
anonime**. Il rischio verso il cliente Telegram è la sola conoscenza della targa;
quello tra servizio e backend è la fiducia completa in ID/sessione/destinatario
forniti dal consumer. Il limiter conta solo lookup fallite, è in RAM e la chiave
è scelta dal consumer. Non limita lookup valide o creazioni ripetute.
Definire con M3 una prova di possesso/identità e un legame verificabile tra cliente,
canale e conversazione; aggiungere i test di bypass prima di usare dati reali.

### F-02 — P1: idempotenza chat non vincolata a sessione e payload

In `ChatService.createMission`, su `requestId` esistente viene confrontato soltanto
`clienteId`. Lo stesso ID con cliente uguale ma chat, canale o payload differenti
restituisce la missione preesistente. Non viene confrontato `sessionId` né un hash
del payload; la risposta non chiarisce una richiesta modificata. Il lock cliente
serializza il caso stesso cliente, ma non è una prova di isolamento fra chat.
Definire lo scope della chiave e restituire conflitto per riuso incompatibile.
Il test esistente copre retry identico e cliente diverso, non questi casi.

### F-03 — P1: validazione UI, DTO e DB non allineata

[index.html](../../src/main/resources/static/index.html), righe 392–393, accetta
nome cliente fino a 200 caratteri e telefono fino a 40; lo
[schema](../../database/01_schema.sql), righe 34–35, consente 150 e 32.
[RegistryDtos](../../src/main/java/it/exprivia/fornitori/dto/RegistryDtos.java)
usa `NotBlank`, senza `Size`: il server inoltra input ammessi dal form al DB,
anziché rifiutarli con validazione di campo. La gestione globale dei conflitti
non distingue vincoli/overflow/duplicati; verificare l'esatto HTTP nel test da aggiungere.

Analogamente, `kmEffettivi` e `costoFinale` non hanno `Digits`/massimo coerente
con `NUMERIC(10,2)`. Il client ammette 99.999.999 km, ma il costo risultante supera
la capacità della colonna anche con la tariffa diurna seed. Input con più di due
decimali possono essere usati per il prezzo e poi arrotondati dal DB nei km,
perdendo la precisione usata nel calcolo (H-003 dichiara di non arrotondare i km).
Decidere precisione e limiti condivisi, applicarli a DTO/OpenAPI/UI e provarli
contro PostgreSQL. Nessuna modifica dei dati storici è necessaria per l'audit.

### F-04 — P1: refresh ricorsivo della UI alla scadenza

[app.js](../../src/main/resources/static/app.js), `updateCountdowns` (riga 576),
chiama `loadDriverMissions`/`loadMissions` quando la deadline è passata. I loader
ricreano gli elementi DOM e richiamano subito `updateCountdowns`; il flag
`dataset.refreshed` viene perso. Se il server restituisce ancora `proposta`, si
genera un ciclo di richieste fino al passaggio dello scheduler (default 30 secondi).
Nella vista reference le fixture proposte hanno date storiche e non vengono
fatte scadere dallo scheduler: il ciclo può persistere finché la vista resta aperta.

Un harness Node che estrae la funzione originale e simula il nuovo DOM a ogni
risposta ha prodotto sei refresh senza tick del timer, fermati artificialmente.
È una prova della logica, non un browser E2E. Conservare lo stato del refresh per
missione fuori dal DOM, evitare auto-refresh operativo delle fixture e limitare
frequenza/richieste contemporanee. Servono test live e reference con rete simulata.

### F-05 — P1: rifiuto oltre deadline dipendente dallo scheduler

[MissionService](../../src/main/java/it/exprivia/fornitori/service/MissionService.java),
`accept` controlla `now >= scadenza`, mentre `reject` (riga 246) controlla solo
stato e autista. Un reject dopo deadline, prima del job, persiste `non_accettata`;
dopo il job viene respinto perché lo stato è già `scaduta`. L'esito storico dipende
dall'ordine di esecuzione e non solo dal tempo della risposta. Allineare la policy
di deadline per i due comandi e testare deadline esatta e gara reject/expiry.

### F-06 — P1: decisione tariffaria contraddetta da codice e test

[DECISION_LOG H-002](../management/DECISION_LOG.md) dichiara 20:00–08:00 con
estremi inclusi. [PricingService](../../src/main/java/it/exprivia/fornitori/service/PricingService.java),
righe 33–34, implementa **20:00 inclusa, 08:00 esclusa**;
`PricingServiceTests` verifica esplicitamente 08:00 come diurno. Il runtime è
coerente col test, non con il record decisionale. Non cambiare la tariffa senza
conferma docente; fissare una regola unica e aggiungere estate/inverno, cambio
ora, arrotondamenti e configurazione assente. Il riferimento temporale corrente
è l'accettazione, non la creazione né la chiusura.

### F-07 — P1: percorsi operativi frontend incompleti

La UI non chiama `/cost`, `/intervento` o `/maps/route`. Il chatbot crea sempre
`riparazione_sul_posto`: l'operatore browser non può convertirla in traino con
officina usando l'interfaccia attuale. `MissionResponse` contiene gli ID di
cliente/officina, ma non telefono cliente e indirizzo officina; l'autista non
può leggere le anagrafiche centrali. Il client segnala correttamente l'assenza,
ma il flusso di soccorso resta incompleto. Concordare un DTO operativo minimo,
autorizzato sulla missione, senza aprire tutte le anagrafiche agli autisti.

La lista disponibilità driver viene caricata nelle anagrafiche e non ricaricata
dopo `propose`: la UI può proporre un driver ormai occupato (il backend risponde
409) o mantenerlo disabilitato dopo che si è liberato. La sicurezza server-side
resta efficace. M1/M2 devono concordare refresh e test di integrazione.

### F-08 — P1: outbox persistente, consegna non esclusiva

[NotificationOutboxRepository](../../src/main/java/it/exprivia/fornitori/repository/NotificationOutboxRepository.java)
legge i primi 20 `PENDING` senza claim/lease. Il lock è applicato alla registrazione
dell'esito, dopo l'invio. Due dispatcher possono quindi inviare lo stesso evento;
un crash dopo invio perde `_sent_unreported`, conservato solo in RAM in
[notifications.py](../../chatbot/fornitori_chatbot/notifications.py).
La event key unica impedisce duplicati in accodamento, non in consegna.

Retry a 30 secondi nel processo e stop dopo cinque esiti falliti sono presenti;
mancano prenotazione persistente, `next_attempt_at`, gestione `retry_after` del
provider e recupero amministrativo di FAILED. Venti elementi in attesa del
retry possono occupare tutta la prima pagina e ritardare le notifiche successive.
Progettare con M5 una consegna almeno una volta osservabile e idempotente dove
possibile; non promettere exactly-once. Gli eventi `internal` delle missioni
centrali non hanno dispatcher implementato.

### F-09 — P1 prima di esposizione esterna: hardening e prove sicurezza

BCrypt, sessione, cambio session ID, CSRF operatore, token service comparato con
`MessageDigest.isEqual`, DTO e ownership sono presenti. Non risultano rate limit
login, policy di blocco account, rotazione/scoping dei token, profilo di rilascio
che imponga HTTPS/cookie Secure (`SESSION_COOKIE_SECURE` default false) o CSP.
Bootstrap è caricato da CDN senza attributo integrity: dipendenza di runtime
da considerare anche nella demo offline.

`ApiSecurityTests` usa AuthenticationManager e servizi mock e non contiene una
prova esplicita di mutazione operatore senza CSRF, né login BCrypt reale su DB,
rotazione del CSRF al login, cookie su server reale o copertura di tutti i ruoli
su tutti i comandi. Non è corretto dedurre questi PASS dalla sola suite verde.
Definire test negativi e configurazione ambiente, senza sostituire l'auth a sessione
o introdurre token nel frontend.

### F-10 — P1/P2: contratto mappe, autorizzazione e stime

[MapService](../../src/main/java/it/exprivia/fornitori/service/MapService.java)
evita correttamente transazioni aperte durante le chiamate esterne. Tuttavia:

- `GET /maps/route` persiste coordinate e km: è una mutazione sotto una GET senza
  CSRF. Separare la consultazione dal salvataggio o formalizzare una diversa API.
- L'accesso usa `MissionService.get`, che ammette anche autisti con proposte
  storiche; `resolveDriver` non richiede l'assegnazione corrente. La guida mappe
  parla invece di autista assegnato. La consultazione storica è esplicita nel
  contratto missioni/note: serve una decisione dedicata per mappe e dati futuri,
  non una rimozione indiscriminata dell'accesso storico.
- Km stimati non associati alla versione di itinerario/autista: una stima per
  driver B può restare dopo assegnazione ad A; `changeIntervention` e `propose`
  non la invalidano. Una chiamata lenta può salvare una stima per il tipo/officina
  precedente. Non influisce sul prezzo, che usa km effettivi, ma sul suggerimento.
- Provider response non valida range coordinate/distanza positiva/geometria
  completa; rate limit Nominatim solo per JVM. Timeout presenti, nessun test live.

### F-11 — P1/P2: integrità e gestione schema

Lo schema protegge FK, stati e unicità live. `ck_mission_coordinate` ha però un
ramo con `BETWEEN` su colonne nullable: con una coordinata NULL e l'altra valida
l'espressione diventa UNKNOWN, che un CHECK non rifiuta. Il service di creazione
controlla la coppia, ma il vincolo DB da solo non garantisce quanto dichiarato.
Correggere con una migrazione esplicita e test SQL delle quattro combinazioni.

Non è presente un registro migrazioni Flyway/Liquibase o equivalente: `IF NOT
EXISTS` non evolve una tabella già esistente. Le guide M1 e DATABASE_DESIGN
riportano un setup 01–04, insufficiente per l'entità `chat_mission_requests`;
README e manuale tecnico includono correttamente 05/06. Manca un percorso testato
di upgrade da schema precedente. `chat_sessions` è creata ma non mappata/usata dal
runtime; `chat_mission_requests` manca dal dizionario/diagramma ER canonico.

### F-12 — P1: OpenAPI dichiarativa, non verificata contro runtime

Le 30 operazioni `implemented` corrispondono alle rotte dei cinque controller;
le quattro `designed` sono due GET utenti e due webhook. Disallineamenti concreti:

- `serviceBearer.bearerFormat: JWT`, ma il filtro confronta un token opaco statico;
- `ChatMissionRequest` eredita `canaleOrigine=centrale` da CreateMission, mentre
  ChatService ammette solo telegram/whatsapp; eredita anche `kmStimati`, ignorato
  dal DTO chat;
- `sessionId`/`requestId` hanno massimo 64 in Java, omesso nello YAML; limiti testo
  e numerici incompleti;
- `Dashboard` non descrive `riferimentoAl` e `fonte`, restituiti dal DTO;
- risposte 200 di reject/cost senza schema del corpo; errori non descritti con
  uno schema comune, nonostante il runtime restituisca `status/error`.

CI e `validate_docs.py` fanno parsing YAML e controllano metadati status/role:
non confrontano request/response con DTO, controller o consumer. Servono test
contrattuali su payload/errori reali e una matrice dei permessi.

### F-13 — P1: documentazione e workbook non completamente riconciliati

- BUR executive summary/attori e apertura FUD dichiarano assenti UI e routing;
  le sezioni successive e TAD ne dichiarano correttamente la presenza. Anche
  commenti di tre diagrammi missione negano la UI esistente.
- FUD UC-003 cita test Registry/duplicati non presenti come suite reale;
  BR-002/NFR-002 non vanno letti come prova di ogni scenario CSRF/ruolo.
- Il foglio **Requisiti** di `registri.xlsx` contiene intestazione
  `Attore / Esigenza / Stato` e sette stakeholder, nessun BR/NFR.
  [build_registers.py](../../tools/build_registers.py) seleziona la prima tabella
  del BUR, non quella dei requisiti. La verifica esistente controlla i nomi dei
  fogli e non rileva il difetto. Il foglio Tracciabilita contiene invece i 13 BR.
- Conteggi ed esiti test del generatore Excel sono stringhe fisse, non estratti
  dai report JUnit. README cita 11 test MVC; la suite corrente ne esegue 14.
- La guida M5 cita D-009 per notifiche Telegram, ma D-009 corrente è WhatsApp;
  il comportamento notifiche è in H-009. `chatbot/README.md` cita il vecchio
  `docs/DECISION_LOG.md` e contiene il commento «chat_id non esce dal bot»,
  mentre il destinatario è trasmesso al backend e persistito.

Preservare gli snapshot Sprint datati come storico; correggere le fonti canoniche
e i generatori nella fase successiva. Non trattare la presenza di un workbook o
un validatore verde come prova della completezza dei requisiti.

### F-14 — P2: debito architetturale e operatività

`MissionService` concentra workflow, letture, reporting, audit, mappe e outbox con
13 dipendenze di costruttore. Stati/canali sono stringhe replicate tra servizi,
SQL, JS e Python. I layer esistono e i controller sono sottili: è opportuno un
refactoring incrementale dopo i test, non una riscrittura.

Liste e storico senza paginazione; dashboard carica tutte le missioni e risolve
i driver in ciclo (accessi ripetuti, in parte mitigati dalla persistence context).
Scheduler tratta tutte le scadenze in una transazione; limiter fa cleanup
dell'intera mappa a ogni verifica. Mancano prove di carico e indici specifici per
poll outbox, lookup stato chat e ordinamento scadenze da valutare su EXPLAIN.
La ricerca solo punteggiatura può normalizzare la targa a vuoto e produrre `LIKE
'%%'`, restituendo tutti i clienti già autorizzati alla centrale.

Mancano metriche/health applicativi dedicati, correlation ID uniforme, gestione
backup/restore e retention testata. Il log errore generico non sostituisce una
diagnostica operativa. Conversazioni, offset Telegram e deduplica messaggi sono
in RAM; il polling avanza l'offset prima della lavorazione e non rende durabili
le risposte fallite. Il lock globale del ChatEngine serializza anche chiamate
HTTP di conversazioni diverse. Le integrazioni chat usano Clock reale per la
validità delle polizze seed: i test possono cambiare esito col calendario.

## 6. Test mancanti o insufficienti

| Area | Prove da aggiungere nella fase successiva |
|---|---|
| Anagrafiche | Creazione cliente contro DB, duplicato normalizzato anche concorrente, compagnia inesistente, limiti nome/telefono, ricerca targa/nome/punteggiatura e niente hash nei payload |
| Sicurezza operatore | Login reale BCrypt/sessione/CSRF, mutazioni senza token, ruolo errato su ogni comando, isolamento storico/note, cookie e logout dal browser |
| Chat | Create senza lookup, cliente/sessione/canale/destinatario non coerenti, polizza scaduta alla creazione, chiave idempotenza con payload diverso e concorrente, limiti distribuiti |
| Missioni | Reject alla/dopo deadline, accept/expiry concorrenti, doppio accept/complete, stessa missione proposta a driver diversi, persistenza effettiva dell'expiry dopo 409 e rollback outbox |
| Prezzi | Regola 08:00 approvata, ora legale/solare, input negativi/null/overflow/precisione, configurazione mancante e rounding |
| Mappe | Vecchio assegnatario, stima obsoleta dopo cambio driver/intervento, gara routing/modifica, payload provider malformato/null/range, timeout e limiti concorrenti |
| Outbox | Due dispatcher, crash dopo invio prima di ACK, 429/retry_after, saturazione prima pagina, ripartenza/FAILED e ordine eventi |
| Contratti | Validazione request/response/errori contro OpenAPI; test Python → Spring → PostgreSQL, senza provider reali |
| Browser | Login → cliente → missione → proposta → risposta/chiusura → storico; reference/live, countdown scaduto senza loop, refresh disponibilità, rete lenta/sessione scaduta, accessibilità |
| Schema/documenti/CI | Upgrade 01–04 → 05/06, riesecuzione script, check coordinate NULL, fixture immutate nei valori oltre al conteggio; BR/NFR presenti nel foglio giusto; Python e contract/browser nel workflow |

Non sono stati aggiunti test o correzioni applicative in questa fase. La suite
Java esistente verde resta evidenza utile, ma molti test di integrazione invocano
direttamente i servizi e usano rollback del test: non provano da soli l'intero
percorso HTTP/browser né ogni confine di commit.

## 7. Dipendenze e coordinamento

| Owner | Attività condivisa | Dipendenza / risultato atteso |
|---|---|---|
| Minervini Vincenzo — M1 | Contratti, validazione, workflow, schema, sicurezza backend e piano test | Conservare Java 25/PostgreSQL 18; proporre cambi piccoli e verificabili |
| M2 frontend (nome da confermare) + M1 | F-03/F-04/F-07, DTO operativo, refresh driver, errori | Contratto concordato prima di cambiare payload; test browser su DB isolato |
| M3 chatbot (nome da confermare) + M1 | F-01/F-02, identità/sessione/idempotenza e riavvio | Definire cosa prova la lookup; nessun accesso diretto al DB dal bot |
| M4 mappe (nome da confermare) + M1/M2 | F-10, salvataggi, ownership, invalidazione stime e UI | Scope mappa e provider confermato; distinguere sempre km stimati/effettivi |
| M5 notifiche (nome da confermare) + M1/M3 | F-08, claim/retry/recovery e canali | Contratto outbox/provider; nessuna consegna reale implicita nei test |
| Docente/team/documentazione | Fonti BUR/FUD, policy notturna, scope WhatsApp/mappa, F-13 | Specifica e acceptance verificabili; niente verbali o approvazioni ricostruiti |

La responsabilità backend nominale deriva dalla richiesta di questa fase. Le
guide esistenti indicano soltanto M1–M5; non si inventano nomi o assegnazioni degli
altri membri. Nessun messaggio esterno o ticket è stato creato.

## 8. Piano di correzioni P0/P1/P2 e ingresso nella fase 2

**P0 — prima di esporre il chatbot a dati reali.** Minervini Vincenzo con M3:
chiudere F-01 definendo identità e binding cliente/conversazione, aggiungere test
di bypass e limitare abusi di lookup/create. Finché non risolto, mantenere il
perimetro dimostrativo con dati sintetici. Con il docente acquisire le fonti e
formalizzare le decisioni indispensabili; la mancanza delle fonti blocca una
dichiarazione di conformità, non la correzione dei difetti tecnici già dimostrati.
**Acceptance:** nessuna richiesta per un cliente arbitrario o divulgazione PII
basata sulla sola targa nel perimetro destinato all'uso reale; prove negative verdi.

**P1 — rendere completa e affidabile la baseline integrata.** Ordine consigliato:

1. M1/M2: F-03/F-04, limiti condivisi e countdown senza ciclo; aggiungere regressioni
   su PostgreSQL/browser, inclusa vista reference. Questi difetti non richiedono
   una ridefinizione dell'architettura.
2. M1: F-05/F-06, policy temporali univoche con decisione docente sul pricing;
   provare le gare fra transizioni e l'errore dopo scadenza.
3. M1/M3/M5: F-02/F-08, scope idempotenza e protocollo outbox con claim/retry/recovery;
   provare due consumer e crash, mantenendo esplicita la semantica almeno una volta.
4. M1/M2/M4: F-07/F-10, DTO minimo autista, intervento/costo nel client, contratto
   mappe e invalidazione stime; introdurre mappa visuale solo se confermata nello scope.
5. M1/team: F-09/F-11/F-12, test negativi sicurezza, migrazione del vincolo coordinate,
   contratto OpenAPI coerente; processo di upgrade DB ripetibile.
6. Team/QA: F-13 e sezione 6, aggiungere Python e verifiche contrattuali alla CI,
   browser sul flusso principale, correggere BUR/FUD/workbook e rigenerare derivati
   soltanto dopo aver corretto le fonti.

**Acceptance P1:** nessun P1 aperto nel perimetro confermato, 60 test esistenti
ancora verdi più regressioni mirate, suite Python in CI, percorso centrale/autista
dimostrato E2E e documenti coerenti con prove datate sullo SHA verificato.

**P2 — sostenibilità e funzioni condizionate dallo scope.** M1/team: dividere
gradualmente MissionService per use case, centralizzare stati senza rompere i
consumer, paginare liste e spostare aggregazioni nel DB dopo misurazione;
aggiungere osservabilità, retention, backup/restore e prove di carico. M3/M5:
durabilità delle conversazioni/offset e gestione backpressure. Valutare gestione
utenti, WhatsApp/webhook e provider di produzione solo dopo conferma del requisito.
**Acceptance:** misure e test di recupero ripetibili; nessuna nuova funzione
presentata come implementata prima della relativa verifica.

La fase 2 parte dal branch `feature/backend` allo SHA indicato, con revisione di
questo audit e selezione dei primi item P0/P1. Questa fase non autorizza né esegue
commit applicativi, push, Pull Request o interventi sul branch `main`.

## 9. Prosecuzione autorizzata: hardening e integrazione

Le sezioni precedenti fotografano la fase 1 sulla baseline 5a16da8 e non sono
state riscritte retroattivamente. La richiesta successiva autorizza modifiche,
commit, push esclusivamente su feature/backend e Draft PR, senza merge finale.
Correzioni, inventario completo fetch/controller/service/DB, file modificati,
impatti per frontend e chatbot e prove nuove sono registrati in
[BACKEND_E2E_AUDIT.md](BACKEND_E2E_AUDIT.md) e [SECURITY_REVIEW.md](SECURITY_REVIEW.md).
F-03/F-04/F-05 corretti nei casi verificati; F-02 e F-09/F-10 mitigati;
F-01 resta aperto per titolarità reale, F-08 per delivery concorrente/crash.
Java 25/PostgreSQL 18: 118 test PASS senza skip; Node4 PASS; browser2 PASS con
commit SQL riletto da connessione indipendente. Non si dichiara produzione pronta.

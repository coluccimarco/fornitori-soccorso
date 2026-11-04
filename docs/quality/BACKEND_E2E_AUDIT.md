# Backend hardening e integrazione end-to-end

**Data:** 04/11/2026 — **Responsabile backend:** Minervini Vincenzo.
**Baseline:** `5a16da8533e755cebdec605216633abccf7add1f`; branch `feature/backend`.
Il rapporto prosegue [l'audit iniziale](BACKEND_INITIAL_AUDIT.md), che resta evidenza
della fase 1. Non sostituisce l'approvazione dei requisiti da parte del docente.

## Git e fonti

Fetch aggiornato: `origin/main=5a16da8`, `origin/feature/backend=d4c9dea` prima della
pubblicazione. Main era già integrato via fast-forward, nessun ulteriore merge
necessario. L'audit non tracciato è stato copiato fuori repository con SHA256
coincidente prima delle modifiche. La precedente configurazione sensibile resta
nel checkpoint DPAPI descritto nella fase 1, mai ripristinata o versionata.

La lettura completa di BUR, FUD, TAD, requirements, architecture, management,
quality, team, manuals, agile, diagrams, OpenAPI, SQL e README è stata effettuata
nella fase 1 della stessa sessione. In questa fase sono stati riletti i contratti,
le guide e i file coinvolti. Nessun cambiamento remoto intercorso. Confermati:
fonti PDF/Excel originali assenti; nessuna approvazione docente inventata; DOCX
derivati dai Markdown e workbook da correggere alla fonte del generatore.

## Architettura e modifiche concrete

Il monolite conserva controller, DTO, entity, enum, repository, service, config e
common. I controller delegano senza SQL; i service gestiscono transazioni e
autorizzazioni; i repository persistono; nessuna entity è risposta HTTP.
L'avvio del contesto e i test non rilevano dipendenze circolari. Restano un
`MissionService` ampio, stati stringa e letture non paginate: nessuna riscrittura
o nuovo framework introdotto per motivi puramente stilistici.

- `common/ApiError` elimina la dipendenza impropria common → controller.
  `ApiExceptionHandler` uniforma anche parametri non convertibili, parametri
  mancanti, metodo e media type errati; mantiene messaggi interni fuori dal JSON.
- `RegistryDtos` rispetta le lunghezze SQL; `MissionDtos` limita precisione e range.
  Il service controlla anche l'importo **calcolato**, prima di mutare la missione:
  km memorizzabili non implicano costo memorizzabile. Nessun `double` economico.
- `MissionService.reject` applica la stessa deadline di accept e conserva lo stato
  scaduto anche quando risponde 409. La vecchia proposta non dà accesso alla
  missione riassegnata, alle note o al routing del nuovo autista.
- Il DTO missione aggiunge nome/telefono cliente e dati officina solo per missioni
  live proposta/accettata. Non espone intere anagrafiche, polizze o credenziali.
- Login: rotazione CSRF esplicita dopo autenticazione, oltre alla session ID già
  ruotata; limite 20 tentativi/minuto per indirizzo remoto; timeout inattività
  esplicito 30 minuti; profilo HTTPS `prod`; `LoginRequest.toString()` redatto.
- Chat: grant persistente sessione/cliente per 15 minuti dopo lookup positivo,
  polizza ricontrollata alla creazione; fingerprint della richiesta per retry;
  limite globale lookup 100/minuto per istanza oltre al limite per sessione.
  Questo autorizza la pratica nel flusso, **non verifica la titolarità**.
- Mappe: risposte <=1 MiB, timeout connessione/lettura 3/8 s, redirect rifiutati,
  coordinate/distanze validate, stima invalidata al cambio autista/intervento e
  salvataggio condizionato allo stato letto prima della chiamata esterna.
  Nessuna transazione DB resta aperta durante HTTP esterno.
- Ricerca clienti: termine normalizzato vuoto non amplia la ricerca per targa a
  tutti i clienti. Query parametrizzata invariata nella struttura.

## Inventario completo delle chiamate frontend

In `app.js` esiste un solo `fetch`, nel wrapper `request`: cookie same-origin,
JSON, header CSRF sulle mutazioni, lettura 204 senza body. La tabella espande ogni
consumer, incluse le cinque anagrafiche e le due azioni dinamiche accept/reject.
Tutti gli URL sono relativi a `/api/v1`. `M` = MissionController/MissionService,
`R` = RegistryController/RegistryService, `A` = AuthController/AuthService.
Le response sono DTO, non entity. `B` indica prova browser con DB e commit;
`H` prova MockMvc con service e PostgreSQL reali; `C` contract/MVC senza DB.

| Evento UI | HTTP / URL | JSON / query | Controller → service | DTO request → response | Repository / tabelle | Risposta e gestione UI | Evidenza |
|---|---|---|---|---|---|---|---|
| Avvio pagina | GET `/auth/me` | — | A → currentUser | — → CurrentUser | AppUser, Driver / app_users, drivers | 200 showApp; 401 pagina pubblica | B,H,C |
| Prima mutazione/login | GET `/auth/csrf` | — | A.csrf / Security | — → CsrfTokenResponse | HttpSession, nessuna tabella | token/headerName conservati | B,H,C |
| Submit login | POST `/auth/login` | email,password | A → AuthenticationManager, DatabaseUserDetailsService, currentUser | LoginRequest → CurrentUser | AppUser, Driver | 200, nuovo CSRF, vista per ruolo; 401 messaggio generico | B,H,C |
| Click logout | POST `/auth/logout` | —, CSRF | A → SecurityContextLogoutHandler | — → vuoto | invalida HttpSession | 204, pagina pubblica, token client rimosso | B,H,C |
| Login centrale/refresh/missioni | GET `/autisti` | — | R.drivers | — → DriverDto[] | Driver / drivers + missions live | opzioni e disponibilità aggiornata | B,H,C |
| Login centrale/refresh | GET `/mezzi` | — | R.vehicles | — → VehicleDto[] | Vehicle / vehicles | tabella, targa del mezzo | B,H,C |
| Login centrale/refresh | GET `/compagnie` | — | R.companies | — → CompanyDto[] | InsuranceCompany | elenco e select cliente | B,H,C |
| Login/ricerca debounce | GET `/clienti` | search opzionale | R.customers | — → CustomerDto[] | Customer.search, join insurance_companies | lista/ricerca e select missione | B,H,C |
| Login centrale/refresh | GET `/officine` | — | R.workshops | — → WorkshopDto[] | Workshop | lista e select traino | B,H,C |
| Form nuovo cliente | POST `/clienti` | nome,telefono,targa,compagniaId,numeroPolizza,scadenzaPolizza; modelloVeicolo/cittaResidenza opzionali | R.createCustomer | CreateCustomerRequest → CustomerDto | Customer, InsuranceCompany / customers | 201 chiude modal e rilegge registri; 400/409 alert | B,H,C |
| Vista missioni/filtro/refresh | GET `/missioni` | mode live/reference, stato opzionale | M.list | — → MissionResponse[] | Mission / missions | render schede, proposta solo live | B,H,C |
| Stesso evento | GET `/dashboard` | mode | M.dashboard | — → DashboardResponse | Mission, Driver | aggregati e guadagni per autista | B,H,C |
| Form nuova missione | POST `/missioni` | clienteId,tipoIntervento,descrizioneProblema,origineIndirizzo,canaleOrigine,officinaId opzionale | M.create | CreateMissionRequest → MissionResponse | Mission, Customer, Workshop, Event | 201 chiude modal e rilegge missioni | B,H,C |
| Invia proposta | POST `/missioni/{id}/proposte` | autistaId | M.propose | ProposeMissionRequest → ProposalResponse | Mission, Driver, Proposal, Pricing, Event | 200 rilegge missioni/driver; 409 conflitto | B,H,C |
| Login/refresh autista | GET `/autista/missioni` | — | M.driverMissions | — → MissionResponse[] | Mission, Driver, Customer, Workshop | proposte proprie e missione attiva | B,H,C |
| Accetta | POST `/missioni/{id}/accept` | — | M.accept | — → MissionResponse | Mission, Proposal, Event, Outbox, ChatRequest | 200 rilegge; 409 se scaduta/già risposta | B,H,C |
| Rifiuta | POST `/missioni/{id}/reject` | — | M.reject | — → MissionResponse | Mission, Proposal, Event | 200 rilegge; 409 scaduta | B,H,C |
| Mostra note/missione attiva | GET `/missioni/{id}/notes` | — | M.getNotes | — → MissionNoteResponse[] | Mission, Note, User, Driver | rendering escaped di testo/autore/data | B,H,C |
| Submit segnalazione | POST `/missioni/{id}/notes` | testo | M.addNote | CreateNoteRequest → MissionNoteResponse | Mission, Note, User, Driver | 201 reset form e rilettura | B,H,C |
| Chiudi missione | POST `/missioni/{id}/complete` | kmEffettivi | M.complete → PricingService | CompleteMissionRequest → MissionResponse | Mission, Proposal, Pricing, Event, Outbox | 200 rilegge e conferma; 400/409 mantiene errore | B,H,C |
| Vista storico | GET `/autista/storico` | — | M.driverHistory | — → DriverHistoryResponse | Mission, Driver | storico personale, totaleEur | B,H,C |

Le mutazioni usano transazioni service; i lock proteggono missione/autista e gli
indici parziali impediscono doppie assegnazioni live. Le letture service sono
read-only. Il browser verifica commit con una **connessione SQL separata**, poi
rilegge API/UI. I test HTTP transazionali usano flush/clear e rollback finale:
non sono spacciati per commit duraturi. Due test concorrenti usano transazioni
reali e verificano esclusione/deduplicazione dopo commit.

### Endpoint non chiamati dalla UI

`GET /missioni/{id}`, `PATCH /missioni/{id}/intervento`, `PATCH /missioni/{id}/cost`
sono presenti/testati, ma senza comando web dedicato. La UI usa invece il DTO
dell'elenco per il dettaglio autista. `GET /maps/route` non è integrato nella UI.
Le cinque operazioni `/chat/*` sono consumate dal bot Python. Le due operazioni
utenti e i due webhook restano `designed`. In totale: **21 operazioni consumate
dal frontend, 9 altre implementate, 4 progettate**; 30 implementate su 34 dichiarate.

## Contratti e impatto sui colleghi

Per **Valeriano Francesco e Cangellosi Giuseppe**: nessuna modifica a CSS,
estetica, navigazione o layout. `index.html` cambia solo maxlength nome/telefono
150/32. `app.js` rinnova CSRF dopo login, elimina token al logout, riconosce una
sessione persa anche dopo 403 senza ritentare automaticamente una scrittura,
limita refresh scadenza a 30 s senza ricorsione/reference, rilegge disponibilità
driver e usa cinque nuovi campi opzionali nel markup già esistente. Numeri JSON
rimangono numeri, date ISO e stati lowercase; il costo rimane autoritativo server.

Per **Colucci Marco**: endpoint e campi Python non rinominati. Il flusso normale
lookup → conferma → create con stessa sessionId rimane valido. Una creazione
senza lookup o oltre 15 minuti ora riceve 403; ripetere lookup prima di una nuova
conferma. Retry con richiesta identica mantiene 201 e stessa missione; mismatch
sessione/cliente/canale/destinatario/payload dà 409. Le richieste legacy senza
fingerprint possono confrontare solo i campi disponibili: limite dichiarato.
TTL, policy scaduta e identità reale richiedono coordinamento prima dell'uso reale.

Per mappe: il GET legacy salva dati e ora **richiede X-CSRF-TOKEN**. Nessun
consumer `app.js` coinvolto. Chiamanti esterni devono recuperarlo da `/auth/csrf`.
Non si rinomina l'endpoint durante questa revisione. Outbox: invio almeno una
volta, non exactly-once; multi-dispatcher/lease e recovery persistente rimangono aperti.

## PostgreSQL e integrità

Migrazione `database/07_backend_hardening.sql`: CHECK esplicito per coppia
coordinate, tabella `chat_lookup_grants`, indice expiry, colonna fingerprint
nullable per compatibilità. Nessuna tabella eliminata e nessuna fixture riscritta.
Su un database già inizializzato 01–06 applicare 07; su uno vuoto applicare 01–07
in ordine. Se esistono coordinate incomplete la VALIDATE fallisce: correggere
solo dopo verifica dell'origine, senza inventare valori. Il grant usa repository
JDBC parametrizzato nella stessa transazione del lookup; il resto conserva JPA.

Connessione **autenticata e read-only** a localhost:5432/fornitori: PostgreSQL 18.6,
utente fornitori_app; `missions` non esiste. Stato: connessione PASS, disponibilità
schema applicativo BLOCKED. Nessuno script applicato al DB personale. Gli script
e i test sono eseguiti su cluster nuovi, porte 55437/55438; il browser usa un
ruolo runtime non-superuser con soli DML, uso schema e sequenze.

Baseline isolata: 30 fixture; 16 eseguite, 6 pending, 4 scadute, 4 rifiutate;
totale EUR1509.71 e assertion per autista PASS. Le fixture restano separate dalle
missioni live, inclusi vincoli di concorrenza e letture dashboard.

## Test ed evidenze

| Categoria | Risultato locale | Limite |
|---|---|---|
| Java 25 / PostgreSQL 18.6 | 118 test, 0 failure/error/skip | Dettaglio in TEST_EVIDENCE |
| Contract OpenAPI | 33 test inclusi nei 118 | 32 record/tipi + mapping operazioni; non convalida ogni vincolo semantico OAS |
| Python chatbot | 64 test | Provider simulati; nessun Telegram reale |
| JavaScript Node | 4 test PASS | Countdown e escaping, non browser |
| Browser Chromium 156 / DB | 2 scenari PASS | Due scenari e connessione SQL indipendente |
| OSV dipendenze Maven | 124 dipendenze; 0 advisory corrispondenti al momento della scansione | Non garantisce assenza vulnerabilità o copertura futura |
| Provider live / WhatsApp | NOT RUN / non implementato | Non usare dati reali per collaudo indiscriminato |
| CI remota nuovo branch | Da verificare dopo push | Run storici non attribuiti ai nuovi commit |

Prima delle correzioni: 8 nuovi contract test hanno prodotto 4 failure reali
(nome troppo lungo, due distanze non valide, ID non convertibile). Dopo le
correzioni passano. Gli altri casi negativi sono regression test aggiunti, non
si dichiara una loro esecuzione precedente contro la baseline.
Durante la preparazione: corretta una dipendenza mock mancante nei test MVC;
nel browser corretti un selettore nascosto e la simulazione di scadenza che
violava il CHECK della deadline. Nessun test disabilitato per ottenere PASS.

Comandi riproducibili: `./mvnw clean verify` con datasource isolato, `python -m
unittest discover` da chatbot, `node --test tools/test_frontend.cjs`, `python
tools/validate_docs.py`; scanner: Maven `dependency:list` e `python
tools/check_dependencies.py`. Browser: installare `tools/requirements-e2e.txt`,
`python -m playwright install chromium`, poi `python tools/run_isolated_browser.py`
con `PG_BIN` e `JAVA_HOME`. Il runner crea directory/cluster nuovi, genera
credenziali casuali, conserva log/screenshot privati e arresta i propri processi.

## File e performance

Modifiche Java: DTO Auth/Registry/Mission, AuthController, common ApiError/handler,
SecurityConfig, RequestRateLimiter, MissionService/ChatService/MapService,
ProviderHttp/Nominatim/OSRM/MapProviders, Mission/ChatMissionRequest,
CustomerRepository/ChatLookupGrantRepository. Configurazioni: application e prod.
SQL: solo 07 nuovo. Frontend: app.js/index.html, nessun CSS. Test: suite esistenti
estese, ApiContract/OpenApiContract/PostgresHttpWorkflow/RateLimit nuovi; runner
browser e JS, scanner dipendenze. CI e documenti canonici aggiornati con gli esiti.

Nessun benchmark latenza/throughput o EXPLAIN eseguito: non si dichiara un guadagno
percentuale. Miglioramenti verificabili: un refresh scadenza ogni 30 s, body
provider limitato a 1 MiB, memoria limiter limitata a 10.000 chiavi, nessuna
transazione durante provider HTTP. Restano aggregazioni in memoria, liste non
paginate e lookup aggiuntivi per contatti operativi; dimensionamento non validato.

## Residui e gate di rilascio

P0 prima di dati reali: identità/titolarità chatbot e consenso/privacy con il
docente; non risolti dal grant. P1: lease/retry persistenti outbox, recovery bot,
deployment HTTPS e segreti/ruoli DB operativi, inizializzazione DB locale, UI
rettifica/intervento/mappe e collaudo consumer dopo nuova policy CSRF/lookup.
P2: paginazione, separazione interna del service, carico e osservabilità,
verifica visuale Word/accessibilità/browser multipli. Vedere [security review](SECURITY_REVIEW.md).

La consegna sarà una **Draft PR** finché questi gate essenziali restano aperti;
nessun merge su main. Il dettaglio SHA/commit/URL viene restituito nel report
finale e nella PR, evitando riferimenti circolari al commit che contiene questo file.

## Matrice requisiti aggiornata

I riferimenti ai componenti provengono dalla fase 1, gli esiti qui sotto dalla
revisione corrente. PASS significa soltanto i casi nominati, non approvazione
integrale del requisito. Gli identificativi BR/NFR sono quelli canonici locali.

| Requisito | Frontend | Controller/API | Service | Repository | Database | Test | Stato |
|---|---|---|---|---|---|---|---|
| BR-001; UC-002/003 — anagrafiche e cliente | `app.js`: registri, ricerca, form cliente | `RegistryController` | `RegistryService` | Customer, Driver, Vehicle, InsuranceCompany, Workshop Repo | `customers`, `drivers`, `vehicles`, `insurance_companies`, `workshops` | PostgresHttpWorkflowTests + browser A | PASS nei flussi verificati; limiti input corretti |
| BR-002; UC-001 — accesso e ruoli | login/logout/me/csrf same-origin | `AuthController`, `SecurityConfig` | `AuthService`, `DatabaseUserDetailsService` | AppUser, Driver Repo | `app_users`, `drivers` | ApiSecurity/ApiContract + login DB + browser A/B | PASS ruoli/sessione/CSRF; prod da verificare |
| BR-003; UC-004 — creazione/dettaglio live | form missione, filtro live/reference | `MissionController` | `MissionService.create/get/list` | Mission, Customer, Workshop, MissionEvent Repo | `missions`, `mission_events`, FK, `is_fixture` | HTTP/PG + browser A/B | PASS creazione e rilettura; retry centrale non idempotente |
| BR-004; UC-005 — proposta/storico | select autista e POST proposte; storico tentativi non esposto | `MissionController.propose` | `MissionService.propose` | Mission, Driver, MissionProposal, PricingConfig Repo | `mission_proposals`, indici unici parziali | MissionPostgres + browser B | PASS proposte concorrenti e storico persistito |
| BR-005; UC-006 — risposta/scadenza | accept/reject, countdown | `MissionController` | `MissionService.accept/reject/expireDueProposals`, scheduler | Mission, MissionProposal, Driver Repo | deadline, lock pessimisti, `uq_driver_active_live` | MissionService + HTTP/PG Clock + browser B + Node | PASS scadenza accept/reject e refresh limitato |
| BR-006; UC-007/008 — completamento/prezzo | form km e riepilogo | `MissionController.complete` | `MissionService`, `PricingService` | Mission, MissionProposal, PricingConfig, Event, Outbox Repo | km/costi `NUMERIC(10,2)`, date e outbox | PricingService + HTTP/PG + browser A | PASS precisione/costo; fonte docente da convalidare |
| BR-007; UC-008 — rettifica motivata | Nessun comando web `/cost` | `MissionController.adjustCost` | `MissionService.adjustCost` | Mission, MissionEvent Repo | costo calcolato/finale, motivo, attore, evento | HTTP/PG e MissionPostgres | PASS API; comando UI assente |
| BR-008; UC-009 — dashboard/storico autista | vista missioni con aggregati; storico personale | `MissionController` | `dashboard`, `driverMissions`, `driverHistory` | Mission, Driver Repo | fixture/live e costi finali | HTTP/PG + browser A | PASS live/reference e aggregati personali |
| BR-009; UC-004/010 — richiesta e stato chat | Python `ChatEngine` + `HttpBackend`, Telegram/console | `ChatController` | `ChatService`, limiter, `MissionService.createFromChat` | Customer, InsuranceCompany, ChatMissionRequest, Mission, Driver Repo | `chat_mission_requests` da 05/06; `chat_sessions` non usata dal runtime | 15 ChatPostgres + 6 ChatApiSecurity + Python | PARZIALE: grant/retry verificati, titolarita aperta |
| BR-010; UC-011 — notifiche cliente | `NotificationDispatcher`, `TelegramApi`; nessuna UI outbox | `ChatController.notifications/reportNotification` | `MissionService.addOutboxEvent`, `ChatService` | NotificationOutbox, ChatMissionRequest Repo | `notifications_outbox`, event key unica | ChatPostgres + Python notifications | PARZIALE: evento/ACK verificati, lease/crash delivery aperto |
| BR-011; UC-012 — route/km stimati | `app.js` non invoca route, nessuna mappa | `MapController` | `MapService`, Nominatim/OSRM, salvataggi MissionService | Mission, Driver, Vehicle, Workshop, AppUser Repo | coordinate origine e km stimati | 13 MapPostgres + 11 MapProviderClients + CSRF MVC | PARZIALE: API verificata, UI/provider live non collaudati |
| BR-012 — WhatsApp | Nessun adapter WhatsApp; enum non è integrazione | Webhook solo OpenAPI `designed` | Nessun servizio canale | Infrastruttura chat/outbox condivisa, nessun adapter dedicato | canale ammesso dallo schema | Nessun runtime provider | PIANIFICATO, non implementato |
| BR-013 — UI centrale/autista | HTML/CSS/JS presenti | Auth, Registry, Mission; Map non consumato | Servizi sopra | Repository sopra | Tabelle sopra | 2 scenari browser + 4 Node | PARZIALE: ciclo missione PASS; UI cost/intervento/mappe assenti |
| NFR-001 — credenziali fuori dai DTO | Solo `CurrentUser`/DTO, mai entity nel JS | Controller restituiscono DTO | mapping espliciti | hash letto solo da UserDetailsService | hash BCrypt seed | Login HTTP/PG, contract DTO, ispezione log | PASS DTO; policy logging operativa da completare |
| NFR-002 — autorizzazione/CSRF | header dal CSRF endpoint | due security chain; auth, missioni e chat | ruolo/ownership missione, chat a token | utenti/proposte | identità utente/driver | MVC + HTTP/PG + browser B | PASS casi codificati di CSRF e ownership |
| NFR-003 — PostgreSQL/tipi | Input numerici/data | DTO validati parzialmente | transazioni JPA | repository PostgreSQL | sei script, Hibernate validate | Migrazione07, schema validate, HTTP/PG e browser | PASS cluster isolati; DB personale non inizializzato |
| NFR-004 — assegnazioni concorrenti | conflitto HTTP gestito genericamente | `MissionController` | lock missione poi driver | lock pessimisti | unicità driver live e proposta | MissionPostgres: due assegnazioni e due accettazioni concorrenti | PASS transazioni reali e indici preservati |
| NFR-005 — bot solo via API | `HttpBackend`, nessun driver SQL | `ChatController` | `ChatService` | solo backend accede al DB | persistita dal backend | Python e ChatPostgres | PASS architettura bot via API; identita distinta |
| NFR-006 — segreti/errori integrazione | messaggi fallback, token da environment | error handler + token filter | timeout provider, errori espliciti | outbox errori persistiti | hash/metadata, non token configurazione | OSV124, provider11, checkpoint e diff | PARZIALE: hardening verificato, deployment/governance aperti |

## Checklist e percentuali indicative

Percentuali = elementi verificati / elementi elencati, arrotondate all'intero;
non rappresentano copertura linee, conformità ASVS o avanzamento assoluto del
progetto. Un elemento incompleto conta zero, anche se parzialmente implementato.

| Area | Verificati | Ancora aperti | Calcolo |
|---|---|---|---|
| Architettura | Layer, delega controller, transazioni service, repository parametrizzati, DTO senza entity, assenza cicli al bootstrap | Nessuno dei 6 controlli; dimensione service resta debito | 6/6 = 100% del controllo strutturale |
| Database | Schema/FK, unique live, coordinate, fixture, transazioni concorrenti, runtime isolato non-superuser | Schema del DB personale operativo | 6/7 = 86% |
| API | 30 operazioni implementate, mapping verificato | 4 operazioni designed (utenti/webhook) | 30/34 = 88% della superficie dichiarata |
| Sicurezza | BCrypt/sessione, CSRF, ruoli, ownership, input, rate limit locale, provider bounds, errori/redazione | Identità chatbot, deployment HTTPS operativo, recovery delivery outbox | 8/11 = 73% della checklist |
| Integrazione frontend | Login/logout, registri, cliente, creazione missione, proposta, risposta/rifiuto, note, chiusura, dashboard/storico | Rettifica, modifica intervento, mappa | 9/12 = 75% |
| Test | Unit/MVC, PostgreSQL, contract, Python, Node, browser Chromium | Provider live, browser multipli/accessibilità completa | 6/8 = 75% delle categorie scelte |
| Documentazione | Canonici architettura/API, tracciabilità, manuali/team, evidenze/rischi, generazione derivati | Fonte originale docente e controllo visuale Word | 5/7 = 71% dei gate documentali |

### Regressione HEAD e CI remota

Il controllo finale ha riprodotto un bypass sul metodo HEAD implicito del GET
mappe: risposta200 senza CSRF. Un nuovo test MVC falliva prima della correzione;
il matcher copre ora GET **e HEAD**, mantenendo le protezioni delle mutazioni.
La suite completa successiva comprende118 test. Il primo run della PR#5,
[41807529992](https://github.com/coluccimarco/fornitori-soccorso/actions/runs/41807529992),
è PASS su entrambi i job al commit7dc0aa3 (precedente al fix HEAD), incluso browser
con PostgreSQL. Il nuovo run sul commit correttivo è riportato nella PR e nel
report finale: non attribuire il run precedente a un nuovo SHA.

Anche `CsrfTokenResponse.toString()` è redatto; il test diagnostica verifica
che password e token casuali non compaiano nelle rappresentazioni dei DTO.

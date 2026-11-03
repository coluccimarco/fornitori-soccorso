# FORNITORI — gestione del soccorso stradale

Project work ITS Developer 4.0 per coordinare richieste di soccorso stradale,
centrale operativa, autisti e integrazioni esterne. La baseline `main` integrata
fino a `f763d53` comprende backend Java/Spring, client web statico, workflow
missioni, chatbot Telegram e API di geocodifica/percorso. La UI non visualizza
ancora mappe e WhatsApp resta pianificato. CI e controlli locali sulla baseline
integrata sono completati; i limiti browser/provider sono esplicitati nei report.

Apri la [documentazione aggiornata](./docs/README.md) o l'[indice generale](./docs/00_INDICE_GENERALE.md)
per audit, BUR/FUD/TAD, guide, test e Word modificabili. Il PDF e l'Excel originali
del docente non sono presenti nella working tree esaminata: i requisiti non sono
quindi dichiarati approvati.

Il [report conclusivo della revisione](./docs/management/FINAL_REVIEW_REPORT.md)
riassume evidenze, deliverable e attività ancora bloccate.

## Stato della baseline

| Area | Stato |
|---|---|
| Java 25 LTS / Maven | **IMPLEMENTATO**; verificato con Temurin 25.0.5 |
| Backend Spring Boot: sessione, CSRF, ruoli, anagrafiche e rotte missioni | **IMPLEMENTATO**; 11 test MVC passati |
| Schema, seed e baseline PostgreSQL | Script **IMPLEMENTATI**; baseline PostgreSQL CI **PASS**; servizio locale attivo, ma accesso app locale da configurare con credenziali |
| Missioni, timeout, pricing, note, dashboard e storico autista | **IMPLEMENTATO** in `main`; PostgreSQL CI **PASS** |
| Chatbot (`chatbot/`) e API `chat/*` | **IMPLEMENTATO**; prova Telegram dichiarata nel README del modulo |
| Notifiche al cliente | Outbox/backend e dispatcher Telegram presenti; prova reale dichiarata nel README del modulo; WhatsApp **PIANIFICATO** |
| UI web | **PRESENTE** in `src/main/resources/static/`: pagina pubblica, login, centrale e area autista; browser E2E **NOT RUN** |
| API mappe | **PRESENTE** per geocodifica, percorso e distanza stimata; la UI non mostra ancora la mappa |
| CI Java 25 | Build, test, OpenAPI e PostgreSQL baseline/integration **PASS** (run `41656606659`, commit `f763d53`) |

## Requisiti locali

- JDK 25 LTS (il POM dichiara sia `java.version` sia `maven.compiler.release` a 25).
- PostgreSQL 18 o compatibile.
- Maven Wrapper incluso; scarica Maven 3.9.9 al primo avvio.

## Stack tecnologico

| Area | Tecnologia |
|---|---|
| Backend | Java 25 LTS, Spring Boot 4.1.2, Spring Security, Spring Data JPA |
| Build | Maven Wrapper 3.9.9 |
| Persistenza | PostgreSQL 18 o compatibile |
| Contratto | REST/JSON, OpenAPI 3.0.3 |
| Client | Web HTML/CSS/JavaScript/Bootstrap 5.3, servito da Spring Boot |
| Provider previsti | Telegram e WhatsApp Business/Twilio, soggetti ad approvazione |

### Windows: installazione JDK 25

Usa una distribuzione JDK 25 affidabile, per esempio Eclipse Temurin 25 LTS.
Per una sessione PowerShell senza modifiche globali:

```powershell
$env:JAVA_HOME = "$HOME\.jdks\temurin-25.0.5+8"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\mvnw.cmd -version
```

In alternativa scarica l'ultima release 25 LTS da
[Adoptium](https://adoptium.net/temurin/releases/?version=25), verifica il checksum
pubblicato dal distributore e configura `JAVA_HOME` per il tuo utente. Non abbassare
il target Maven per aggirare un JDK mancante.

La password PostgreSQL non è inclusa nel repository. Impostare localmente:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<password configurata localmente>"
```

Il database e l'utente applicativo devono essere predisposti dall'amministratore
PostgreSQL. Non riutilizzare credenziali dimostrative fuori da un ambiente locale.

## Schema e caricamento dati

Eseguire in ordine nel database `fornitori`:

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/03_verifiche.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/05_chat_requests.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/06_chat_recipient.sql
```

Chi ha già un database deve eseguire solo gli script `05` e `06` che non ha ancora
applicato, altrimenti l'applicazione non si avvia.

Gli script non cancellano dati. Lo schema usa `IF NOT EXISTS`; il seed usa conflitti
per chiave primaria come no-op e riallinea le sequenze identity. Applicare le future
modifiche strutturali con migrazioni esplicite, non con Hibernate `update`.

Le righe del docente in `missions` sono marcate `is_fixture=TRUE` e costituiscono lo
storico di riferimento al 28/10/2026. Non vanno modificate dallo scheduler operativo.

## Build, test e avvio

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Con il backend avviato, aprire `http://localhost:8080/`. La pagina pubblica è
accessibile senza login; l'area riservata usa la sessione e le API REST dello stesso
server.

Hibernate è configurato in `validate`: l'applicazione si avvia solo se il database
esiste, gli script sono stati eseguiti e le variabili datasource sono corrette.
Il test di caricamento del contesto richiede `SPRING_DATASOURCE_PASSWORD`; i test
web di sicurezza non accedono al database.

La pipeline [backend-ci.yml](./.github/workflows/backend-ci.yml) verifica Java 25
e, in un job separato, inizializza un PostgreSQL effimero con schema, seed e
assertion di baseline.

## API backend

Tutte le anagrafiche e le operazioni di sessione, tranne il recupero CSRF e il login,
richiedono una sessione. Le API centrali richiedono `RESPONSABILE`; il ruolo
`AUTISTA` non riceve i dati riservati della centrale. Le richieste mutative richiedono
il token ottenuto da `GET /api/v1/auth/csrf` nell'header `X-CSRF-TOKEN`.

| Metodo | Endpoint | Stato |
|---|---|---|
| POST | `/api/v1/auth/login` | Implementato; JSON, sessione HttpOnly |
| POST | `/api/v1/auth/logout` | Implementato; sessione invalidata |
| GET | `/api/v1/auth/me` | Implementato |
| GET | `/api/v1/auth/csrf` | Implementato |
| GET | `/api/v1/autisti` | Implementato; disponibilità calcolata dalle missioni operative |
| GET | `/api/v1/mezzi` | Implementato |
| GET | `/api/v1/compagnie` | Implementato |
| GET | `/api/v1/clienti?search=...` | Implementato; nome o targa |
| POST | `/api/v1/clienti` | Implementato; centrale |
| GET | `/api/v1/officine` | Implementato |
| GET/POST | `/api/v1/missioni` | Implementato in `main` |
| GET | `/api/v1/missioni/{id}` | Implementato; autista limitato alle missioni autorizzate |
| POST | `/api/v1/missioni/{id}/proposte` | Implementato |
| POST | `/api/v1/missioni/{id}/accept`, `/reject`, `/complete` | Implementato |
| PATCH | `/api/v1/missioni/{id}/cost` | Implementato |
| GET/POST | `/api/v1/missioni/{id}/notes` | Implementato |
| GET | `/api/v1/dashboard`, `/api/v1/autista/missioni`, `/api/v1/autista/storico` | Implementato |
| PATCH | `/api/v1/missioni/{id}/intervento` | Implementato; centrale |
| GET | `/api/v1/maps/route` | Implementato; centrale e autista, usa Nominatim e OSRM |
| POST | `/api/v1/chat/lookup-targa`, `/api/v1/chat/missioni` | Implementato; token di servizio del chatbot |
| POST | `/api/v1/chat/stato` | Implementato; token di servizio del chatbot |
| GET | `/api/v1/chat/notifiche` | Implementato; token di servizio del chatbot |
| POST | `/api/v1/chat/notifiche/{id}/esito` | Implementato; token di servizio del chatbot |

Le API `chat/*` usano `Authorization: Bearer` con il valore di `FORNITORI_CHAT_TOKEN`,
da impostare uguale sul backend e sul chatbot. Se manca, rispondono sempre `401`.
Vedi [chatbot/README.md](./chatbot/README.md).

Il contratto completo è in [api/openapi.yaml](./api/openapi.yaml); gli stati
`implemented` e `designed` vanno letti per singola operation e non attestano da
soli la verifica end-to-end. Le nuove API mappe sono parte del backend integrato.
La UI non invoca ancora il servizio mappe.

## Struttura Spring Boot

I package seguono la suddivisione per layer del progetto di riferimento SpringbootES:

```text
src/main/java/it/exprivia/fornitori/
├── FornitoriApplication.java
├── config/       # configurazione Spring Security e sessione
├── controller/   # endpoint REST
├── dto/          # payload di richiesta e risposta
├── entity/       # entità JPA
├── enums/        # ruoli applicativi
├── repository/   # query e accesso ai dati con Spring Data JPA
├── service/      # logica applicativa
└── common/       # gestione condivisa degli errori API
```

Il flusso è `controller → service → repository → entity`; i controller usano i DTO
per non esporre direttamente le entità persistenti. Le classi di test sono in
`src/test/java/it/exprivia/fornitori/`.

## Credenziali demo e sicurezza

Il seed contiene esclusivamente hash BCrypt delle password d'esempio. Le credenziali
da usare per una demo locale vanno consultate nei materiali distribuiti dal docente;
non copiarle in questo repository, nei log o in ambienti pubblici. I DTO non espongono
password né hash. Il cookie di sessione è HttpOnly e le richieste mutative mantengono
la protezione CSRF.

## Componenti e documentazione

Apri la [dashboard visuale Spring & Scrum](./docs/SPRING_SCRUM_DASHBOARD.html)
nel browser per esplorare i layer Spring, filtrare il backlog per Sprint e
scaricare un CSV importabile in Jira. È una vista statica del repository, non una
dashboard collegata a Jira.

| Componente | Responsabile | Guida |
|---|---|---|
| Backend e database | Membro 1 | [Guida backend](./docs/team/MEMBRO_1_BACKEND.md) |
| Frontend | Membro 2 | [Guida frontend](./docs/team/MEMBRO_2_FRONTEND.md) |
| Chatbot | Membro 3 | [Guida chatbot](./docs/team/MEMBRO_3_CHATBOT.md) |
| Mappe | Membro 4 | [Guida mappe](./docs/team/MEMBRO_4_MAPS.md) |
| Notifiche | Membro 5 | [Guida notifiche](./docs/team/MEMBRO_5_NOTIFICATIONS.md) |

- Inizia da [indice generale](./docs/00_INDICE_GENERALE.md) e
  [guide del team](./docs/team/README.md).
- Architettura, database, API e regole: [documentazione per area](./docs/README.md).
- Piano Scrum: [registro](./docs/agile/README.md) e [backlog](./docs/agile/PRODUCT_BACKLOG.md).
- Processo branch/PR: [docs/07_GIT_WORKFLOW.md](./docs/07_GIT_WORKFLOW.md).

- Report Sprint 1: [docs/SPRINT_1_REPORT.md](./docs/SPRINT_1_REPORT.md)
- Evidenze effettive: [docs/quality/TEST_EVIDENCE.md](./docs/quality/TEST_EVIDENCE.md)
- Database e dizionario: [docs/architecture/DATABASE_DESIGN.md](./docs/architecture/DATABASE_DESIGN.md)
- Contratto API normativo: [api/openapi.yaml](./api/openapi.yaml)
- Manuale tecnico: [docs/manuals/MANUALE_TECNICO.md](./docs/manuals/MANUALE_TECNICO.md)
- Manuale utente: [docs/manuals/MANUALE_UTENTE.md](./docs/manuals/MANUALE_UTENTE.md)
- Word editabili: [docs/word/](./docs/word/)

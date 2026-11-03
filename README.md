# FORNITORI — gestione del soccorso stradale

Project work ITS Developer 4.0 per coordinare richieste di soccorso stradale,
centrale operativa, autisti e integrazioni esterne. Il repository è la baseline
condivisa dei cinque componenti; al 03/11/2026 backend Sprint 1, schema, seed,
autenticazione e anagrafiche sono implementati. Workflow missioni, UI e integrazioni
restano pianificati o da validare: non considerarli funzionalità disponibili.

## Stato della baseline

| Area | Stato |
|---|---|
| Java 25 LTS / Maven | **IMPLEMENTATO**; verificato con Temurin 25.0.5 |
| Backend Spring Boot: sessione, CSRF, ruoli e anagrafiche | **IMPLEMENTATO**; 6 test MVC passati |
| Schema, seed e baseline PostgreSQL | Script **IMPLEMENTATI**, esecuzione su database **BLOCCATA** senza credenziali locali |
| Missioni, timeout, pricing, dashboard | **PIANIFICATO** per Sprint 2; non implementato |
| UI, chatbot, mappe e notifiche | **PIANIFICATO**; guide e contratti in `docs/` |
| CI Java 25 | **IMPLEMENTATO** nel workflow; verificare l'esito GitHub dopo il push |

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
| Client previsti | Web HTML/CSS/JavaScript/Bootstrap; Leaflet/OSM per mappe |
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
```

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

Hibernate è configurato in `validate`: l'applicazione si avvia solo se il database
esiste, gli script sono stati eseguiti e le variabili datasource sono corrette.
Il test di caricamento del contesto richiede `SPRING_DATASOURCE_PASSWORD`; i test
web di sicurezza non accedono al database.

La pipeline [backend-ci.yml](./.github/workflows/backend-ci.yml) verifica Java 25
e, in un job separato, inizializza un PostgreSQL effimero con schema, seed e
assertion di baseline.

## API Sprint 1

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

Il contratto completo, incluse le API future, è in [api/openapi.yaml](./api/openapi.yaml).

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

| Componente | Responsabile | Guida |
|---|---|---|
| Backend e database | Membro 1 | [Guida backend](./docs/team/MEMBRO_1_BACKEND.md) |
| Frontend | Membro 2 | [Guida frontend](./docs/team/MEMBRO_2_FRONTEND.md) |
| Chatbot | Membro 3 | [Guida chatbot](./docs/team/MEMBRO_3_CHATBOT.md) |
| Mappe | Membro 4 | [Guida mappe](./docs/team/MEMBRO_4_MAPS.md) |
| Notifiche | Membro 5 | [Guida notifiche](./docs/team/MEMBRO_5_NOTIFICATIONS.md) |

- Inizia da [docs/00_START_HERE.md](./docs/00_START_HERE.md) e
  [guida generale del team](./docs/01_GUIDA_GENERALE_TEAM.md).
- Architettura, database, API e regole: [indice documentazione](./docs/00_START_HERE.md).
- Piano Scrum: [docs/08_AGILE_SCRUM.md](./docs/08_AGILE_SCRUM.md) e [backlog](./docs/agile/PRODUCT_BACKLOG.md).
- Processo branch/PR: [docs/07_GIT_WORKFLOW.md](./docs/07_GIT_WORKFLOW.md).

- Report Sprint 1: [docs/SPRINT_1_REPORT.md](./docs/SPRINT_1_REPORT.md)
- Evidenze effettive: [docs/TEST_EVIDENCE.md](./docs/TEST_EVIDENCE.md)
- Dizionario dati: [docs/DATABASE_DICTIONARY.md](./docs/DATABASE_DICTIONARY.md)
- Contratto API normativo: [api/openapi.yaml](./api/openapi.yaml)

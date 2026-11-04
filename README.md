# FORNITORI — gestione del soccorso stradale

Project work ITS Developer 4.0 per coordinare richieste di soccorso stradale, centrale operativa
e autisti. Comprende backend Java/Spring, client web con mappa, workflow delle missioni e chatbot
Telegram. Requisiti e acceptance del docente restano da confermare: PDF ed Excel originali non sono
nel repository.

Apri l'[indice della documentazione](./docs/00_INDICE_GENERALE.md).

## Stato

| Area | Stato |
|---|---|
| Backend: sessione, CSRF, ruoli, anagrafiche, missioni, tariffe, note, dashboard | Implementato |
| UI web: pagina pubblica, centrale e area autista, mappa Leaflet | Implementato |
| Mappe: geocodifica, percorso e km stimati con Nominatim e OSRM | Implementato |
| Chatbot Telegram e API `chat/*` | Implementato; la sola targa identifica il cliente, limite noto della demo |
| Notifiche al cliente con stato di invio persistente | Implementato per le missioni create dal bot |
| WhatsApp, webhook in ingresso, gestione utenti | Fuori ambito |

## Requisiti

- JDK 25 LTS.
- PostgreSQL 18 o compatibile, con `psql`.
- Maven Wrapper incluso.
- Python 3.10 o superiore per il chatbot.

## Stack

| Area | Tecnologia |
|---|---|
| Backend | Java 25, Spring Boot 4.1.2, Spring Security, Spring Data JPA |
| Build | Maven Wrapper 3.9.9 |
| Persistenza | PostgreSQL 18 |
| Contratto | REST/JSON, OpenAPI 3.0.3 |
| Client | HTML, CSS, JavaScript, Bootstrap 5.3, Leaflet, serviti da Spring Boot |
| Chatbot | Python, sola libreria standard, Telegram in long polling |
| Mappe | Nominatim e OSRM pubblici |

## Configurazione

La password PostgreSQL non è nel repository. Impostarla localmente (PowerShell):

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<password configurata localmente>"
```

Su macOS e Linux usare `export` con gli stessi nomi.

## Database

Creare un database vuoto di proprietà dell'utente applicativo:

```powershell
createdb -O fornitori_app fornitori
```

Eseguire tutti gli script in ordine:

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/03_verifiche.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/05_chat_requests.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/06_chat_recipient.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/07_backend_hardening.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/08_cleanup_and_delivery.sql
```

Hibernate è in `validate`: senza tutti gli script, 07 e 08 compresi, l'applicazione non si avvia. Su un
database esistente applicare solo gli script mancanti, nell'ordine, dopo un backup: chi è già a 07 esegue
solo 08. Dopo 08 non rieseguire 01 e 02: `08_cleanup_and_delivery.sql` rimuove `chat_sessions` e
`drivers.stato_seed`.

Le righe del docente in `missions` sono marcate `is_fixture=TRUE` e costituiscono lo storico di riferimento
al 28/10/2026: non vengono mai modificate.

## Build e avvio

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Su macOS e Linux usare `./mvnw`. Aprire `http://localhost:8080/`: la pagina pubblica è accessibile senza
login, l'area riservata usa la sessione e le API dello stesso server.

La pipeline [backend-ci.yml](./.github/workflows/backend-ci.yml) compila il progetto, applica gli script a
un PostgreSQL effimero e verifica l'avvio dell'applicazione con un ruolo senza privilegi di amministratore.

## API

Il contratto è in [api/openapi.yaml](./api/openapi.yaml): 38 operazioni, 34 implementate e 4 fuori ambito.
Mappa delle superfici in [API_SPECIFICATION.md](./docs/architecture/API_SPECIFICATION.md).

Le API centrali richiedono `RESPONSABILE`; le mutazioni richiedono il token di `GET /api/v1/auth/csrf`
nell'header `X-CSRF-TOKEN`. Le API `chat/*` usano `Authorization: Bearer` con `FORNITORI_CHAT_TOKEN`, da
impostare uguale su backend e chatbot; se manca, rispondono `401`. Vedi [chatbot/README.md](./chatbot/README.md).

## Struttura

```text
src/main/java/it/exprivia/fornitori/
├── config/       # Spring Security e sessione
├── controller/   # endpoint REST
├── dto/          # payload di richiesta e risposta
├── entity/       # entità JPA
├── enums/        # ruoli applicativi
├── repository/   # accesso ai dati
├── service/      # logica applicativa
└── common/       # gestione condivisa degli errori
src/main/resources/static/   # client web
database/                    # script SQL 01..08
chatbot/fornitori_chatbot/   # bot Telegram
```

Il flusso è `controller → service → repository → entity`; i DTO evitano di esporre le entità.

## Credenziali demo e sicurezza

Il seed contiene solo hash BCrypt delle password d'esempio; le credenziali per la demo vanno consultate nei
materiali del docente. Questa è una demo con dati sintetici: la sola targa identifica il cliente e il progetto
non è pronto per dati reali. Vedi [sicurezza](./docs/architecture/SECURITY_DESIGN.md).

## Team

| Area | Responsabili | Guida |
|---|---|---|
| Backend | Vincenzo Minervini | [Guida](./docs/team/MEMBRO_1_BACKEND.md) |
| Frontend | Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio | [Guida](./docs/team/MEMBRO_2_FRONTEND.md) |
| Chatbot | Marco Colucci | [Guida](./docs/team/MEMBRO_3_CHATBOT.md) |
| Mappe | Marco Colucci | [Guida](./docs/team/MEMBRO_4_MAPS.md) |
| Notifiche | Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi | [Guida](./docs/team/MEMBRO_5_NOTIFICATIONS.md) |

Altri riferimenti: [manuale tecnico](./docs/manuals/MANUALE_TECNICO.md),
[manuale utente](./docs/manuals/MANUALE_UTENTE.md), [database](./docs/architecture/DATABASE_DESIGN.md),
[Word editabili](./docs/word/).

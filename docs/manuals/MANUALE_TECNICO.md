# Manuale tecnico FORNITORI

**Codice:** FOR-TECH-001 · **Versione:** 2.0 · **Data:** 04/11/2026
**Destinatari:** sviluppatori · **Responsabile:** Vincenzo Minervini

## Prerequisiti

- Git e JDK 25; verificare `java -version` e `javac -version`.
- PostgreSQL 18 o compatibile e `psql`.
- Maven Wrapper incluso; non serve installare Maven.
- Python 3.10+ per il chatbot, solo con la libreria standard.
- Node.js non è necessario: il frontend non ha un package manager.

## Configurazione e avvio

Da PowerShell, nella root del repository:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<password autorizzata>"
```

Su un database nuovo applicare tutti gli script, in ordine:

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/05_chat_requests.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/06_chat_recipient.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/07_backend_hardening.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/08_cleanup_and_delivery.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/09_integrity_and_indexes.sql
```

Su un database esistente applicare solo gli script mancanti, dopo un backup. Hibernate usa `validate`: se manca uno
script, l'avvio fallisce con un errore `Schema validation`. Dopo 08 non rieseguire 01 e 02. Per un uso condiviso, usare
un account per le migrazioni e un account applicativo non amministratore con `USAGE` sullo schema, DML sulle tabelle e
uso delle sequenze.

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Aprire `http://localhost:8080/`. Per HTTPS usare `SPRING_PROFILES_ACTIVE=prod`: il cookie `Secure` non viene inviato su
HTTP. Sessione inattiva 30 minuti; login limitati a 20 al minuto per IP.

## Proprietà applicative

| Proprietà | Default | Uso |
|---|---|---|
| `fornitori.chat.service-token` (`FORNITORI_CHAT_TOKEN`) | vuoto | Token del chatbot; se vuoto le API chat rispondono 401 |
| `fornitori.missions.expiry-delay-ms` | 30000 | Intervallo del controllo delle scadenze |
| `fornitori.maps.nominatim-url`, `fornitori.maps.osrm-url` | servizi pubblici | Provider di mappe |
| `fornitori.maps.user-agent` | `FORNITORI-project-work` | User-Agent verso Nominatim |
| `fornitori.maps.viewbox` | Puglia | Area di ricerca; vuota per tutta Italia |

## Struttura frontend

| File | Responsabilità |
|---|---|
| `src/main/resources/static/index.html` | Pagina pubblica, login, area centrale e autista, modali |
| `src/main/resources/static/app.js` | Chiamate API, sessione e CSRF, viste, mappa, aggiornamenti periodici |
| `src/main/resources/static/styles.css` | Layout dell'area riservata, responsive |
| `src/main/resources/static/public-site.css` | Sito pubblico |
| `src/main/resources/static/images/` | Illustrazione, icona del chatbot, favicon |

Il client aggiorna ogni 15 secondi gli avvisi della centrale e le proposte dell'autista. Gli errori mostrano `message`
del server o il testo del codice `error`.

## Mappe

`GET /api/v1/maps/route?missionId=...&driverId=...` richiede sessione e `X-CSRF-TOKEN`. `MapService` compone base del mezzo,
sinistro e, per il traino, officina. Se mancano le coordinate usa Nominatim, con al massimo una richiesta al secondo per
istanza; OSRM restituisce km stimati e geometria. La ricerca è limitata alla Puglia: fuori area un indirizzo risulta "non
trovato". Codici di risposta in [`MEMBRO_4_MAPS.md`](../team/MEMBRO_4_MAPS.md).

## Notifiche

Il bot chiama `POST /api/v1/chat/notifiche/claim`, invia e registra l'esito. Stati e tempi sono in
[`MEMBRO_5_NOTIFICATIONS.md`](../team/MEMBRO_5_NOTIFICATIONS.md). Le righe in `FAILED` si leggono su
`notifications_outbox.ultimo_errore`.

## Chatbot

Da `chatbot/`, seguire [`chatbot/README.md`](../../chatbot/README.md) per console, backend HTTP e Telegram. Backend e bot
condividono `FORNITORI_CHAT_TOKEN`. Il bot usa solo `/api/v1/chat/*`, mai il database.

## Controlli prima di un merge

```powershell
.\mvnw.cmd clean package
git diff --check
```

Poi avviare l'applicazione e provare a mano il flusso modificato.

## Test

I test su database cancellano i dati live: partono solo con `FORNITORI_TEST_DATABASE=isolated` e si fermano se il database non si chiama `fornitori_e2e*`.
Comandi e risultati in [`SECURITY_TEST_REPORT.md`](../security/SECURITY_TEST_REPORT.md).

## Risoluzione dei problemi

- `Schema validation: missing column`: mancano script SQL, applicare tutti quelli in `database/`.
- `401/403`: verificare sessione, token e ruolo.
- `400/409`: leggere il messaggio e lo stato prima di ritentare.
- `503` mappe: servizio esterno non disponibile; i km si possono comunque inserire.
- Non registrare password, bearer token, messaggi Telegram o dati personali.


# Manuale tecnico

**Codice:** FOR-TECH-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Destinatari:** sviluppatori M1–M5 · **Baseline:** branch `documentazione`, commit base `25c613b`

## Prerequisiti

- Git.
- JDK 25 (POM imposta `java.version` e `maven.compiler.release` a 25).
- Maven Wrapper incluso; il README indica Maven 3.9.9.
- PostgreSQL 18 o compatibile e `psql` per runtime/test database.
- Python 3.10+ per chatbot; usa libreria standard.

Frontend, Node, mappe e provider WhatsApp non sono richiesti: i rispettivi moduli
non sono nella baseline.

## Configurazione backend

Da PowerShell, dalla root:

```powershell
java -version
javac -version
.\mvnw.cmd -version
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<secret autorizzato>"
```

Usare credenziali provisionate dall'amministratore. Non incollarle in shell history,
Git, screenshot o log. Hibernate è `validate`: applicare SQL prima dell'avvio.
Su database esistente applicare soltanto migrazioni additive non già installate.

Script iniziali per database nuovo, nell'ordine descritto dal README:

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/03_verifiche.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/05_chat_requests.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/06_chat_recipient.sql
```

Gli script non sostituiscono backup/restore; verificare i prerequisiti prima di
applicarli. Non eseguire operazioni distruttive in produzione.

## Build, test e avvio

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Il test di caricamento app richiede datasource configurato. La CI `Backend CI`
fornisce PostgreSQL 18 effimero ed esegue baseline SQL e test. Se l'accesso locale
è bloccato, registrarlo BLOCKED; non saltare silenziosamente i test DB.

## Avvio chatbot

Da shell nella directory `chatbot`:

```powershell
python -m unittest discover
python -m fornitori_chatbot
```

La console usa il backend fake. Per backend HTTP configurare URL e token come da
[`chatbot/README.md`](../../chatbot/README.md); non inserire token nel comando o nel
repository. Per Telegram il provider richiede segreto del bot gestito fuori dal repo.
La guida del modulo contiene i parametri esatti per shell POSIX/Windows.

## Architettura e procedure

Il backend è organizzato in `controller`, `dto`, `entity`, `enums`, `repository`,
`service`, `config`, `common`. OpenAPI è il contratto; nessun client deve interrogare
il DB. Branch e PR seguono [`07_GIT_WORKFLOW.md`](../07_GIT_WORKFLOW.md). Per una
modifica:

1. verificare story/requisito, owner e test esistenti;
2. accordare i cambi API/schema con consumer;
3. modificare codice, test e documenti correlati nello stesso changeset;
4. eseguire `.\mvnw.cmd clean verify`, test chatbot e `git diff --check` pertinenti;
5. confrontare l'esito con quello locale e CI, senza dichiarare PASS per parti non eseguite.

## Problemi frequenti

| Sintomo | Verifica |
|---|---|
| build richiede release Java diversa | controllare `java -version`, `JAVA_HOME`; non abbassare POM |
| backend non avvia | verificare PostgreSQL, variabili datasource, SQL applicato e `ddl-auto=validate` |
| `fe_sendauth: no password supplied` | credenziale non configurata; chiedere accesso, non cambiare auth server |
| API risponde `401/403` | verificare sessione/ruolo; per `/chat/*` usare service token |
| mutazione rifiutata CSRF | eseguire prima `GET /auth/csrf`, inviare header conforme |
| bot non raggiunge backend | controllare `FORNITORI_API_URL`, token e schema SQL chatbot |
| mappa/webhook non disponibile | progettata, non runtime; non è errore di configurazione locale |

Frontend non esistente: non è possibile documentare comandi o strumenti di avvio.

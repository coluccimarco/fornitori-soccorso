# Report Sprint 1 — backend e database

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend & Database Lead<br>
**Indice:** repository · modifiche · database · API · test · rischi · team · review

## A. Analisi del repository

- Branch creato: `feature/backend`; branch iniziale `main`, senza commit locali.
- Stato iniziale: progetto Spring Boot minimo con una classe applicativa, un test
  context-load e un `pom.xml`; nessun endpoint o script DB nel repository.
- Stack dichiarato inizialmente: Maven, Spring Boot, Java 21 e PostgreSQL. Il
  `pom.xml` locale è stato modificato a Java 25 durante il lavoro e tale modifica
  è stata preservata.
- La cartella iniziale `target/` non era tracciata; ora è esclusa da `.gitignore`.

## B. Modifiche effettuate

**Aggiunti:** `.gitignore`, `.mvn/wrapper/maven-wrapper.properties`,
`api/openapi.yaml`, `database/01_schema.sql`, `database/02_seed.sql`,
`database/03_verifiche.sql`, `database/04_assert_baseline.sql`,
`docs/API_CONTRACT.md`, `docs/DATABASE_DICTIONARY.md`,
`docs/DECISION_LOG.md`, `docs/SPRINT_1_REPORT.md`, `docs/TAD_FORNITORI.md`,
`docs/TEST_EVIDENCE.md`, `docs/BACKEND_SPRINT1_RUNBOOK.md`,
`README.md`; classi Java organizzate per layer (`controller`, `dto`, `entity`,
`enums`, `repository`, `service`, `config`, `common`); test MVC
`ApiSecurityTests.java`.

**Modificati:** `src/main/resources/application.properties` (datasource da
ambiente, validazione Hibernate, cookie sessione), test context-load (condizionato
alla presenza della password DB).

Il POM preesistente dichiarava già Java 25; è stato completato con
`maven.compiler.release=25`. La versione Spring Boot 4.1.2 presente è stata
mantenuta senza migrazioni automatiche.

## C. Database

È stato integrato il modello relazionale del PM Kit per 13 tabelle, con FK, chiavi
univoche, check, indici, `NUMERIC`, `TIMESTAMPTZ`, fixture storiche e outbox. Il
seed contiene le righe di esempio con hash BCrypt; gli script sono ripetibili senza
cancellazioni e riallineano le sequenze identity. L'assert SQL confronta conteggi,
riepilogo e guadagni per autista.

Conteggi e totale sono stati verificati direttamente nei fogli Excel. PostgreSQL
18 è in esecuzione in locale e la porta `localhost:5432` è raggiungibile, ma
l'autenticazione dell'utente applicativo non è stata verificata: il tentativo senza
password ha restituito `fe_sendauth: no password supplied`. Gli script SQL non
sono stati eseguiti sul DB locale. La baseline è stata invece eseguita con esito
positivo nel job PostgreSQL effimero di GitHub Actions.

## D. API

Implementati: login/logout/me/CSRF, lettura di autisti, mezzi, compagnie, clienti
(ricerca nome/targa e inserimento responsabile) e officine. Persistenza via JPA;
disponibilità autista calcolata solo sulle missioni operative. Le API missioni,
dashboard, mappa e webhook restano progettate e sono marcate come tali in OpenAPI.

## E. Test

I conteggi Excel e i valori riepilogativi sono stati verificati sulla sorgente;
anche i 10 hash BCrypt sono stati confrontati con i valori del foglio senza
stampare o versionare password in chiaro. Eclipse Temurin 25.0.5 è stato scaricato
da Adoptium e verificato tramite SHA-256. `.\mvnw.cmd clean verify` su JDK 25 ha
esito BUILD SUCCESS: 6 test MVC passati, test context-load saltato senza
`SPRING_DATASOURCE_PASSWORD`. La baseline DB locale non è stata eseguita; i job
Java 25 e PostgreSQL di GitHub Actions sono passati. Vedi
[TEST_EVIDENCE.md](./TEST_EVIDENCE.md).

## F. Problemi e decisioni aperte

- Import e baseline DB non eseguiti localmente perché le credenziali applicative
  non sono disponibili; servizio PostgreSQL 18 attivo e porta raggiungibile.
- Build `clean verify` su JDK 25 PASS; job GitHub Actions Java 25 e PostgreSQL PASS.
- L'Excel sceglie l'ora di accettazione per la maggiorazione notturna; il PDF non
  definisce quale timestamp usare. Decisione provvisoria registrata.
- Il foglio missioni ha due missioni attive per l'autista 3; i record restano
  invariati e separati dall'operatività live.
- Alla chiusura originaria dello Sprint 1 non erano stati eseguiti commit, push o
  merge; il successivo versionamento è gestito separatamente e non cambia lo stato
  tecnico descritto in questo report.

## G. Integrazioni M2–M5

- **M2:** può usare il login JSON e le API anagrafiche implementate; deve gestire
  CSRF e i codici `401`/`403`/`409`.
- **M3:** ricerca targa disponibile via clienti; missione e webhook non sono
  ancora operativi.
- **M4:** coordinate basi/officine esposte; calcolo percorso non implementato.
- **M5:** outbox predisposta; eventi e invio non implementati.

## H. Stato Sprint

- **Completato nel codice:** schema/seed/assertion, sessione e ruoli, DTO e query
  JPA anagrafiche, contratto API, documentazione.
- **Parziale/non verificato:** runtime endpoint contro PostgreSQL locale, import
  locale e baseline locale; build Java 25 e sei test MVC sono PASS.
- **Rinviato a Sprint 2:** workflow missioni, timeout, pricing, concorrenza,
  idempotenza e test end-to-end.
- **Rinviato ad altri membri:** chatbot, mappe e notifiche.

## I. Demo Sprint Review (5–7 minuti)

1. Configurare una password PostgreSQL locale e avviare il database.
2. Eseguire i quattro script SQL; mostrare il successo dell'assert baseline.
3. Avviare il backend con `.\mvnw.cmd spring-boot:run`.
4. Recuperare un CSRF token da `/api/v1/auth/csrf`, effettuare login responsabile
   usando una credenziale presente solo nel foglio locale del docente, e chiamare
   `/api/v1/auth/me`.
5. Consultare autisti, clienti filtrati per targa, mezzi, compagnie e officine.
6. In sessione autista provare `/api/v1/clienti` e mostrare `403`; senza sessione
   mostrare `401`.
7. Mostrare OpenAPI e indicare chiaramente le API missioni non ancora implementate.

## J. Sprint 2 consigliato

Implementare una verticale di missione con `Clock` iniettato, transazioni e lock,
proposte con deadline a 10 minuti, job per scadenze live, controlli al momento
dell'accettazione, rifiuto/riproposizione con storico, chiusura idempotente e costo
`BigDecimal`. Aggiungere test di integrazione PostgreSQL per concorrenza, timeout,
ruoli, notifica outbox e arrotondamenti. Confermare col docente il timestamp tariffario.

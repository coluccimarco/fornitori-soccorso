# Evidenze di test

**Versione:** 1.2 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend & Database Lead.<br>
**Indice:** environment, dataset, build, database, contratto. Fonte dei dettagli:
[report Sprint 1](./SPRINT_1_REPORT.md) e [report Sprint 2](./SPRINT_2_REPORT.md).

I valori attesi Excel non sono prova di un'importazione PostgreSQL riuscita.

| Controllo | Evidenza | Esito |
|---|---|---|
| Branch e stato Git | `feature/backend`; file sorgente iniziale preservato | PASS |
| Conteggi dati Excel | Lettura diretta dei fogli: 10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti, 6 officine, 30 missioni | PASS (solo sorgente Excel) |
| Confronto seed/Excel | Confrontati tutti i campi seed con le righe Excel; nessuna differenza rilevata | PASS (confronto sorgente, non import DB) |
| Riepilogo Excel | 16 eseguite, 6 pending, 4 scadute, 4 non accettate, EUR 1.509,71 | PASS (solo sorgente Excel) |
| Verifica hash BCrypt | 10 hash seed confrontati con le password del foglio locale; nessun valore in chiaro stampato o aggiunto ai test | PASS |
| Servizio PostgreSQL locale | PostgreSQL 18 in esecuzione; porta TCP `localhost:5432` raggiungibile | PASS |
| Credenziali PostgreSQL applicative | Autenticazione rifiutata: `fe_sendauth: no password supplied`; credenziali non configurate per questa verifica; server non modificato | BLOCKED |
| PostgreSQL effimero con Docker locale | Docker Desktop Engine risponde HTTP 500 sulla API; nessun container o database locale è stato creato | BLOCKED |
| Esecuzione schema, seed e assertion in PostgreSQL | Richiede credenziali DB; non eseguita | BLOCKED |
| JDK locale | Eclipse Temurin 25.0.5+8 LTS installato in `%USERPROFILE%\.jdks\temurin-25.0.5+8`; archivio ufficiale verificato con SHA-256 prima dell'estrazione | PASS |
| Build e test Java 25 | `.\mvnw.cmd clean verify` con `JAVA_HOME` Temurin 25.0.5: BUILD SUCCESS; 17 test totali, 0 falliti, 0 errori, 4 saltati per DB non configurato | PASS (unit/MVC; DB BLOCKED) |
| Test login, errore password, 401/403, CSRF, sessione, logout e route missioni | `ApiSecurityTests`: 11 superati, 0 falliti, 0 errori | PASS (JDK 25.0.5) |
| Test di caricamento app con database locale | 1 test condizionato non eseguito senza `SPRING_DATASOURCE_PASSWORD`; il servizio è attivo ma l'accesso dell'app non è stato autenticato | BLOCKED |
| Test mirati Sprint 2 (Java 25.0.5) | `MissionServiceTests`, `PricingServiceTests`, `ApiSecurityTests`: 13 superati; 0 falliti, 0 errori | PASS (unit/MVC) |
| Copertura integrazione PostgreSQL Sprint 2 | Flusso missione, note, rettifica/dashboard/storico, reject/riproposizione/scadenza e concorrenza; i 3 test PostgreSQL sono saltati localmente e la CI sul branch non è ancora stata eseguita | BLOCKED |
| Test dati PostgreSQL, filtri, FK e ripetibilità | Richiedono database autenticato; non eseguiti | BLOCKED |
| Sicurezza password | 10 hash BCrypt validati in precedenza; nessun DTO include password/hash | PASS (verifica sorgente precedente) |
| OpenAPI | YAML parsato; 31 operazioni con status/ruolo validi. 10 Sprint 1 implementate, 13 Sprint 2 ancora `designed` fino alla verifica CI, 8 pianificate | PASS (metadata); runtime Sprint 2 NOT RUN |
| Java package layout | Confrontati package dichiarati e directory sorgente; target Java 25 corretto | PASS |
| Git diff whitespace | `git diff --check` | PASS |

Comandi previsti:

```powershell
.\mvnw.cmd clean verify
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
```

Non dichiarare superati i test DB Sprint 2 finché il job PostgreSQL CI e
`04_assert_baseline.sql` non terminano con successo. La verifica di connessione
locale senza password ha restituito `fe_sendauth: no password supplied`; non sono
state modificate configurazione o dati del server.

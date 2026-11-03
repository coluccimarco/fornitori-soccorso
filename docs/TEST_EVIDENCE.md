# Evidenze di test

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend & Database Lead.<br>
**Indice:** environment, dataset, build, database, contratto. Fonte dei dettagli:
[report Sprint 1](./SPRINT_1_REPORT.md).

I valori attesi Excel non sono prova di un'importazione PostgreSQL riuscita.

| Controllo | Evidenza | Esito |
|---|---|---|
| Branch e stato Git | `feature/backend`; file sorgente iniziale preservato | PASS |
| Conteggi dati Excel | Lettura diretta dei fogli: 10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti, 6 officine, 30 missioni | PASS (solo sorgente Excel) |
| Confronto seed/Excel | Confrontati tutti i campi seed con le righe Excel; nessuna differenza rilevata | PASS (confronto sorgente, non import DB) |
| Riepilogo Excel | 16 eseguite, 6 pending, 4 scadute, 4 non accettate, EUR 1.509,71 | PASS (solo sorgente Excel) |
| Verifica hash BCrypt | 10 hash seed confrontati con le password del foglio locale; nessun valore in chiaro stampato o aggiunto ai test | PASS |
| Credenziali PostgreSQL | Connessione locale rifiutata: `fe_sendauth: no password supplied`; non è stata modificata la configurazione server | BLOCKED |
| PostgreSQL effimero con Docker locale | Docker Desktop Engine risponde HTTP 500 sulla API; nessun container o database locale è stato creato | BLOCKED |
| Esecuzione schema, seed e assertion in PostgreSQL | Richiede credenziali DB; non eseguita | BLOCKED |
| JDK locale | Eclipse Temurin 25.0.5+8 LTS installato in `%USERPROFILE%\.jdks\temurin-25.0.5+8`; archivio ufficiale verificato con SHA-256 prima dell'estrazione | PASS |
| Build e test Java 25 | `.\mvnw.cmd clean verify` con `JAVA_HOME` Temurin 25.0.5: BUILD SUCCESS | PASS |
| Test login, errore password, 401/403, CSRF, sessione e logout | `ApiSecurityTests`: 6 superati, 0 falliti, 0 errori | PASS (JDK 25.0.5) |
| Test di caricamento app con database | 1 test condizionato non eseguito senza `SPRING_DATASOURCE_PASSWORD` | BLOCKED |
| Test dati PostgreSQL, filtri, FK e ripetibilità | Richiedono database autenticato; non eseguiti | BLOCKED |
| Sicurezza password | 10 hash BCrypt validati in precedenza; nessun DTO include password/hash | PASS (verifica sorgente precedente) |
| OpenAPI | File YAML parsato; 10 operazioni implementate e 21 progettate, tutte con status e ruolo | PASS (contratto; non test runtime per le progettate) |
| Git diff whitespace | `git diff --check` | PASS (stato al controllo iniziale; ripetere prima del commit) |

Comandi previsti:

```powershell
.\mvnw.cmd clean verify
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
```

Non dichiarare superata la baseline DB finché `04_assert_baseline.sql` non termina
con successo su un database caricato.

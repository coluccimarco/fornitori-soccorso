# Build, configurazione e deployment

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)

## Stato

Avvio locale e pipeline GitHub Actions sono disponibili. Container, staging, produzione e osservabilità non
sono configurati e sono fuori ambito.

## Avvio locale

Prerequisiti: Java 25, PostgreSQL 18 e Maven Wrapper. Crea un database demo e applica `database/01..08` come
indicato nel [README](../README.md). Quindi:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<secret locale>"
$env:SESSION_COOKIE_SECURE = "false"
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Per HTTPS in un ambiente remoto usare il profilo `prod` e TLS al reverse proxy. Hibernate `validate` richiede
lo schema già applicato.

## Pipeline

`.github/workflows/backend-ci.yml` usa Ubuntu, Temurin 25 e un PostgreSQL 18 temporaneo: compila il progetto,
controlla le dipendenze e la documentazione, applica gli script e verifica l'avvio dell'applicazione con un
ruolo senza privilegi di amministratore. La password usata è fittizia e limitata al servizio effimero.

## Fasi future

1. Decidere target, rete, backup e migrazioni.
2. Iniettare i segreti da un secret manager, mai da file versionati.
3. Abilitare TLS, cookie Secure, health check, metriche, log strutturati e alert.
4. Provare backup, restore e rollback.

**Riferimenti:** [sicurezza](./architecture/SECURITY_DESIGN.md), [rischi](./management/RISK_REGISTER.md).

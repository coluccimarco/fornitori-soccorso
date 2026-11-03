# Build, configurazione e deployment

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend/DevOps<br>
**Indice:** locale · configurazione · build · ambienti · stato deployment

## Scopo e stato

Le istruzioni locali e CI sono **IMPLEMENTATE**. Container, staging, produzione,
segreti runtime e osservabilità sono **PIANIFICATI**, non configurati.

## Avvio locale

Prerequisiti: Java 25 LTS, PostgreSQL 18 compatibile e Maven Wrapper. Crea un DB
demo isolato, imposta:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<secret locale>"
$env:SESSION_COOKIE_SECURE = "false"
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Per HTTPS in un ambiente remoto impostare cookie Secure e TLS al reverse proxy.
Hibernate `validate` richiede schema già applicato.

## CI

`.github/workflows/backend-ci.yml` usa GitHub-hosted Ubuntu, Temurin 25, cache
Maven, build/test e un job PostgreSQL temporaneo. La password CI è fittizia,
limitata al service effimero; non riutilizzarla in alcun ambiente persistente.

## Future fasi operative

1. Decidere target (VM/container/servizio gestito), rete, backup e migrazioni.
2. Iniettare segreti da secret manager, mai da file committed.
3. Abilitare TLS, cookie Secure, health checks, metriche/log strutturati e alert.
4. Eseguire backup/restore test e deployment rollback.
5. Promuovere solo artefatti provenienti da CI verde e taggare dopo approvazione.

**Dipendenze:** DB provisionato, JDK 25, segreti autorizzati, decisione hosting.
**Criterio:** ambiente riproducibile e testato, piano rollback/backup verificato.
**Riferimenti:** [README](../README.md),
[sicurezza](./architecture/SECURITY_DESIGN.md),
[rischi](./management/RISK_REGISTER.md).

# Sicurezza, privacy e segreti

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend/Security Owner<br>
**Indice:** implementato · accesso · segreti · dati · webhook · criteri

## Scopo e stato

Documenta controlli presenti e requisiti per integrazioni future. Sessione,
BCrypt, CSRF e ruoli sono **IMPLEMENTATI** nel backend; security review dei flussi
completi e webhook sono **DA VALIDARE/PIANIFICATI**.

## Controlli attuali

- Login JSON e Spring Security con sessione server-side; cookie HttpOnly, SameSite
  Lax, `Secure` configurabile per HTTPS.
- Password nel DB solo come hash BCrypt; DTO non restituiscono hash.
- CSRF attivo per operazioni mutative; token da `GET /api/v1/auth/csrf`.
- Accesso centrale `RESPONSABILE`; dati anagrafici non esposti ad AUTISTA.
- Validazione request e handler d'errore centralizzato. Non loggare body/password.

## Configurazione e dati

Datasource da env `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`. Non committare `.env`, token, dump locali o file Excel
personali. Rotazione credenziali via ambiente; produzione deve usare HTTPS e
`SESSION_COOKIE_SECURE=true`. Limita accesso a telefoni, targhe e polizze secondo
necessità del ruolo.

## Futuri webhook e provider

Verificare firma/secret sul raw payload, timestamp/anti-replay, idempotency key,
limiti dimensione/rate, allowlist dove possibile e logging senza PII superflua.
Segreti provider solo in secret manager o environment protetto; mai in documenti,
esempi JSON, issue o commit. Retry deve essere idempotente.

**Dipendenze:** config per ambiente e review M1.<br>
**Completamento:** test 401/403/CSRF, nessun segreto nel diff, autorizzazioni
server-side, webhook validato, dati minimizzati e logging verificato.<br>
**Riferimenti:** [API](./04_API_REST.md), [deployment](./10_DEPLOYMENT.md),
[notifiche](./team/MEMBRO_5_NOTIFICATIONS.md).

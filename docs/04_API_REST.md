# Contratto API REST

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — API Owner<br>
**Indice:** fonte normativa · sessione · endpoint · errori · integrazione

## Scopo e stato

`api/openapi.yaml` è la sola fonte normativa di URL, DTO e stati. Sprint 1:
10 operazioni **IMPLEMENTATE**; le 13 operazioni del backend missioni Sprint 2
sono implementate nel branch `feature/backend-sprint2` ma restano marcate
`designed` fino al passaggio dei test PostgreSQL CI. Le restanti 8 operazioni
sono pianificate. Usa `x-implementation-status` per distinguere gli stati.

## Auth/sessione

Le sessioni sono server-side. Il browser chiama `GET /api/v1/auth/csrf`, conserva
il cookie di sessione e invia il token nel header `X-CSRF-TOKEN` per POST/PATCH.
`POST /api/v1/auth/login` riceve JSON email/password; non usare Basic Auth.
Credenziali invalide: 401. Tutti i payload sono JSON, tranne `204` logout.

## Endpoint implementati

| Endpoint | Ruolo | Stato |
|---|---|---|
| GET `/api/v1/auth/csrf` | Pubblico | IMPLEMENTATO |
| POST `/api/v1/auth/login` | Pubblico + CSRF | IMPLEMENTATO |
| GET `/api/v1/auth/me`, POST `/api/v1/auth/logout` | Autenticato | IMPLEMENTATO |
| GET `/api/v1/autisti`, `/mezzi`, `/compagnie`, `/officine` | RESPONSABILE | IMPLEMENTATO |
| GET `/api/v1/clienti?search=...` | RESPONSABILE | IMPLEMENTATO |
| POST `/api/v1/clienti` | RESPONSABILE + CSRF | IMPLEMENTATO |

Le operazioni missioni, dashboard e storico autista sono implementate nel branch
Sprint 2 ma non vanno considerate approvate per i consumer prima della verifica DB
CI. Route, webhook e chat restano pianificati. Consultare lo YAML per request,
response, security e codici; non inventare alias o payload locali.

## Errori e criteri d'integrazione

`400` input non valido, `401` sessione/credenziali, `403` ruolo insufficiente,
`409` conflitto. Le risposte d'errore non devono esporre stack, password o dati
interni. Il client gestisce loading, timeout, sessione scaduta e retry idempotenti.
Ogni modifica breaking richiede review M1 e aggiornamento di tutti i consumer.

**Dipendenze:** sessione e CSRF, autorizzazione per ruolo, OpenAPI.<br>
**Completamento:** endpoint chiamato corrisponde a OpenAPI, test include successi e
errori, consumer approvano le variazioni.<br>
**Riferimenti:** [contratto per membri](./API_CONTRACT.md),
[integrazioni](./team/INTEGRATION_CONTRACTS.md), [sicurezza](./06_SICUREZZA.md).

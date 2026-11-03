# Membro 1 — Backend & Database Lead

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1<br>
**Indice:** responsabilità · layer · presente · Sprint 2 · test

## Scopo e responsabilità

Owner di Spring, PostgreSQL, OpenAPI e integrazione. Coordina `pom.xml`, schema,
seed e contratti. Java 25 LTS; Spring Boot 4.1.2 attuale; Maven Wrapper 3.9.9.

## Struttura

`controller` valida HTTP e delega; `dto` definisce payload; `entity` mappa DB;
`repository` esegue query; `service` contiene use case/transazioni; `enums` definisce
valori stabili; `config` configura sessione/CSRF/ruoli; `common` normalizza errori.
Non esporre entity, hash o credenziali.

## Disponibile — IMPLEMENTATO

Login JSON, CSRF, logout, profilo, sessione HttpOnly, ruoli e API anagrafiche:
autisti, mezzi, compagnie, clienti (ricerca e inserimento), officine. Configura
PostgreSQL da environment; applica script `database/01...04` in ordine.

## Sprint 2 — IMPLEMENTATO IN `main`

- Missioni, proposte e storico proposte; transizioni transazionali.
- Timeout da `pricing_config`, scheduler live-only e guard al momento dell'accept.
- Lock pessimista missione/autista; indice DB contro doppie assegnazioni.
- Chiusura con tariffa server-side, `BigDecimal`, `HALF_UP` e rettifica motivata con audit.
- Dashboard reference/live, totale e subtotali per autista; fixture escluse dal live.
- `Clock` iniettato per deadline; timezone tariffaria Europe/Rome.
- Note e API autista con verifica dell'identità lato server.
- Test unitari/MVC e test PostgreSQL end-to-end/concorrenza passati in CI;
  la verifica PostgreSQL locale resta bloccata dall'assenza di credenziali.

Le 13 operazioni backend missione del report storico sono state integrate in
`main` tramite PR #3. Il job CI latest `41656606659` su `f763d53` è verde su Java 25
e PostgreSQL 18. Il conteggio corrente è 34 operation (30 implementate, 4
progettate), incluse geocodifica e routing backend.

## Logging, test e Definition of Done

Log strutturati senza password/token/body PII; errori inattesi con correlation ID
se introdotto, messaggi API generici. Prima di ogni PR: JDK25
`.\mvnw.cmd clean verify`, test PostgreSQL, diff check e OpenAPI.

**Dipendenze:** PostgreSQL, feedback tariffario, consumer M2–M5.
**Completamento backend:** test PostgreSQL CI PASS; 13 operazioni OpenAPI
promosse a `implemented`. **Riferimenti:**
[runbook](../BACKEND_SPRINT1_RUNBOOK.md),
[regole](../requirements/BUSINESS_RULES.md).

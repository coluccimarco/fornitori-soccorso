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

## Sprint 2 — PIANIFICATO

- Missioni nuove, proposte e storico proposte; transizioni atomiche.
- Timeout di 10 minuti; scheduler live-only e guard al momento dell'accept.
- Lock pessimista/ottimista e indice DB contro autisti doppiamente occupati.
- Chiusura OK, km effettivi, `BigDecimal`, rounding e rettifica con audit.
- Dashboard reference/live con conteggi eseguite, pendenti, scadute e rifiutate,
  totale e subtotali per autista; escludere fixture dal live.
- `Clock` iniettato per deadline e fascia notturna.
- Test DB di concorrenza, idempotenza, autorizzazioni e baseline fixture.

## Logging, test e Definition of Done

Log strutturati senza password/token/body PII; errori inattesi con correlation ID
se introdotto, messaggi API generici. Prima di ogni PR: JDK25
`.\mvnw.cmd clean verify`, DB test se tocca persistenza, diff check e OpenAPI.

**Dipendenze:** PostgreSQL, feedback tariffario, consumer M2–M5.
**Risultato atteso Sprint 2:** mission workflow provato su PostgreSQL, API aggiornate,
nessuna fixture mutata. **Riferimenti:** [runbook](../BACKEND_SPRINT1_RUNBOOK.md),
[regole](../05_BUSINESS_RULES.md).

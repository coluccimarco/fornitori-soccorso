# Sprint 1 — Fondamenta

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Scrum Master<br>
**Indice:** goal · work · stato effettivo · acceptance · demo · rischi

## Sprint Goal

Lasciare una base condivisa: schema/seed, backend sessione e anagrafiche, contratti
e ambiente Java 25 verificabile.

## User stories e attività

| Attività | Owner | Dipendenza | Esito verificato |
|---|---|---|---|
| Strutturare schema/seed/assertion e preserving fixture | M1 | Dataset corso | Script presenti; DB locale BLOCKED |
| Login JSON, CSRF, sessione e ruoli | M1 | Schema utenti | Implementato; test MVC PASS |
| Consultare anagrafiche e creare cliente | M1 | JPA/DB | Codice implementato; runtime DB DA VALIDARE |
| Stabilire OpenAPI per feature presenti/future | M1 + consumer | Requisiti | YAML con 10 implemented e 21 designed |
| Uniformare Java 25 e test | M1 | JDK 25 | `clean verify` PASS su Temurin 25.0.5 |
| CI con service PostgreSQL | M1 | GitHub Actions | Workflow creato; esito remoto DA VALIDARE |
| Preparare onboarding e contratti team | Tutti | Ruoli/requirements | Documentazione in questo branch; review team DA FARE |

Stime iniziali indicative: backend/schema 5–7 h, test/CI 2–3 h, contratti/onboarding
2–4 h, con parallelismo e review. Non sono timesheet o consuntivi.

## Acceptance criteria / Definition of Done

- Java 25 `clean verify` verde.
- Sei test web passano; database baseline passa su PostgreSQL effimero CI.
- Endpoint implementati marcati correttamente e API future non dichiarate operative.
- Ogni membro trova guide e primo task.
- PR reviewata e CI verde.

## Stato e demo

**Implementato localmente:** schema/seed/OpenAPI/backend, package per layer,
Java25 build/test, documentazione e workflow CI. **Bloccato/non validato:** run DB
locale senza credenziali, risultato workflow GitHub e conferma dei consumer.
Script demo: mostra `clean verify`, login MVC, il contratto implemented/designed e
gli script di baseline; non dichiarare l'assertion locale superata.

**Rischi:** credenziali DB, decisione timestamp tariffario, status CI remote.
**Passaggio a Sprint 2:** feedback regole di tariffa e prova dei quattro script DB.
**Riferimenti:** [report](../SPRINT_1_REPORT.md),
[evidenze](../quality/TEST_EVIDENCE.md).

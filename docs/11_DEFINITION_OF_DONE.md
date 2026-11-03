# Definition of Done

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Team Lead — Membro 1<br>
**Indice:** task · codice · API/DB · test · review · release

## Scopo

Checklist di completamento comune; adattare i test al tipo di deliverable senza
marcare come completi controlli non eseguiti.

## Criteri obbligatori

- Requisito/issue e acceptance criteria sono soddisfatti e tracciabili.
- Implementazione rispetta package e ownership; errori sono espliciti, nessun
  fallback ingannevole o dato sensibile nei log.
- DTO e OpenAPI aggiornati; consumer informati su breaking changes.
- Dati/migrazioni preservano fixture e vincoli; script hanno strategia di rollback.
- Test pertinenti passano su Java 25; `clean verify`, lint/quality e `git diff --check`
  passano. DB/servizi esterni testati o dichiarati BLOCKED con motivazione.
- Documentazione aggiornata; PASS/FAIL/BLOCKED/NOT RUN distinti.
- PR ha diff controllato, nessun segreto, review approvata e CI verde.
- Demo o evidenza verificabile preparata per la review.

## Stato iniziale

Sprint 1: baseline applicativa implementata; Java 25 `clean verify` PASS; test DB
locale BLOCKED. La CI aggiunta non è considerata PASS finché non termina su GitHub.
La review esterna/PR è da eseguire.

**Dipendenze:** CI, code owner e criteri specifici della story.<br>
**Completamento:** ogni checkbox applicabile è soddisfatta e documentata.<br>
**Riferimenti:** [Testing QA](./09_TESTING_QA.md), [Git workflow](./07_GIT_WORKFLOW.md).

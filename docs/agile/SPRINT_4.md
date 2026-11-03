# Sprint 4 — QA e presentazione

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Scrum Master<br>
**Indice:** goal · attività · acceptance · demo · rischio · release gate

## Sprint Goal

Validare una build integrata, correggere i difetti e preparare una demo onesta,
ripetibile e documentata. Questo è un obiettivo di piano, non evidenza che il
collaudo o la demo siano stati eseguiti.

## Attività

| Attività | Owner | Dipendenza |
|---|---|---|
| End-to-end centrale/autista, fixture/live e regressione | M1 + M2 | Sprint 2 |
| Chatbot & webhook privacy, retry, duplicate delivery | M3 + M5 | Sprint 3 |
| Map route, coordinate, fallback provider | M4 + M2 | Sprint 3 |
| Cross-browser, accessibilità e responsive | M2 | UI complete |
| Security review e secret scan; test backup/rollback design | M1 + tutti | deployment target |
| Bug triage, docs, demo script e rehearsal | Tutti | integrazione completa |

## Acceptance / DoD

CI Java25 verde; DB PostgreSQL baseline verificata; P0/P1 chiuse o motivate; nessun
segreto; permessi/casi negativi provati; manuale/setup ripetibile; demo eseguibile
offline o con provider sandbox. Tutti i test distinguono fixture e live. Release tag
solo dopo approvazione; nessun tag definitivo se quality gate è bloccato.

## Demo e rischi

Sequenza: login, missione, assegnazione/accettazione, route, chiusura, costo,
notifica, dashboard e casi 401/403/timeout. Rischi: stabilità rete, dati sensibili,
dipendenze provider. Preparare fallback registrato senza fingere live integration.

**Dipendenze:** Sprint 1–3 e ambiente integrato. **Passaggio:** review finale e
sign-off DoD. **Riferimenti:** [QA](../quality/TEST_PLAN.md), [deployment](../10_DEPLOYMENT.md).

### Nota di riconciliazione — 03/11/2026

La baseline `main` contiene ora il client web e l'API di routing con test CI;
questo non sostituisce browser E2E, provider live, verifica visiva né sign-off
docente. Il piano di Sprint 4 resta un piano storico, non un verbale.

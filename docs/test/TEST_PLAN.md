# Test Plan FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — QA Coordinator<br>
**Indice:** scope · ambienti · livelli · exit criteria · evidenze

## Scope e stato

Copertura pianificata per backend, DB e i quattro componenti. Oggi sono implementati
i test MVC auth/ruoli; integrazione DB in locale **BLOCKED** senza credenziali.
JUnit/Maven con Java 25 è il runner backend.

## Ambienti

- Local developer: PostgreSQL isolato, env segreti non versionati.
- CI: Java Temurin 25 e PostgreSQL 18 effimero, seed/ assertion caricati dal workflow.
- External integrations: fake adapter default; sandbox solo con autorizzazione.
- Browser matrix e provider reali da concordare nel Sprint 4.

## Test levels

| Livello | Scopo | Esempi |
|---|---|---|
| Unit | Logica senza I/O | transizioni, pricing, normalizzazione |
| Web slice | HTTP/security | 401/403, CSRF, login/logout, DTO |
| DB integration | schema e transazioni | FK, unique index, fixture/live, concorrenza |
| Contract | compatibilità consumer | request/response con OpenAPI |
| E2E | flussi browser e canali | centrale→autista, chatbot, route, notifica |

## Exit criteria

Tutti i P0 passano, nessun secret, baseline di riferimento invariata, CI Java25 e
PostgreSQL verde, blocchi documentati, demo riproducibile. Test di costo e
assegnazione devono usare clock/fixture deterministici.

**Dipendenze:** CI, dataset autorizzato, endpoint disponibili. **Riferimenti:**
[casi](./TEST_CASES.md), [evidenze](../TEST_EVIDENCE.md), [DoD](../11_DEFINITION_OF_DONE.md).

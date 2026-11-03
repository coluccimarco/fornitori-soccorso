# Registro delle decisioni

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Architecture Owner<br>
**Indice:** fonte canonica · decisioni attive · processo revisione

## Scopo

Il registro storico già presente in [`DECISION_LOG.md`](./DECISION_LOG.md) è la
fonte canonica per le decisioni D-001–D-006. Non duplicare le decisioni: aggiungere
nuovi record lì con data, contesto, scelta, conseguenze e stato.

## Decisioni correnti

- Storico docente `is_fixture=TRUE`, separato dall'operatività live.
- Timestamp tariffario: accettazione, implementato ma **DA CONFERMARE** col docente.
- Importi con `BigDecimal`; rounding finale `HALF_UP` testato sui casi unitari.
- Outbox per eventi accept/completamento con `channel=internal`; dispatcher esterno
  ancora fuori perimetro.
- Sessione server-side, login JSON, BCrypt e CSRF.
- Target Java 25 LTS, release Maven 25; Spring Boot 4.1.2 mantenuto.

## Revisione

Una decisione che cambia schema/API/security richiede review M1 e dei consumer
coinvolti. Aggiornare questo indice e la fonte canonica nella stessa PR.

**Criterio:** ogni scelta ambigua ha owner e stato esplicito.<br>
**Dipendenze:** review tecnica e requisiti del corso.<br>
**Riferimenti:** [business rules](./05_BUSINESS_RULES.md),
[rischi](./13_RISCHI_E_IMPEDIMENTI.md).

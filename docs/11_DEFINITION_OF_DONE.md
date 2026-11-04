# Definition of Done

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)

## Criteri

- Requisito e acceptance soddisfatti e tracciabili.
- Implementazione coerente con package e responsabilità; errori espliciti, nessun fallback ingannevole,
  nessun dato sensibile nei log.
- DTO e OpenAPI aggiornati; consumer informati dei cambi incompatibili.
- Dati e migrazioni preservano fixture e vincoli; gli script hanno una strategia di rollback.
- `./mvnw clean package` riuscito su Java 25, applicazione avviata e flusso provato a mano; `git diff --check`
  pulito.
- Documentazione aggiornata.
- PR con diff controllato, nessun segreto, review approvata e pipeline verde.
- Demo preparata per la review.

**Riferimenti:** [Git workflow](./07_GIT_WORKFLOW.md).

# Registro decisioni consolidato

**Codice:** FOR-DEC-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Questo registro sintetizza decisioni/documentazione esistenti. Non attribuisce
approvazione a persone non identificate; i log storici
[DECISION_LOG.md](../DECISION_LOG.md) e [12_DECISION_LOG.md](../12_DECISION_LOG.md)
sono conservati come evidenza.

| ID | Tema | Decisione/baseline documentata | Stato | Fonte |
|---|---|---|---|---|
| D-001 | Stack backend | Java 25, Spring Boot 4.1.2, Maven Wrapper, PostgreSQL 18/compatibile | Implementato; versione da valutare solo con change | `pom.xml`, README |
| D-002 | Tariffa | Seed: €50 uscita, €1,20/km, supplemento 25%; calcolo Java in `Europe/Rome` | Implementato nel servizio; regola docente e timestamp restano da confermare | `database/02_seed.sql`, `PricingService.java`, report Sprint 2 |
| D-003 | Timeout | Seed 10 minuti; scheduler applicativo gestisce scadenze live | Implementato; intervallo effettivo configurabile | SQL seed, `MissionExpiryScheduler.java` |
| D-004 | Storico | `is_fixture` distingue righe didattiche dal lavoro live | Implementato | Schema SQL e servizi missione |
| D-005 | Canale chatbot | Flusso a menu deterministico, senza AI obbligatoria | Implementato per console/Telegram; Gemini facoltativo non implementato | `chatbot/README.md` |
| D-006 | Segreti | Token chatbot fornito tramite variabile d'ambiente condivisa col backend | Implementato come configurazione; rotazione/secret store di produzione non documentati | README chatbot e proprietà backend |
| D-007 | Identità chatbot | README segnala che la verifica basata sulla targa non prova la titolarità | Limite noto; soluzione/approvazione da definire | `chatbot/README.md` |
| D-008 | Telegram | Bot a long polling, notifiche implementate nel modulo | Dichiarato provato su Telegram dal README/commit; CI non è test provider reale | `chatbot/README.md`, history Git |
| D-009 | WhatsApp | Provider e relativo webhook non integrati | Pianificato | OpenAPI `designed`, guide team |
| D-010 | Frontend | Nessuna applicazione browser nella working tree | Non implementato | Inventario repository |
| D-011 | Mappe | API route presente solo come progetto nel contratto | Pianificato | OpenAPI `designed`; nessun modulo frontend/provider |
| D-012 | Docente | File PDF/Excel non disponibili all'audit | Richiesta di acquisizione aperta | Inventory working tree |

## Decisioni da richiedere

- Conferma dei requisiti obbligatori/facoltativi, dataset e acceptance dal docente.
- Quale timestamp determina tariffa notturna e quale versione delle regole è normativa.
- Necessità di UI, mappe, WhatsApp e autenticazione più forte del chatbot.
- Owner del contract source e revisione della descrizione introduttiva OpenAPI.

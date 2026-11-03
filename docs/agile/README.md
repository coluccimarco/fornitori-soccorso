# Registro Scrum e stato degli Sprint

**Baseline verificata:** repository `main` a `25c613b`, 03/11/2026.

Le quattro schede `SPRINT_1.md`–`SPRINT_4.md` descrivono obiettivi e attività, non
sono verbali approvati. Lo stato del codice si è evoluto oltre alcuni riepiloghi
testuali storici:

| Sprint / area | Evidenza Git/repository | Stato verificabile | Verbale e risultati evento |
|---|---|---|---|
| 1 — Fondamenta backend | PR #1 merged; commit backend, SQL, auth/anagrafiche, workflow CI | Funzionalità descritte nel report; run CI latest verde | Date precise, daily/review/retro non reperiti |
| 2 — Workflow missioni | PR #3 merged; controller/service/tests; PostgreSQL job verde | Backend implementato e testato in CI | Non assumere che commit equivalga a review demo |
| 3 — Chat/integrations | Commit chatbot, Telegram, API chat e outbox nella storia di `main`; suite presente; Telegram prova reale dichiarata nel README bot | Bot console/Telegram e supporto backend presenti; mappe e WhatsApp assenti | Verbali/demo review non reperiti |
| 4 — QA/consegna | Nessuna evidenza che l'intero scope UI/mappe sia completato | Stato da verificare; non chiamarlo completato | Review/retrospettiva non reperite |

`PRODUCT_BACKLOG.md` e le schede Sprint sono documenti di pianificazione; non
consuntivi. Non convertire il flusso commit in date, Story Points o meeting
inventati. Un verbale assente si compila con il template, senza backdating.

**Riferimenti:** [piano Scrum](../08_AGILE_SCRUM.md), [backlog](./PRODUCT_BACKLOG.md),
[template daily](./DAILY_SCRUM_TEMPLATE.md), [review](./SPRINT_REVIEW_TEMPLATE.md),
[retro](./RETROSPECTIVE_TEMPLATE.md).

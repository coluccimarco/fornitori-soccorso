# Registro Scrum e stato degli Sprint

**Baseline verificata:** `origin/main` a `f763d53`, 03/11/2026.

Le quattro schede `SPRINT_1.md`–`SPRINT_4.md` descrivono obiettivi e attività, non
sono verbali approvati. Lo stato del codice si è evoluto oltre alcuni riepiloghi
testuali storici:

| Sprint / area | Evidenza Git/repository | Stato verificabile | Verbale e risultati evento |
|---|---|---|---|
| 1 — Fondamenta backend | PR #1 merged; commit backend, SQL, auth/anagrafiche, workflow CI | Funzionalità descritte nel report; run CI latest verde | Date precise, daily/review/retro non reperiti |
| 2 — Workflow missioni | PR #3 merged; controller/service/tests; PostgreSQL job verde | Backend implementato e testato in CI | Non assumere che commit equivalga a review demo |
| 3 — Chat/integrations | Bot console/Telegram, API chat/outbox, MapController/MapService, Nominatim/OSRM e client web integrati in `main` | API mappe implementata e test CI verde; UI presente ma senza mappa; browser E2E e provider live NOT RUN | Verbali/demo review non reperiti |
| 4 — QA/consegna | CI run `41656606659` su `f763d53`; fonti docente e acceptance non disponibili | Verifica automatica backend PASS; collaudo UI/provider e sign-off mancanti | Review/retrospettiva non reperite |

`PRODUCT_BACKLOG.md` e le schede Sprint sono documenti di pianificazione; non
consuntivi. Non convertire il flusso commit in date, Story Points o meeting
inventati. Un verbale assente si compila con il template, senza backdating.

**Riferimenti:** [backlog](./PRODUCT_BACKLOG.md),
[template](./TEMPLATES.md), [evidenze di test](../quality/TEST_EVIDENCE.md).

## Obiettivi e stato

| Sprint / area | Stato verificabile | Limiti dell'evidenza |
|---|---|---|
| 1 — Fondamenta | Backend, SQL, auth/anagrafiche e workflow CI presenti; dettagli nel [report](../SPRINT_1_REPORT.md) | Report storico; CI latest non sostituisce ogni test locale |
| 2 — Missioni | Workflow missioni e test PostgreSQL CI presenti; dettagli nel [report](../SPRINT_2_REPORT.md) | Un merge o commit non prova demo/review |
| 3 — Integrazioni | Chatbot/Telegram, UI e API mappe presenti in baseline; vista mappa assente | Browser E2E e provider live NOT RUN; WhatsApp non implementato |
| 4 — QA/consegna | CI Java/OpenAPI e PostgreSQL verde sul run `41656606659` | Requisiti originali, sign-off e collaudo UI/provider mancanti |

Le schede Sprint sono un piano adattato, non consuntivi approvati. Lo schema
storico ipotizzava quattro Sprint giornalieri; non dimostra durata o date effettive.
Non inferire punti, review o retrospettive dai commit. Le fonti e gli acceptance
del docente restano da acquisire.

## Rituali e verbali

Planning, daily, review e retrospettiva sono eventi da registrare solo se svolti;
le schede Sprint sono piani, non verbali. Non derivare date, Story Points o
risultati demo dai commit. Usare il template consolidato in
[`TEMPLATES.md`](./TEMPLATES.md).

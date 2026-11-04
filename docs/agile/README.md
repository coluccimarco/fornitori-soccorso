# Registro Scrum e stato degli Sprint

**Riferimento integrato localmente:** `origin/main` a `bb8e548`, merge nel branch
`feature/backend` con `ffccfa2`, 04/11/2026. `git fetch origin --prune` è
riuscito; il branch è stato pubblicato su `origin/feature/backend` al commit
`ca50e8b`. La CI GitHub sul commit pubblicato è PASS (run
[41826245713](https://github.com/coluccimarco/fornitori-soccorso/actions/runs/41826245713)).

Le quattro schede `SPRINT_1.md`–`SPRINT_4.md` descrivono obiettivi e attività, non
sono verbali approvati. Lo stato del codice si è evoluto oltre alcuni riepiloghi
testuali storici:

| Sprint / area | Evidenza Git/repository | Stato verificabile | Verbale e risultati evento |
|---|---|---|---|
| 1 — Fondamenta backend | PR #1 merged; commit backend, SQL, auth/anagrafiche, workflow CI | Funzionalità descritte nel report; run CI latest verde | Date precise, daily/review/retro non reperiti |
| 2 — Workflow missioni | PR #3 merged; controller/service/tests; PostgreSQL job verde | Backend implementato e testato in CI | Non assumere che commit equivalga a review demo |
| 3 — Chat/integrations | Bot console/Telegram, API chat/outbox, MapController/MapService, Nominatim/OSRM e client web integrati; aggiornamenti UI da `origin/main` fino a `bb8e548` | API mappe implementata; UI senza mappa. Provider live NOT RUN | Verbali/demo review non reperiti |
| 4 — QA/consegna | Merge di `origin/main=bb8e548`; branch `feature/backend` pubblicato a `ca50e8b`; test Java, Python, Node e browser/DB eseguiti localmente | Java/PostgreSQL 18.6: 145 test PASS; browser: 2 scenari PASS in entrambi gli ordini su DB temporaneo; chatbot 74 PASS; Node 4 PASS; documentazione validata; CI GitHub sul commit pubblicato PASS (entrambi i job) | Rendering Word, provider live e sign-off non verificati |

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
| 3 — Integrazioni | Chatbot/Telegram, UI e API mappe presenti; aggiornamenti UI integrati localmente fino a `bb8e548`; vista mappa assente | Browser E2E locale PASS sui flussi verificati; provider live NOT RUN; WhatsApp non implementato |
| 4 — QA/consegna | Java/PostgreSQL, browser E2E isolati, chatbot, test Node e validazione documentale PASS localmente; CI sul commit `ca50e8b` PASS | Il run `41811689974` su `69ead57` è storico; fonti docente, sign-off, rendering Word e provider live da verificare |

Le schede Sprint sono un piano adattato, non consuntivi approvati. Lo schema
storico ipotizzava quattro Sprint giornalieri; non dimostra durata o date effettive.
I test locali riportati sopra dimostrano solo i flussi e i controlli indicati,
non un sign-off complessivo. Non inferire punti, review o retrospettive dai commit.
Le fonti e gli acceptance del docente restano da acquisire.

## Rituali e verbali

Planning, daily, review e retrospettiva sono eventi da registrare solo se svolti;
le schede Sprint sono piani, non verbali. Non derivare date, Story Points o
risultati demo dai commit. Usare il template consolidato in
[`TEMPLATES.md`](./TEMPLATES.md).

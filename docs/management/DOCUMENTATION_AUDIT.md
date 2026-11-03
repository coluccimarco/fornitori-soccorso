# Audit della documentazione FORNITORI

**Codice:** FOR-AUD-001 · **Versione:** 2.0 · **Data:** 03/11/2026
**Branch:** `documentazione` · **Baseline applicativa:** `origin/main` / `f763d53`
**Merge locale:** `6dbd5aa` (`origin/main` integrato; conflitti README e guida M2 risolti)

## Sintesi esecutiva

La documentazione è stata riallineata alla baseline che include backend, client web,
chatbot/Telegram e API mappe. Il client non visualizza ancora mappe e non espone tutti
i flussi centrali; provider live, browser E2E e WhatsApp non sono verificati o
implementati. OpenAPI dichiara 34 operation (30 `implemented`, 4 `designed`).

La CI GitHub Actions run `41656606659` sul commit `f763d53` ha entrambi i job
Java/OpenAPI e PostgreSQL 18 conclusi in `success`. In locale, Maven/JDK 25 ha
registrato 60 test, 0 failure/error e 29 skip; la suite Python conta 64 pass. I test
saltati e le prove locali non sono assimilati ai job CI. Le fonti originali del
docente (PDF/Excel), l'approvazione dei requisiti, il browser E2E e l'ispezione Word
visiva restano BLOCCATI/NOT RUN.

## Perimetro e fonti

Verificati: repository, branch/merge, README, codice Java e Python, asset frontend,
schema/seed SQL, OpenAPI, test, workflow CI, documenti canonici, report Sprint,
generatori DOCX/workbook/diagrammi e link Markdown. La working tree non contiene i
documenti originali del docente citati nelle pagine: conteggi Excel e scelte di
business conservano quindi il loro stato di evidenza storica, non di conferma
indipendente. Non sono stati inventati approvazioni o verbali.

## Inventario canonico dopo la riorganizzazione

| Area | Documenti autorevoli | Funzione |
|---|---|---|
| Navigazione | `docs/README.md`, `docs/00_INDICE_GENERALE.md` | Entry point e catalogo dei deliverable |
| Management | `docs/management/` | Audit/report, charter, PMP, scope, decision log, rischi, RACI e change log |
| Requisiti | `docs/requirements/` | BUR, FUD, business rules e traceability |
| Architettura | `docs/architecture/` | TAD, API map, database con dizionario, sicurezza |
| Team | `docs/team/` | Overview, guide M1–M5 e contratti d'integrazione |
| Agile | `docs/agile/` | Stato Scrum, backlog, quattro schede Sprint e template consolidati |
| Qualità | `docs/quality/` | Piano, casi, evidenze e workbook |
| Manuali | `docs/manuals/` | Manuale utente e tecnico |
| Diagrammi | `docs/diagrams/` | Sorgenti Mermaid e rendering |
| Word / modelli | `docs/word/`, `docs/templates/` | 9 deliverable DOCX e materiali di stile |
| Storico operativo | report Sprint 1–2, runbook backend e dashboard HTML | Evidenze/strumenti con status esplicito, non fonti normative |

Alla verifica strutturale risultano 50 Markdown in `docs/`, 1 dashboard HTML,
13 sorgenti Mermaid, 26 rendering PNG/SVG, 1 workbook e 9 DOCX. La directory
`docs/test/` e `docs/reference/` sono state rimosse dopo il trasferimento dei
contenuti unici.

## Registro di consolidamento e rimozione

| Percorsi ridondanti rimossi | Contenuto preservato / destinazione canonica |
|---|---|
| `00_START_HERE.md`, `01_GUIDA_GENERALE_TEAM.md` | Navigazione in `00_INDICE_GENERALE.md`; regole team in `team/README.md` |
| `02_ARCHITETTURA.md`, `TAD_FORNITORI.md` root | TAD completo in `architecture/TAD_FORNITORI.md` |
| `03_DATABASE.md`, `DATABASE_DICTIONARY.md` root, `architecture/DATA_DICTIONARY.md` | Schema, setup, relazioni e dizionario unificati in `architecture/DATABASE_DESIGN.md` |
| `04_API_REST.md`, `API_CONTRACT.md` | OpenAPI normativa; mappa operation in `architecture/API_SPECIFICATION.md`; dipendenze in `team/INTEGRATION_CONTRACTS.md` |
| `05_BUSINESS_RULES.md` root | Regole consolidate in `requirements/BUSINESS_RULES.md` |
| `06_SICUREZZA.md` | Controlli presenti e requisiti webhook in `architecture/SECURITY_DESIGN.md` |
| `08_AGILE_SCRUM.md` | Stato Sprint e rituali in `agile/README.md` |
| `09_TESTING_QA.md`, `docs/test/TEST_PLAN.md` | Piano canonico in `quality/TEST_PLAN.md` |
| `docs/test/TEST_CASES.md`, `quality/REQUIREMENTS_TRACEABILITY.md` | Casi in `quality/TEST_CASES.md`; traceability in `requirements/REQUIREMENTS_TRACEABILITY.md` |
| `docs/TEST_EVIDENCE.md` root | Registro dettagliato in `quality/TEST_EVIDENCE.md`; riferimenti aggiornati |
| `12_DECISION_LOG.md`, `13_RISCHI_E_IMPEDIMENTI.md` | Decisioni storiche integrate in `management/DECISION_LOG.md`; rischio corrente in `management/RISK_REGISTER.md` |
| `DECISION_LOG.md` root | Contesto e motivazioni conservati come record storici H-001–H-010 nel log management |
| `reference/README.md`, `reference/BUR_FORNITORI.md`, `reference/FUD_FORNITORI.md` | Puntatori duplicati eliminati; BUR/FUD canonici in `requirements/` |
| `architecture/INTEGRATION_DESIGN.md` | Provider, direzioni e limiti trasferiti in `team/INTEGRATION_CONTRACTS.md` |
| `DAILY_SCRUM_TEMPLATE.md`, `SPRINT_REVIEW_TEMPLATE.md`, `RETROSPECTIVE_TEMPLATE.md` | Campi e criteri unificati in `agile/TEMPLATES.md` |

Sono stati mantenuti i report Sprint come snapshot, il runbook operativo e i
diagrammi storici quando utili. I link interni sono stati reindirizzati ai
documenti canonici; la dashboard HTML statica è stata aggiornata al backlog
corrente e segnala che non sincronizza Jira.

## Discrepanze corrette

| ID | Discrepanza | Correzione/evidenza |
|---|---|---|
| AUD-001 | Stato del frontend indicato come assente | Client HTML/CSS/JS presente; test browser non eseguito e flussi incompleti dichiarati |
| AUD-002 | API mappe confuse con mappa nel browser | API Nominatim/OSRM presente e testata in CI; UI non invoca `/api/v1/maps/route` |
| AUD-003 | Conteggio API non aggiornato | OpenAPI ricalcolata: 34 operation, 30 implemented, 4 designed |
| AUD-004 | Test CI, locali e live aggregati | Piano/casi/evidenze separano run CI, esecuzione locale, skip e NOT RUN |
| AUD-005 | Falsa equivalenza fra merge, Sprint review e demo | Registro Scrum conserva le schede come piani; verbali non reperiti |
| AUD-006 | Log decisioni divergenti | D-001–D-012 correnti e H-001–H-010 storici in un solo registro |
| AUD-007 | Riferimenti e manuali non riflettevano i flussi nuovi | Manuali e guide M1–M5 aggiornati alla UI e API correnti |
| AUD-008 | Componenti esterni descritti come live senza prova | Provider mappe e Telegram live non attribuiti alla CI; WhatsApp/webhook NOT RUN |

## Evidenze e limiti

| Verifica | Esito |
|---|---|
| CI run `41656606659` / `f763d53` | PASS, job Java/OpenAPI e PostgreSQL 18 |
| Maven locale, Temurin 25.0.5 | BUILD SUCCESS; 60 test: 31 pass, 0 failure/error, 29 skip |
| Chatbot Python locale | `python -m unittest discover`: 64 pass |
| JavaScript statico | `node --check src/main/resources/static/app.js`: PASS |
| Documentazione/generatori | `tools/validate_docs.py`: 51 Markdown, 34 OpenAPI operation, 9 DOCX validi/12 tabelle/2 immagini, workbook 8 fogli, 13 sorgenti/26 rendering Mermaid | PASS |
| Database PostgreSQL locale | Credenziali applicative assenti per questa verifica; schema/test locali NOT RUN |
| Docker PostgreSQL locale | Engine ha risposto HTTP 500; nessun container/database creato |
| Browser, provider live, webhook/WhatsApp | NOT RUN o non implementati |
| Visualizzazione Word/paginazione | NOT RUN; il precedente tentativo automatico non ha prodotto un controllo visivo affidabile |
| Fonti e acceptance docente | BLOCKED: PDF/Excel originali assenti |

Per comandi e dettagli per commit, fare riferimento a
[`quality/TEST_EVIDENCE.md`](../quality/TEST_EVIDENCE.md). I conteggi del workbook
storico sono citazioni della documentazione precedente, non una verifica del
database locale o delle fonti non disponibili.

## Azioni residue

1. Acquisire PDF/Excel e confermare BUR, tariffa e acceptance con il docente.
2. Eseguire browser E2E con ambiente/credenziali autorizzati; validare accessibilità
   e flussi mancanti se rientrano nello scope.
3. Decidere se la mappa visuale, WhatsApp/webhook e persistenza chat sono requisiti.
4. Verificare provider live solo con policy e autorizzazione adeguate.
5. Aprire DOCX in Word, aggiornare i campi TOC e verificare figure, tabelle,
   numerazione e pagine; questo audit non dichiara verifica visuale.
6. La pubblicazione Git e l'eventuale Pull Request sono descritte nel report finale
   dopo la verifica della revisione.

# FORNITORI — indice generale e registro documentale

**Codice:** FOR-IDX-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Responsabile:** coordinamento progetto (ruolo M1 nel repository) · **Stato:** baseline documentale, non approvata dal docente
**Repository:** [coluccimarco/fornitori-soccorso](https://github.com/coluccimarco/fornitori-soccorso)

## Per iniziare

- [Audit documentale e discrepanze](./management/DOCUMENTATION_AUDIT.md)
- [Report conclusivo della revisione](./management/FINAL_REVIEW_REPORT.md)
- [Business Requirements (BUR)](./requirements/BUR_FORNITORI.md)
- [Functional Design (FUD)](./requirements/FUD_FORNITORI.md)
- [Technical Architecture (TAD)](./architecture/TAD_FORNITORI.md)
- [Piano Scrum e stato degli Sprint](./agile/README.md)
- [Piano e casi test](./quality/TEST_PLAN.md) · [evidenze](./quality/TEST_EVIDENCE.md)
- [Matrice di tracciabilità](./requirements/REQUIREMENTS_TRACEABILITY.md)
- [Manuale utente](./manuals/MANUALE_UTENTE.md) · [Manuale tecnico](./manuals/MANUALE_TECNICO.md)
- [Guide del team](./team/) · [Word modificabili](./word/)

## Catalogo

Versione e data sono metadati della baseline di riordino, non date di approvazione.
Il codice owner M1–M5 designa il ruolo del team, non una persona nominativa.

| Codice | Titolo | Categoria | Markdown autorevole | Word | Stato / responsabile | Dipendenze |
|---|---|---|---|---|---|---|
| FOR-IDX-001 | Indice generale e registro | Navigazione | `docs/00_INDICE_GENERALE.md` | `docs/word/00_INDICE_DOCUMENTAZIONE.docx` | Baseline / M1 | Tutti i documenti |
| FOR-AUD-001 | Audit documentazione | Governance | `docs/management/DOCUMENTATION_AUDIT.md` | — | Verificato sul repository / M1 | Git, CI, codice, docs |
| FOR-REV-001 | Report conclusivo revisione | Governance | `docs/management/FINAL_REVIEW_REPORT.md` | — | Baseline del branch / M1 | Audit, test, build documenti |
| FOR-BUR-001 | Business Requirements | Requisiti | `docs/requirements/BUR_FORNITORI.md` | `docs/word/BUR_FORNITORI.docx` | Bozza da confermare / team-docente | Specifiche corso non presenti |
| FOR-FUD-001 | Functional Design | Requisiti | `docs/requirements/FUD_FORNITORI.md` | `docs/word/FUD_FORNITORI.docx` | Baseline distinta per stato / M1–M5 | BUR, API, test |
| FOR-TAD-001 | Technical Architecture | Architettura | `docs/architecture/TAD_FORNITORI.md` | `docs/word/TAD_FORNITORI.docx` | Baseline tecnica / M1 | Codice, DB, CI |
| FOR-PMP-001 | Piano gestione progetto | Management | `docs/management/PROJECT_MANAGEMENT_PLAN.md` | `docs/word/PROJECT_MANAGEMENT_PLAN.docx` | Baseline, verbali da confermare / team | Backlog, commit |
| FOR-TEAM-001 | Guide del team | Operativo | `docs/team/` | `docs/word/GUIDA_TEAM.docx` | Ruoli M1–M5 / membri | OpenAPI, struttura repo |
| FOR-USER-001 | Manuale utente | Manuale | `docs/manuals/MANUALE_UTENTE.md` | `docs/word/MANUALE_UTENTE.docx` | Bot e UI presenti; browser E2E non eseguito / M2–M3 | Frontend e chatbot |
| FOR-TECH-001 | Manuale tecnico | Manuale | `docs/manuals/MANUALE_TECNICO.md` | `docs/word/MANUALE_TECNICO.docx` | Baseline / M1 | POM, SQL, chatbot, CI |
| FOR-TEST-001 | Piano test e collaudi | Qualità | `docs/quality/TEST_PLAN.md`, `docs/quality/TEST_CASES.md`, `docs/quality/TEST_EVIDENCE.md` | `docs/word/REGISTRO_TEST_E_COLLAUDI.docx` | CI Java/OpenAPI e PostgreSQL PASS; browser NOT RUN / M1–M5 | Report CI, suite |
| FOR-TRACE-001 | Matrice requisiti/API/test | Tracciabilità | `docs/requirements/REQUIREMENTS_TRACEABILITY.md` | — | Requisiti locali, approvazione docente assente | BUR, OpenAPI, casi/evidenze |
| FOR-RSK-001 | Registro rischi | Management | `docs/management/RISK_REGISTER.md` | — | Aperto e aggiornabile / team | Dipendenze e fonti |
| FOR-DEC-001 | Registro decisioni | Management | `docs/management/DECISION_LOG.md` | — | Baseline / team-docente | Decisioni storiche |
| FOR-CHG-001 | Registro modifiche documentali | Management | `docs/management/CHANGE_LOG.md` | — | Attivo / M1 | Revisioni Git |
| FOR-DATA-001 | Matrici di lavoro | Workbook | `docs/quality/registri.xlsx` | — | Generato / M1 | Requisiti, API, test, rischi |

## Registro delle fonti normative

| Fonte | Autorità | Limiti |
|---|---|---|
| `pom.xml`, codice Java/Python e configurazione | Implementazione presente | Non dimostra da sola il superamento dei test |
| `api/openapi.yaml` | Contratto HTTP dichiarato | Estensioni di stato sono metadati; si confrontano con controller/test |
| `database/*.sql` | Schema e fixture dichiarati | Lo script non prova che sia stato importato in un database locale |
| GitHub Actions e test report associati | Esito dei job eseguiti | Non certificano UI, mappe o provider non inclusi nei job |
| Documenti e report preesistenti in `docs/` | Evidenza storica o guida | Possono essere obsoleti; verificare l'audit |
| Specifiche PDF/Excel del docente | Fonte primaria prevista | Non trovate nella working tree: requisiti da confermare |

## Generazione Word e aggiornamento indice

Da root: `python tools/build_docs.py`. I DOCX sono output generati, non fonti
manuali. L'indice Word è un campo aggiornabile: aprire il DOCX in Microsoft Word,
fare clic destro sull'indice, scegliere **Aggiorna campo → Aggiorna intero indice**.
Il contenuto generato non dichiara che Word abbia già ricalcolato pagine o indice.

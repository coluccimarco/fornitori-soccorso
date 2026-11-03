# Audit della documentazione FORNITORI

**Codice:** FOR-AUD-001 · **Versione:** 1.0 · **Data di rilevazione:** 03/11/2026
**Perimetro verificato:** branch `documentazione`, base `main` aggiornata a `25c613b`; nessuna modifica applicativa richiesta.

## Sintesi esecutiva

Il repository contiene un backend Spring Boot/Java e un chatbot Python utilizzabile
con Telegram; lo stato della documentazione non seguiva l'integrazione in `main`.
La CI GitHub Actions associata a `25c613b` risulta conclusa con successo nei due job
(Java/OpenAPI e PostgreSQL 18). Il progetto non contiene un frontend, mappe o un
collegamento WhatsApp operativo. Il menu Telegram e il recapito dei messaggi di
accettazione/completamento sono riportati come provati in locale nel README e nel
runbook del chatbot; questa revisione non ha ripetuto una prova su un telefono.

L'audit ha letto README, documenti di `docs/` inclusi `reference/`, `team/`,
`agile/`, `diagrams/`, `test/`, contratto OpenAPI, script SQL, POM, sorgenti e test,
workflow CI e storia Git/PR. Il PDF di specifica e il file Excel originale citati
nei documenti non sono presenti nella working tree e non sono stati forniti come
allegati: la loro verifica indipendente è quindi **BLOCCATA**. Non sono stati
inventati dati o verbali per colmare questa lacuna.

## Inventario per aree

La data è quella riportata nei front matter/testate, ove esistente; molti documenti
non hanno versione formale o nome di approvatore. La qualità descrive utilità
attuale, non approvazione.

| Percorso / gruppo | Titolo e finalità | Versione / data / responsabile | Stato e qualità | Duplicati, collegamenti o obsolescenza | Azione |
|---|---|---|---|---|---|
| `README.md` | Panoramica, setup e stato applicativo | Senza controllo formale; storico 03/11/2026 | Utile, ma aveva formulazioni riferite al branch separato e stato non allineato al latest `main` | Duplica setup/API in docs; sezione stato va riferita alla baseline integrata | Allineare alla revisione `main` |
| `docs/00_START_HERE.md` | Onboarding e navigazione | 1.0, 03/11/2026, M1 | Buono per onboarding, ma diceva che chatbot/notifiche erano pianificati | Indice sovrapposto a README e altre guide | Reindirizzare all'indice canonico aggiornato |
| `docs/01_GUIDA_GENERALE_TEAM.md`, `07_GIT_WORKFLOW.md`, `11_DEFINITION_OF_DONE.md` | Regole di team e contributo | 1.0, 03/11/2026, M1 | Riutilizzabili, da coordinare con indice/branch attuali | Link sparsi e nomi branch storici | Conservare e indicizzare; aggiornare le parti mutate |
| `docs/02_ARCHITETTURA.md`, `03_DATABASE.md`, `DATABASE_DICTIONARY.md`, `TAD_FORNITORI.md` | Disegno tecnico e schema | 1.0–1.2, 03/11/2026, M1 | Contenuti tecnici in parte validi, TAD troppo sintetico e status legacy | Ripetono package, API, stato e modello DB | Creare un TAD canonico; mantenere i file storici come riferimenti |
| `docs/04_API_REST.md`, `API_CONTRACT.md`, `api/openapi.yaml` | Elenco API e contratti | Guide 1.0; OpenAPI 1.0.0 | OpenAPI contiene 34 operazioni; conteggi e descrizione introduttiva legacy non sempre riflettono il codice | Fonti di stato concorrenti | OpenAPI resta contratto; rimandare alla matrice verificata |
| `docs/05_BUSINESS_RULES.md`, `reference/BUR_FORNITORI.md` | Regole e requisiti business | BUR 1.0, 03/11/2026, team lead | Baseline corta; menziona fonte docente non accessibile in questo audit | Si sovrappone al FUD e alla decision log | Sostituire il BUR di riferimento con una baseline tracciabile e dichiarata provvisoria |
| `docs/reference/FUD_FORNITORI.md`, `docs/05_BUSINESS_RULES.md` | Flussi funzionali e regole | FUD 1.0, 03/11/2026, team lead | Elenca flussi, ma affermava che il chatbot era solo pianificato | Duplicato parziale di regole, backlog e guide M3/M5 | Creare FUD per use case e stati di implementazione |
| `docs/08_AGILE_SCRUM.md`, `docs/agile/*` | Backlog, Sprint e template | 1.0–1.1, 03/11/2026, M1 | La scheda Sprint 2 descrive codice reale; riepilogo Sprint 1–4 contraddice l'avanzamento | Rischio di trattare piano come verbale | Separare baseline Git/CI dai verbali mancanti; non riscrivere gli storici |
| `docs/SPRINT_1_REPORT.md`, `SPRINT_2_REPORT.md` | Evidenze storiche Sprint | 1.1/1.0, 03/11/2026, M1 | Utili come snapshot datati; parte dei conteggi è stata superata dalla CI successiva | Ripetuti in QA e README | Conservare come storico, riferirsi al job latest per stato corrente |
| `docs/09_TESTING_QA.md`, `TEST_EVIDENCE.md`, `docs/test/*` | Piano, casi ed esiti test | 1.0/1.2, 03/11/2026, M1 | Buoni per backend; copertura del bot e separazione CI/local incompleta nel riepilogo | Tabelle di evidenze ripetute nei report | Conservare le evidenze originali, aggiungere matrice consolidata |
| `docs/06_SICUREZZA.md`, `10_DEPLOYMENT.md` | Sicurezza, setup e runtime | 1.0, 03/11/2026, M1 | Distinguono alcuni limiti ma non descrivono pienamente il servizio chatbot effettivo | Duplica README e guide membri | Aggiornare le guide e mantenere una sola procedura tecnica |
| `docs/12_DECISION_LOG.md`, `DECISION_LOG.md`, `13_RISCHI_E_IMPEDIMENTI.md` | Decisioni e rischi | 1.0, 03/11/2026, M1 | Doppio log decisioni con contenuti differenti; rischio non sempre riferito a `main` | Duplicazione diretta | Unificare con rimandi; non attribuire approvazioni non provate |
| `docs/team/MEMBRO_1_*` ... `MEMBRO_5_*`, `INTEGRATION_CONTRACTS.md` | Ruoli, onboarding e API tra componenti | 1.0, 03/11/2026, membri | M2/M4 roadmap coerente; M3/M5 e contratti M1/M5 avevano status obsoleti | Ripetizione di OpenAPI e Scrum | Aggiornare status, ownership e link; API resta in OpenAPI |
| `docs/diagrams/*.mmd` | Sorgenti Mermaid | Senza metadati omogenei | Modificabili; 13 sorgenti validate/renderizzate in PNG e SVG da CLI locale | Diagrammi di contesto duplicati nel TAD | Conservare sorgenti e rigenerare output con lo script |
| `docs/SPRING_SCRUM_DASHBOARD.html` | Dashboard locale statica | Senza versione | Utile visualizzazione, non fonte live; esporta backlog statico | Sovrappone backlog | Conservare come vista ausiliaria |
| `chatbot/README.md` | Installazione/operatività chatbot | Senza versione documentale | Evidenza importante di Python, Telegram, token e limiti | Ripete futuro manuale utente/tecnico | Rimane riferimento specifico al modulo |
| `pom.xml`, `src/`, `chatbot/`, `database/`, workflow | Fonti di implementazione e verifica | Revisione `25c613b` | Fonti primarie per stack, routes e code presenti | Non costituiscono requisiti approvati | Riferimenti delle nuove guide, non alterati |

## Registro delle discrepanze

| ID | Documento | Informazione precedente | Fonte attendibile / evidenza | Correzione adottata | Motivo / decisione aperta |
|---|---|---|---|---|---|
| AUD-001 | `docs/08_AGILE_SCRUM.md`, `docs/agile/SPRINT_2.md` | Sprint 2 chiamato pianificato mentre la scheda riportava backend implementato | `main` contiene PR backend #3, controller/service/test; run CI `41651140562` esegue verify PostgreSQL con successo | Indice e Scrum ora distinguono piano, merge/stato e verbali; scheda storica non riscritta | Date e verbale formale non verificabili |
| AUD-002 | `docs/00_START_HERE.md`, README e `docs/team/MEMBRO_3_CHATBOT.md` | Chatbot indicato come pianificato o API non approvate | `chatbot/` e `ChatController`; test chatbot e CI; commit Telegram su `main` | Stato corretto: chatbot e API backend implementati; prova Telegram dichiarata nei documenti di modulo, non riprodotta in questo audit | Prova sul dispositivo non indipendentemente ripetuta |
| AUD-003 | README, `docs/team/MEMBRO_5_NOTIFICATIONS.md`, `docs/08_AGILE_SCRUM.md` | Notifiche Telegram trattate come futuro/outbox solo predisposta | `notifications.py`, API outbox in `ChatController`, report di modulo e runbook | Distinguere outbox transazionale/backend, recapito Telegram per chat supportate, WhatsApp pianificato | Consegnabilità e operatività di lungo periodo non certificate da CI |
| AUD-004 | `docs/04_API_REST.md`, `docs/API_CONTRACT.md`, `api/openapi.yaml` | Conteggi vecchi (23 implementate/8 progettate) e descrizione introduttiva OpenAPI iniziale stale | Conteggio riproducibile delle operazioni nel YAML sul commit `25c613b`: 34 totali, 29 `implemented`, 5 `designed` | Nuovi documenti usano il conteggio ricalcolato; OpenAPI non modificata, la sua introduzione resta da aggiornare dal contract owner | Confermare la policy di manutenzione e il conteggio dopo le prossime modifiche |
| AUD-005 | `docs/SPRINT_1_REPORT.md`, README | Commit e implementazione presentati come se il branch `feature/backend` non fosse integrato | Storia Git: PR #3 merged in `main`; ultimo commit verificato `25c613b` | Stato corrente è il ramo integrato; i report Sprint rimangono snapshot storici | Nessuna |
| AUD-006 | `docs/reference/BUR_FORNITORI.md`, `FUD_FORNITORI.md`, `TAD_FORNITORI.md` | Materiale descritto come requisiti o design completi, ma sintetico e/o legato a Sprint 1 | Confronto con controller/API, SQL, test e docs | Aggiunte fonti canoniche complete per baseline e link espliciti alla fonte | PDF docente non reperito: approvazione requisiti bloccata |
| AUD-007 | `docs/DECISION_LOG.md`, `docs/12_DECISION_LOG.md` | Esistono due log decisionali divergenti | Entrambi i file sono versionati, non esiste campo “approved by” comune | Creato registro decisioni consolidato che cita i log storici | Stabilire se unire gli storici e chi ha autorità di approvazione |
| AUD-008 | Guide member 1/3/5 e API | Integrazione chatbot descritta come pianificata o non approvata | Codice backend e bot, OpenAPI operativa e run CI | Nuovi documenti separano client Telegram reale da feature UI/mappe/WhatsApp future | Decisione prodotto su canale WhatsApp rimane aperta |
| AUD-009 | Backlog/requisiti | Regole di business trattate come richieste del docente senza fonte allegata disponibile | SQL seed, implementazione pricing e documenti storici; non disponibili PDF/Excel originali | Requisiti etichettati come baseline tecnica/da confermare e i valori dataset come evidenze secondarie | Convalidare con docente timestamp notturno, rounding e perimetro obbligatorio |
| AUD-010 | README e piani UI | Frontend e mappe descritti come parti in avanzamento non sempre esplicitamente future | Nessun package frontend; OpenAPI maps è `designed`; team guida M2/M4 | UI e routing sono indicati come pianificati/non implementati | Nessuna prova UI possibile |
| AUD-011 | Percorsi DOCX, workbook e registrazione documenti | Non era presente una pipeline Word canonica | Nessun generatore `.docx` nella working tree iniziale | Aggiunti generatore ripetibile, output Word e workbook dei registri | Word è installato, ma l'esportazione automatica in PDF/ispezione visuale non è stata completata |

## Baseline tecnica ispezionata

| Aspetto | Evidenza | Esito |
|---|---|---|
| Branch di lavoro | `documentazione`, creato dopo fast-forward di `main` fino a `25c613b` | Verificato |
| Main integrato | Ultimo commit `25c613b`; PR backend #3 merged; PR #1 merged; commit chatbot successivi integrati | Verificato con Git/`gh` |
| Java/Boot | `pom.xml`: Java e release 25, Spring Boot parent 4.1.2 | Verificato |
| Database | PostgreSQL 18 dichiarato dal workflow; schema e seed in `database/` | Verificato da file |
| OpenAPI | 34 operazioni: 29 implementate, 5 progettate | Ricalcolato da YAML |
| CI | Workflow run `41651140562`, conclusione success; job Java/OpenAPI e PostgreSQL 18 success | Verificato da GitHub Actions |
| Suite | Test Java e Python presenti; numero di test non reinterpretato come copertura completa | Inventariato; run locale da eseguire dopo i cambi documentali |
| Test PostgreSQL locale | Report preesistente indica credenziali locali mancanti | Non ripetuto; nessuna credenziale toccata |
| PDF e foglio Excel di specifica | Non presenti nel repository elencato e non allegati alla sessione | **BLOCCATO** per controllo dei requisiti primari |
| PR | PR #3 backend merged; stato recuperato tramite `gh` | Verificato |
| Docente/approvatore | Nessuna approvazione nominativa disponibile | **DA CONFERMARE** |

## Controllo qualità dell'audit

| Controllo | Esito | Evidenza / limite |
|---|---|---|
| README, docs ricorsive e reference/team/agile/diagrams/test | PASS | Inventario e lettura dei contenuti rilevanti |
| Codice, POM, DB, OpenAPI e test | PASS | Confronto diretto con commit `25c613b` |
| CI remota e PR integrate | PASS | Workflow `41651140562`, PR #3 |
| Maven locale con Temurin 25.0.5 | PASS con skip espliciti | BUILD SUCCESS; 38 test, 21 pass, 17 skip per DB locale/context non configurati |
| Suite Python chatbot | PASS | `python -m unittest discover` da `chatbot/`: 64 test, 12,169 s, exit 0; non eseguito dal workflow CI |
| Mermaid | PASS | 13 sorgenti, 26 file SVG/PNG generati con Mermaid CLI 11.4.2 |
| DOCX/workbook strutturali | PASS | 9 DOCX validi, campi TOC/pagina, 11 tabelle, 3 figure, 8 fogli workbook; link Markdown verificati |
| Requisiti PDF ed Excel | BLOCKED | Originali non disponibili |
| Verifica manuale live Telegram | NOT RUN | Il README del modulo riporta il test; audit non lo ha ripetuto |
| Ispezione visiva Word/pagine e indice ricalcolato | NOT RUN | Word disponibile; esportazione PDF e ricalcolo dei campi non completati |
| Approvazione docente | DA CONFERMARE | Nessuna evidenza |

## Risorse esterne e limiti

Nessun testo o diagramma da repository terzi è stato copiato. L'audit non dichiara
certificazioni normative né audit formali di sicurezza. La descrizione del progetto
ATM, i modelli arc42/C4/Diátaxis e le specifiche del corso non sono usati come prova
di requisiti. Le informazioni su risultati Excel riportate nei documenti storici
non sono state ricalcolate sull'originale non disponibile.

## Azioni residue

1. Consegnare PDF e Excel originali, poi validare requisiti, dataset, ID e acceptance.
2. Far approvare da docente/team i requisiti contrassegnati `DA CONFERMARE`.
3. Far aggiornare l'introduzione descrittiva di `api/openapi.yaml` dal contract owner.
4. Confermare i verbali/date dei quattro Sprint: non ricostruirli retroattivamente dai commit.
5. Selezionare framework UI, provider mappe e piano WhatsApp prima di pianificare l'implementazione.
6. Aggiornare e rigenerare output DOCX/workbook dopo l'approvazione delle fonti primarie.

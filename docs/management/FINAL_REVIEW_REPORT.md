# Report conclusivo — reengineering documentazione FORNITORI

**Codice:** FOR-REV-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Branch:** `documentazione` · **Baseline applicativa:** `main` / `25c613b`

## A. Executive summary

È stato riorganizzato l'archivio documentale per aree, riallineato lo stato al codice
presente in `main`, aggiunto un audit con registro delle discrepanze, creati BUR/FUD/
TAD e manuali canonici. La pipeline locale genera 9 DOCX modificabili, un workbook
Excel e le figure Mermaid SVG/PNG. La CI dell'applicazione è verde su
`25c613b`. Fonti primarie del corso e verifica visiva Word restano bloccate/non eseguite;
la consegna complessiva non è dichiarata approvata o completa.

## B. Audit iniziale e correzioni

L'audit ricorsivo ha coperto README, docs (`reference`, `team`, `agile`, `diagrams`,
`test`), API, database, POM, sorgenti/test, Git, PR #3 e workflow CI.

Discrepanze principali:

1. Sprint 2 descritto come pianificato nonostante PR #3 e backend integrato.
2. Chatbot/API chat e dispatcher Telegram ancora descritti come pianificati in
   alcuni documenti; notifiche Telegram distinte da WhatsApp futuro.
3. Riepilogo OpenAPI legacy; count ricalcolato: 34 (29 implementate, 5 progettate).
4. README e guide si riferivano a branch feature già integrati.
5. Doppio registro decisioni e duplicazione di BUR/FUD/TAD.
6. Documenti del docente e record Scrum non disponibili/non attestati.

Le evidenze dettagliate, fonti e decisioni aperte sono in
[DOCUMENTATION_AUDIT.md](./DOCUMENTATION_AUDIT.md). I report Sprint storici non sono
stati retrodatati; sono stati aggiunti addendum datati.

## C. Struttura documentale corrente

```text
docs/
├── README.md, 00_INDICE_GENERALE.md
├── management/   audit, charter, PMP, scope, RACI, rischi, decisioni, changelog, report
├── requirements/ BUR, FUD, use cases, business rules, traceability
├── architecture/ TAD, database, dizionario, API, sicurezza, integrazioni
├── team/         overview, guide M1–M5, contratti
├── agile/        backlog, schede Sprint, template, registro stato
├── quality/      test plan, evidence index, traceability, registri.xlsx
├── manuals/      manuale utente e tecnico
├── diagrams/     13 sorgenti .mmd, 26 output SVG/PNG
├── templates/    stile Word, modello, registro
└── word/         9 documenti DOCX generati
tools/            build_docs.py, build_registers.py, build_diagrams.ps1, validate_docs.py
```

I percorsi storici rimangono disponibili dove esistenti; quelli diventati rimandi
puntano ai nuovi documenti autorevoli. I report storici e le evidenze non vengono
presentati come stato corrente.

## D. Documenti Word e fogli di calcolo

| DOCX | Sorgente | Contenuto | Controllo |
|---|---|---|---|
| `docs/word/00_INDICE_DOCUMENTAZIONE.docx` | `docs/00_INDICE_GENERALE.md` | Registro e navigazione | Struttura valida; indice da aggiornare in Word |
| `BUR_FORNITORI.docx` | `requirements/BUR_FORNITORI.md` | Requisiti business, vincoli e accettazione | Struttura valida; approvazione docente assente |
| `FUD_FORNITORI.docx` | `requirements/FUD_FORNITORI.md` | 12 use case con flussi, eccezioni, API e test | Struttura valida |
| `TAD_FORNITORI.docx` | `architecture/TAD_FORNITORI.md` | Stack, componenti, sicurezza, DB, CI e limiti | Struttura valida, 3 figure incorporate |
| `PROJECT_MANAGEMENT_PLAN.docx` | `management/PROJECT_MANAGEMENT_PLAN.md` | Governance e quattro Sprint | Struttura valida |
| `GUIDA_TEAM.docx` | `team/README.md` | Ruoli, confini e checklist PR | Struttura valida |
| `MANUALE_UTENTE.docx` | `manuals/MANUALE_UTENTE.md` | Bot Telegram; limiti UI centrale/autista | Struttura valida |
| `MANUALE_TECNICO.docx` | `manuals/MANUALE_TECNICO.md` | Setup, build, DB, test, chatbot, troubleshooting | Struttura valida |
| `REGISTRO_TEST_E_COLLAUDI.docx` | `quality/TEST_PLAN.md` | Test, release gate e comandi | Struttura valida |

Sono stati creati 9 DOCX validi, 11 tabelle e 3 figure inserite nel TAD. Pagine
totali, impaginazione visiva e aggiornamento indice non sono verificati: Word è
installato, ma l'esportazione automatica in PDF non è terminata in questo ambiente.
Il campo TOC è presente e aggiornabile.
`docs/quality/registri.xlsx` contiene fogli per requisiti, traceability, backlog,
API, test, rischi, RACI e modifiche.

## E. Requisiti e tracciabilità

I requisiti `BR-*` e le story `ST-*` sono identificativi locali creati per rendere
tracciabile la baseline; non sono attribuiti alla specifica docente. PDF e Excel
non erano presenti nel checkout. Il BUR è quindi una bozza da convalidare; criteri
di priorità e alcuni valori della tariffa sono DA CONFERMARE. La matrice collega
requisito, story, componente, API, test ed evidenza disponibile.

## F. Architettura e diagrammi

TAD verificato contro POM, controller/service, `SecurityConfig`, database SQL,
OpenAPI e workflow. Java 25/Spring Boot 4.1.2/PostgreSQL 18 sono effettivi nel
repository/CI. Bot e API chat sono presenti; UI, routing, webhooks e WhatsApp sono
separati come futuri. Sono stati mantenuti/aggiornati ER e stato missione e aggiunti
contesto, container, package, sequenze creazione, assegnazione, scadenza,
completamento e chatbot/notifiche.

Mermaid CLI 11.4.2 ha validato 13 sorgenti e generato 26 asset PNG/SVG. Nessuna
figura dei modelli esterni è stata copiata.

## G. Scrum

Le schede conservano la pianificazione; un registro separato riassume gli avanzamenti
dedotti da Git/CI. PR #3 ha integrato workflow missioni; i componenti chatbot e
Telegram sono presenti nella baseline attuale. Date/verbali di daily, Sprint Review
e retrospettive non sono stati trovati. UI e mappe restano mancanti; completamento
dello Sprint 4 non dichiarato.

## H. Test ed evidenze

- CI GitHub run `41651140562`, commit `25c613b`: entrambi i job Java/OpenAPI e
  PostgreSQL 18 in success.
- Locale Temurin 25.0.5: `mvnw clean verify` BUILD SUCCESS, 38 test, 0 failure/error,
  21 pass, 17 skip per configurazione DB locale/context non disponibile.
- Chatbot: `python -m unittest discover` da `chatbot/`: 64 test pass, 0 failure.
- CI corrente non esegue la suite Python; prova Telegram reale è riportata nel
  README del modulo ma non è stata ripetuta durante la revisione.

## I. Quality assurance documentale

| Controllo | Stato | Evidenza |
|---|---|---|
| Link relativi Markdown | PASS | 76 file controllati |
| OpenAPI status/role e conteggio | PASS | 34 operation, 29 implemented, 5 designed |
| Diagrammi | PASS | 13 sorgenti, 26 asset |
| DOCX validi | PASS | 9 file; stile Word, campi TOC/pagina, tabelle/figure |
| Workbook registri | PASS | 8 fogli |
| Test backend locale | PASS con skip | 21 pass, 17 skip espliciti |
| Test chatbot locale | PASS | 64 test |
| Ispezione visuale DOCX e pagine | NOT RUN | Word installato; esportazione PDF automatica non completata |
| Specifiche PDF/Excel originali | BLOCKED | Da fornire |
| Approvazione docente | DA CONFERMARE | Nessuna evidenza |
| Verifica manuale live Telegram | NOT RUN | Solo evidenza dichiarata nel README del modulo |

Comando strutturale: `python tools/validate_docs.py`. Rigenerazione:
`python tools/build_docs.py`; diagrammi: `powershell -ExecutionPolicy Bypass -File tools/build_diagrams.ps1`.

## J. Git e consegna

Branch creato secondo richiesta: `documentazione`, dopo switch su `main` e
fast-forward a `origin/main` (`25c613b`). L'altro branch `feature/backend` non è
stato modificato. Nessun commit, push o merge è stato eseguito. Prima della consegna
eseguire `git diff --check`, rigenerare gli output e rivedere il diff.

## K. Residui e ordine suggerito per il docente

1. Fornire le specifiche PDF/Excel originali e confermare la matrice requisiti.
2. Presentare BUR (dopo convalida), FUD e TAD; allegare i diagrammi renderizzati.
3. Presentare PMP/Scrum con nota esplicita sui verbali non reperiti.
4. Presentare piano/evidenze test distinguendo CI, test locale e canale live.
5. Presentare manuale utente come demo limitata Telegram e manuale tecnico.
6. Aggiornare in Word il TOC, controllare paginazione/figure/tabelle e indicare
   nell'indice le versioni approvate.

Prima di un uso operativo reale sono prioritari verifica dell'identità cliente,
scelta del perimetro UI/mappe/WhatsApp, policy privacy e disponibilità del provider.

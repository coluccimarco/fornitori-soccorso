# Report conclusivo — documentazione FORNITORI

**Codice:** FOR-REV-001 · **Versione:** 2.0 · **Data:** 03/11/2026
**Branch di lavoro:** `documentazione` · **Baseline applicativa:** `f763d53`
**Merge commit locale:** `6dbd5aa`

## Sintesi

La revisione integra nella documentazione lo stato più recente del repository,
con backend, client statico, chatbot/Telegram e API mappe. Le fonti documentali
sono state consolidate senza eliminare i report Sprint e le evidenze storiche
utili. Il PDF/Excel originali del docente non sono nel checkout: il BUR e le
regole di business restano quindi da confermare.

La CI Java/OpenAPI e PostgreSQL 18 del run `41656606659` sul commit `f763d53` è
verde. Le verifiche locali hanno esito BUILD SUCCESS e 64 test Python pass; i
29 test Maven saltati, il browser non eseguito e i provider live non sono
presentati come PASS. L'API mappe è implementata ma non invocata dal client;
WhatsApp/webhook non sono implementati.

## Integrazione e correzione di stato

Il branch `documentazione` è stato aggiornato da `origin/main` a `f763d53`.
Il merge non-fast-forward è completato nel commit `6dbd5aa`; i conflitti in
`README.md` e `docs/team/MEMBRO_2_FRONTEND.md` sono risolti. La baseline integrata
non introduce conflitti applicativi.

Conteggio OpenAPI ricalcolato: 34 operation, 30 `implemented` e 4 `designed`
(gestione utenti e webhook in ingresso). La CI verifica backend/API/PostgreSQL,
non l'esperienza browser o la disponibilità live di Telegram, Nominatim e OSRM.

## Riorganizzazione documentale

Gli entry point sono `docs/README.md` e `docs/00_INDICE_GENERALE.md`. Le fonti
autorevoli sono state uniformate in management, requirements, architecture,
team, agile, quality, manuals, diagrams, templates e word.

Sono stati rimossi 27 rimandi o duplicati dopo aver trasferito i contenuti unici:
guide root nella overview/team canonica; setup e dizionario nel disegno DB;
requisiti webhook nella pagina security; log decisionali divergenti nel registro
management; test case/evidence nella quality; tre template Scrum in
`agile/TEMPLATES.md`. BUR/FUD/TAD canonici, report Sprint e runbook sono conservati.
La dashboard HTML statica è stata riallineata e continua a dichiararsi non live.

Inventario verificato sotto `docs/`: 50 Markdown, 1 dashboard HTML, 13 sorgenti
Mermaid, 26 asset PNG/SVG, 1 workbook e 9 documenti Word generati. Per i nomi
precisi rimossi e la tracciabilità delle correzioni consultare
[l'audit documentale](./DOCUMENTATION_AUDIT.md).

## Deliverable Word e workbook

| Output | Sorgente | Contenuto |
|---|---|---|
| `00_INDICE_DOCUMENTAZIONE.docx` | `00_INDICE_GENERALE.md` | Catalogo e navigazione |
| `BUR_FORNITORI.docx` | `requirements/BUR_FORNITORI.md` | Requisiti provvisori e acceptance |
| `FUD_FORNITORI.docx` | `requirements/FUD_FORNITORI.md` | Flussi e stati |
| `TAD_FORNITORI.docx` | `architecture/TAD_FORNITORI.md` | Architettura e limiti |
| `PROJECT_MANAGEMENT_PLAN.docx` | `management/PROJECT_MANAGEMENT_PLAN.md` | Governance e Sprint |
| `GUIDA_TEAM.docx` | `team/README.md` | Ruoli e convenzioni |
| `MANUALE_UTENTE.docx` | `manuals/MANUALE_UTENTE.md` | Uso delle superfici presenti |
| `MANUALE_TECNICO.docx` | `manuals/MANUALE_TECNICO.md` | Setup, build e diagnosi |
| `REGISTRO_TEST_E_COLLAUDI.docx` | `quality/TEST_PLAN.md` | Strategia e gate |

`docs/quality/registri.xlsx` è generato da requisiti, traceability, backlog, API,
test, rischi, RACI e change log. Il controllo strutturale ha trovato 12 tabelle e
2 immagini nei DOCX e 8 fogli nel workbook. L'indice TOC Word è un campo
aggiornabile; generazione strutturale non equivale a revisione di impaginazione.

## Verifiche

| Controllo | Evidenza | Esito |
|---|---|---|
| CI applicativa | GitHub Actions run `41656606659`, commit `f763d53`; job Java/OpenAPI e PostgreSQL 18 `success` | PASS |
| Maven locale | `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`, Temurin 25.0.5+8, `BUILD SUCCESS` | PASS |
| Test Maven | 60 contabilizzati: 31 pass, 0 failure/error, 29 skip principalmente DB/context senza datasource autorizzato | PASS per eseguiti; skip espliciti |
| Chatbot | `python -m unittest discover` da `chatbot/`: 64 test, exit 0 | PASS locale; non in CI |
| JavaScript statico | `node --check src/main/resources/static/app.js` | PASS |
| Documentazione | `python tools/validate_docs.py`: 51 Markdown, OpenAPI, DOCX, workbook e Mermaid validati | PASS |
| Browser E2E | Non eseguito; account/DB per prova autorizzata non disponibili | NOT RUN |
| PostgreSQL locale | Porta 5432 raggiungibile, credenziali applicative assenti per questa verifica | BLOCKED |
| Docker PostgreSQL locale | Docker Desktop Engine HTTP 500; nessun container/database creato | BLOCKED |
| Provider live / WhatsApp | Provider mappe non verificati live; WhatsApp/webhook non implementati | NOT RUN |
| Word visuale | Non è stato prodotto un rendering verificabile in Word | NOT RUN |
| PDF/Excel docente | Non presenti nella working tree verificata | BLOCKED |

`python tools/build_docs.py` ha rigenerato i 9 DOCX e `registri.xlsx`;
`tools/build_diagrams.ps1` ha rigenerato i 26 asset PNG/SVG da 13 sorgenti.
La revisione visuale Word resta NOT RUN. Per il dettaglio per comando e le
evidenze non eseguite consultare
[`quality/TEST_EVIDENCE.md`](../quality/TEST_EVIDENCE.md).

## Stato Git e pubblicazione

Sul branch `documentazione` sono presenti il merge commit `6dbd5aa` e il commit
documentale `1bd00ff`; l'aggiornamento del report è nel commit `9482363`.
`origin/main` è stato rifetchato ed è rimasto a `f763d53`. Il branch è stato
pubblicato su `origin/documentazione` e la [PR #4](https://github.com/coluccimarco/fornitori-soccorso/pull/4)
è aperta verso `main`. Nessun segreto o credenziale locale è stato modificato
o inserito.

## Azioni residue per la consegna didattica

1. Acquisire le specifiche originali e far approvare BUR, regole tariffarie,
   priorità e acceptance.
2. Eseguire browser E2E in un ambiente con accesso autorizzato; decidere se la
   vista mappa e i flussi mancanti rientrano nello scope.
3. Decidere scope e provider di WhatsApp/webhook e le policy privacy per chatbot
   e geocoding esterno.
4. Aprire i DOCX in Microsoft Word, aggiornare i TOC e verificare pagine,
   figure, tabelle e numerazione.
5. Pubblicare il branch e aprire la PR solo dopo la validazione locale e un nuovo
   controllo che `origin/main` non sia avanzato.

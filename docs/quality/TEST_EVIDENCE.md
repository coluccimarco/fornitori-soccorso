# Evidenze di test

**Versione:** 2.1 · **Data:** 04/11/2026

Questo registro separa evidenza CI, esecuzione locale e verifica non eseguita.
Il codice presente o lo stato OpenAPI non sono da soli prova di funzionamento.
Le sezioni datate sotto sono snapshot storici: la tabella finale è il riferimento
per lo stato corrente di questa verifica.

## Baseline storica — 03/11/2026

| Controllo | Evidenza all'epoca | Esito all'epoca |
|---|---|---|
| Integrazione di `origin/main` | `origin/main` aggiornato a `f763d53`; merge nel branch `documentazione` completato con commit `6dbd5aa` | PASS |
| CI Java/OpenAPI e PostgreSQL 18 | GitHub Actions run `41656606659` sul commit `f763d53`; entrambi i job `success` | PASS |
| Maven locale | `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`, Temurin 25.0.5+8; `BUILD SUCCESS` | PASS |
| Test Maven locali | 60 contabilizzati: 31 pass, 0 failure, 0 error, 29 skip, principalmente integrazione DB/context non configurati localmente | PASS per test eseguiti; skip non sono PASS |
| PostgreSQL locale | Servizio/porta 5432 raggiungibile, credenziali applicative assenti per questa verifica; schema/test DB locali non eseguiti | BLOCKED |
| PostgreSQL effimero con Docker locale | Docker Desktop Engine ha risposto HTTP 500; nessun container/database creato | BLOCKED |
| Chatbot | `python -m unittest discover` da `chatbot/`: 64 test, exit code 0 | PASS locale; suite non inclusa nella CI |
| Sintassi JavaScript | `node --check src/main/resources/static/app.js` | PASS |
| Browser/UI | Nessun E2E/browser eseguito; UI presente ma non prova di esperienza utente verificata | NOT RUN |
| Mappe | Test Java automatizzati compresi nella CI; 7 `MapProviderClientsTests` locali PASS; provider live Nominatim/OSRM non verificati | PASS automatizzato; live NOT RUN |
| Mappa nella UI | La pagina non consuma `/api/v1/maps/route` né visualizza mappe | Non implementato |
| Telegram live | Non ripetuto durante questa revisione; riferirsi alle evidenze storiche nel report Sprint 1 | NOT RUN |
| WhatsApp/webhook | Integrazione non implementata | NOT RUN |
| Word visuale | Rendering/ispezione visiva non completati; un precedente tentativo automatico non è riuscito | NOT RUN |
| Requisiti originali docente | PDF/Excel originali non presenti nel checkout verificato | BLOCKED / da confermare |

All'epoca il test DB locale non era stato eseguito; la CI PostgreSQL 18 era
evidenza separata e non equivalente a un test sul server locale.
Nominatim e OSRM sono provider pubblici; disponibilità, limiti e risposta live
non sono coperti dalla suite automatica.

## Evidenze storiche mantenute

I report [Sprint 1](../SPRINT_1_REPORT.md) e
[Sprint 2](../SPRINT_2_REPORT.md) conservano i risultati della baseline
precedente, compresi confronto con il workbook Excel e test di feature backend.
I valori derivati dai fogli (10 utenti, 8 autisti, 8 mezzi, 4 compagnie,
40 clienti, 6 officine, 30 missioni; 16 eseguite e EUR 1.509,71) descrivono
solo il file sorgente locale, non una verifica di importazione nel database.
Le verifiche degli hash BCrypt sono storiche e non hanno riportato password in
chiaro. Non sono state modificate le credenziali del servizio PostgreSQL locale.

## Comandi riproducibili

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
python -m unittest discover
node --check src/main/resources/static/app.js
python tools/validate_docs.py
python tools/build_docs.py
git diff --check
```

Eseguire `python -m unittest discover` dalla directory `chatbot`. Eseguire i
comandi Java con JDK 25 e quelli documentali dalla root del repository.
L'esito dei comandi non ancora rieseguiti dopo la revisione documentale sarà
registrato separatamente nella risposta di audit; non inferirlo da una
precedente baseline.

## Aggiornamento hardening e prove del 04/11/2026

Baseline revisione:5a16da8. Prove locali finali con Temurin25.0.5+8, Maven3.9.9,
PostgreSQL 18.6, Python3.14.6, Node24.15.0, Playwright1.64.1/Chromium156.

| Suite | Test | Esito |
|---|---:|---|
| ApiSecurityTests / ChatApiSecurityTests / ApiContractTests | 14 / 6 / 11 | PASS |
| OpenApiContractTests | 33 | PASS |
| ChatPostgresIntegrationTests | 15 | PASS |
| MapPostgresIntegrationTests / MapProviderClientsTests | 13 / 11 | PASS |
| MissionPostgresIntegrationTests / MissionServiceTests | 4 / 1 | PASS |
| PostgresHttpWorkflowTests | 5 | PASS |
| ChatSupportTests / PricingServiceTests / RateLimitTests | 2 / 1 / 1 | PASS |
| FornitoriApplicationTests | 1 | PASS |
| Totale Java | 118 | PASS;0failure,0error,0skip |
| Node tools/test_frontend.cjs | 4 | PASS |
| Chromium browser_e2e.py | 2 | PASS; commit DB e lettura indipendente |
| Python chatbot unittest discover | 64 | PASS |
| OSV | 124 versioni Maven | 0 advisory corrispondenti nella scansione locale |

`clean verify` con datasource temporaneo e Java 25; report XML in target/surefire-reports.
Log locale `%TEMP%/fornitori-hardening-20261104/maven-postgres.log` (pgdata-05).
Browser finale: `%TEMP%/fornitori-e2e-qubw7b5l/browser-tests.log`, screenshot centrale
completata/storico autista; app avviata con ruolo e2e_app non-superuser. I primi
runner browser fallivano per selettore nascosto e timestamp simulato incompatibile
col CHECK deadline; corretti i test e rieseguiti, senza disabilitare vincoli.
8 nuovi MVC pre-fix:4 failure; risultato post-fix PASS. Nessuna evidenza inventata
per i regression test aggiunti dopo il fix. Il browser prova l'intero percorso;
MockMvc transazionale/flush non è presentato come commit esterno.
Lo snapshot iniziale di questa revisione rilevava il servizio locale ma non aveva
ancora schema/dati; provider live e WhatsApp non erano eseguiti. Questa situazione
è superata dalle verifiche riportate nella tabella finale. I log integrali non
sono deliverable e non vanno versionati: possono contenere dati sintetici o token
di test.
Non versionare i log integrali, che possono contenere dati sintetici o token di test.

### CI del fix finale e integrazione successiva

[Run 41808673238](https://github.com/coluccimarco/fornitori-soccorso/actions/runs/41808673238)
su `f1f84fc`: entrambi i job PASS, incluso browser con commit PostgreSQL. Il
successivo merge di `origin/main` integrava allora il merge esterno della PR #5;
quella nota non descrive gli ulteriori aggiornamenti frontend e CI registrati
nella tabella finale.

### Verifiche aggiornate — 04/11/2026

| Controllo | Evidenza | Esito |
|---|---|---|
| Sincronizzazione Git | `git fetch origin --prune` riuscito; `origin/main=bb8e548` integrato in `feature/backend` senza conflitti; push del branch al commit `ca50e8b` | PASS; branch locale e `origin/feature/backend` allineati |
| CI su `feature/backend` | [Run `41826245713`](https://github.com/coluccimarco/fornitori-soccorso/actions/runs/41826245713), commit `ca50e8b`, workflow `Backend CI`; job Java 25/contract quality e job Java 25/PostgreSQL baseline | PASS: entrambi i job, incluso il flusso browser con PostgreSQL |
| CI storica su main | Run `41811689974`, commit `69ead57`, workflow `Backend CI` | PASS, ma antecedente alle modifiche al workflow |
| Maven Java 25 locale | `clean verify`, Temurin 25.0.5+8 e datasource locale `fornitori`; 145 test, 0 failure/error/skip | PASS |
| PostgreSQL/Playwright | `tools/run_isolated_browser.py`, PostgreSQL 18 temporaneo; ordine standard e inverso, eseguito dopo merge di `bb8e548` | PASS: 2/2 per ordine |
| Round-trip UI/API/DB | UI crea cliente e missione; test SQL indipendente verifica la persistenza; API/UI mostrano accettazione, completamento e storico | PASS nei due scenari browser |
| Privacy driver | PII oscurate prima dell'accettazione, visibili solo al driver assegnato dopo; altro driver riceve 403 | PASS nei test browser, entrambi gli ordini |
| Frontend | `node --test tools/test_frontend.cjs`; `node --check src/main/resources/static/app.js` | PASS: 4 test e sintassi valida |
| Chatbot | `python -m unittest discover` dalla directory `chatbot/` | PASS: 74 test |
| Documentazione | `python tools/validate_docs.py` dopo gli aggiornamenti | PASS; rendering visivo Word NOT RUN |
| CI ruoli DB | Workflow YAML usa un ruolo PostgreSQL non-superuser; run `41826245713` sul commit `ca50e8b` completa entrambi i job | PASS su GitHub Actions |
| Database personale | PostgreSQL 18.6 su localhost:5432, ruolo `fornitori_app` non-superuser; 15 tabelle, 30 missioni, 16 foreign key | PASS; assertion baseline passata prima e dopo `clean verify` |
| Docker Desktop | `docker info` restituisce HTTP 500 | UNAVAILABLE; test browser usano PostgreSQL locale temporaneo, non Docker |

Il run `41809611044` aveva Java/PostgreSQL PASS e browser FAIL sul codice
precedente; il run `41811689974` su `69ead57` è PASS ma antecedente al workflow
aggiornato. Il run `41826245713` sul commit pubblicato `ca50e8b` ha entrambi i
job PASS, incluso PostgreSQL/browser. Le verifiche browser locali sono state eseguite su un database
temporaneo, mai sul database personale. Il controllo backend `mvnw clean verify`
collega l'app al database locale; l'assertion della baseline viene eseguita dopo
la suite per confermare la coerenza dei dati.

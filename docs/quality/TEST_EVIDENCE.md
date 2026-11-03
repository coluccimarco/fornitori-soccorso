# Evidenze di test

**Versione:** 2.0 · **Data:** 03/11/2026

Questo registro separa evidenza CI, esecuzione locale e verifica non eseguita.
Il codice presente o lo stato OpenAPI non sono da soli prova di funzionamento.

## Baseline verificata

| Controllo | Evidenza | Esito |
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

Il test DB locale non è stato aggirato usando credenziali non autorizzate. La CI
PostgreSQL 18 è evidenza separata, non equivalente a un test sul server locale.
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

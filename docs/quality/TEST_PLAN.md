# Piano test e collaudi

**Codice:** FOR-TEST-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Owner proposto:** M1 con contributi dei responsabili componenti

## Strategia e pass/fail

| Livello | Perimetro | Runner/evidenza | Stato |
|---|---|---|---|
| Unit/service Java | prezzi, validazioni, transizioni | Maven Surefire, test `src/test/java` | Presente |
| Web/security Java | sessione, CSRF, ruoli, DTO, errori | `ApiSecurityTests`, `ChatApiSecurityTests` | Presente |
| PostgreSQL integration | schema, missioni, chat, concorrenza e query | Maven Surefire con PostgreSQL 18.6 locale; database isolati per i test browser | PASS locale; vedere `TEST_EVIDENCE.md` |
| Bot Python | dialogo, backend HTTP, notifiche e Telegram channel | `python -m unittest discover` in `chatbot/` | PASS: 74 test locali il 04/11/2026; incluso nel workflow |
| Contract/static | YAML, metadati status/role, package, Java target | step workflow `Backend CI` | CI PASS |
| UI/browser | Flusso centrale/autista e sicurezza con DB isolato | `tools/run_isolated_browser.py` + Chromium + PostgreSQL 18 temporaneo | 2 scenari PASS in ordine standard e inverso; accessibilità completa NOT RUN |
| Mappe | `MapProviderClientsTests`, `MapPostgresIntegrationTests` | API mappe testata in CI; provider live e mappa browser non inclusi | PASS automatico; live/UI NOT RUN |
| WhatsApp/webhook | adapter e provider | Non implementati | NOT RUN |

Un test saltato è distinto da PASS. Il run GitHub Actions `41811689974` su
`69ead57` è evidenza storica e non copre le modifiche locali successive. La
configurazione aggiornata del workflow va verificata in una nuova esecuzione CI.

## Comandi

```powershell
.\mvnw.cmd clean verify
Push-Location chatbot
python -m unittest discover
Pop-Location
$env:PG_BIN = "C:\Program Files\PostgreSQL\18\bin"
$env:JAVA_HOME = "$HOME\.jdks\temurin-25.0.5+8"
python tools\run_isolated_browser.py
git diff --check
```

Eseguire la suite Python dalla directory `chatbot`. Il runner browser crea un
cluster PostgreSQL temporaneo su una porta loopback dedicata e non usa il
database locale `fornitori`; richiede le dipendenze in
`tools/requirements-e2e.txt` e Chromium Playwright. Eseguire Maven prima del
runner, che usa il JAR in `target/`.

Il risultato locale per la baseline integrata e i conteggi sono riportati in
[`TEST_EVIDENCE.md`](./TEST_EVIDENCE.md); il run CI remoto con PostgreSQL 18 è
separato.

## Gate di release didattica

- Requisiti primari e acceptance approvati.
- Build e test unit/web/database richiesti verdi su versioni dichiarate.
- API contrattuali e consumer sincronizzati.
- Nessun segreto o PII esposta; security/privacy review per limitazioni del bot.
- UI e provider richiesti dimostrati realmente, oppure dichiarati non in scope.
- Run/test/evidenza collegati al commit.
- Manuali e DOCX generati; indice Word aggiornato in Word quando richiesto.

## Riferimenti test esistenti

- Java: `ApiSecurityTests`, `ChatApiSecurityTests`, `FornitoriApplicationTests`,
  `MissionPostgresIntegrationTests`, `ChatPostgresIntegrationTests`,
  `MissionServiceTests`, `PricingServiceTests`, `ChatSupportTests`.
- Python: `test_dialogs.py`, `test_http_backend.py`, `test_notifications.py`,
  `test_telegram_channel.py`.
- Casi: [`TEST_CASES.md`](./TEST_CASES.md); evidenze: [`TEST_EVIDENCE.md`](./TEST_EVIDENCE.md).

## Aggiornamento hardening e prove del 04/11/2026

Nuovi gate: ApiContractTests (validazione/ruoli/errori), OpenApiContractTests
(33 test di contratto e mapping), PostgresHttpWorkflowTests (login BCrypt, CSRF, scritture e
riletture, ownership, scadenza e overflow), RateLimitTests; estensioni provider,
chat e doppia accettazione concorrente. `tools/test_frontend.cjs`:4 test Node.
`tools/browser_e2e.py`:2 scenari Chromium con database isolato e connessione SQL
indipendente; `run_isolated_browser.py` gestisce startup e teardown locali.
La CI esegue anche Python, Node, scanner OSV, validazione docs e browser/PG.
Il piano non attribuisce PASS alle suite in base alla sola presenza del file;
esiti aggiornati in TEST_EVIDENCE. Provider live, accessibilità completa e browser
multipli restano NOT RUN. Nessun test disabilitato per ottenere il verde.

Il controllo del 04/11/2026 ha inoltre verificato il flusso browser completo:
creazione cliente e missione dal client, proposta/accettazione/completamento,
riscontro indipendente delle scritture in PostgreSQL e rilettura dei dati nella
UI; il secondo scenario verifica rifiuto, scadenza, riproposizione e permessi.
Entrambi gli scenari sono passati sia nell'ordine standard sia nell'ordine
inverso su database PostgreSQL isolati. Nessun test sorgente è stato rimosso:
le suite presenti sono referenziate dal build o dai runner di progetto.

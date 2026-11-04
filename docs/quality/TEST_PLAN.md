# Piano test e collaudi

**Codice:** FOR-TEST-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Owner proposto:** M1 con contributi dei responsabili componenti

## Strategia e pass/fail

| Livello | Perimetro | Runner/evidenza | Stato |
|---|---|---|---|
| Unit/service Java | prezzi, validazioni, transizioni | Maven Surefire, test `src/test/java` | Presente |
| Web/security Java | sessione, CSRF, ruoli, DTO, errori | `ApiSecurityTests`, `ChatApiSecurityTests` | Presente |
| PostgreSQL integration | schema, missioni, chat, concorrenza e query | `MissionPostgresIntegrationTests`, `ChatPostgresIntegrationTests`; servizio PostgreSQL 18 CI | CI PASS nel run latest; non eseguito localmente |
| Bot Python | dialogo, backend HTTP, notifiche e Telegram channel | `python -m unittest discover` in `chatbot/` | PASS: 64 test locali il 04/11/2026; incluso nel workflow aggiornato |
| Contract/static | YAML, metadati status/role, package, Java target | step workflow `Backend CI` | CI PASS |
| UI/browser | Flusso centrale/autista e sicurezza con DB isolato | tools/browser_e2e.py, runner locale e CI | 2 scenari PASS locali; accessibilità completa NOT RUN |
| Mappe | `MapProviderClientsTests`, `MapPostgresIntegrationTests` | API mappe testata in CI; provider live e mappa browser non inclusi | PASS automatico; live/UI NOT RUN |
| WhatsApp/webhook | adapter e provider | Non implementati | NOT RUN |

Un test saltato è distinto da PASS. L'esito del workflow `41656606659` su `f763d53`
è prova per i job configurati (Java/OpenAPI e PostgreSQL 18), non per browser o
provider esterni reali.
La verifica di integrazione locale PostgreSQL resta separata dai job CI.

## Comandi

```powershell
.\mvnw.cmd clean verify
python -m unittest discover
git diff --check
```

Eseguire `python -m unittest discover` dalla directory `chatbot`. La suite ha restituito
64 test PASS in esecuzione locale. La CI esegue Maven ma il workflow corrente non
esegue la suite Python; aggiungerla è un possibile follow-up di processo, non parte
di questa revisione docs.

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
(32 DTO e mapping), PostgresHttpWorkflowTests (login BCrypt, CSRF, scritture e
riletture, ownership, scadenza e overflow), RateLimitTests; estensioni provider,
chat e doppia accettazione concorrente. `tools/test_frontend.cjs`:4 test Node.
`tools/browser_e2e.py`:2 scenari Chromium con database isolato e connessione SQL
indipendente; `run_isolated_browser.py` gestisce startup e teardown locali.
La CI esegue anche Python, Node, scanner OSV, validazione docs e browser/PG.
Il piano non attribuisce PASS alle suite in base alla sola presenza del file;
esiti aggiornati in TEST_EVIDENCE. Provider live, accessibilità completa e browser
multipli restano NOT RUN. Nessun test disabilitato per ottenere il verde.

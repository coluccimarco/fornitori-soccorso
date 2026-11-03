# Piano test e collaudi

**Codice:** FOR-TEST-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Owner proposto:** M1 con contributi dei responsabili componenti

## Strategia e pass/fail

| Livello | Perimetro | Runner/evidenza | Stato |
|---|---|---|---|
| Unit/service Java | prezzi, validazioni, transizioni | Maven Surefire, test `src/test/java` | Presente |
| Web/security Java | sessione, CSRF, ruoli, DTO, errori | `ApiSecurityTests`, `ChatApiSecurityTests` | Presente |
| PostgreSQL integration | schema, missioni, chat, concorrenza e query | `MissionPostgresIntegrationTests`, `ChatPostgresIntegrationTests`; servizio PostgreSQL 18 CI | CI PASS nel run latest; non eseguito localmente |
| Bot Python | dialogo, backend HTTP, notifiche e Telegram channel | `python -m unittest discover` in `chatbot/` | PASS: 64 test locali il 03/11/2026; non incluso nel workflow CI |
| Contract/static | YAML, metadati status/role, package, Java target | step workflow `Backend CI` | CI PASS |
| UI/browser | UX, accessibilità, browser | Nessuna UI nel repository | NOT RUN / non disponibile |
| Mappe/provider WhatsApp | adapter e provider | Non implementati | NOT RUN |

Un test saltato è distinto da PASS. L'esito del workflow `41651140562` su `25c613b`
è prova per i job configurati (Java/OpenAPI e PostgreSQL 18), non per parti assenti.
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

Su Temurin 25.0.5 `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`
ha restituito BUILD SUCCESS, 38 test contabilizzati, 0 failure/error, 21 pass e
17 skip (13 chat PostgreSQL, 3 missioni PostgreSQL e 1 application-context in
assenza della configurazione DB locale). Il run CI remoto con PostgreSQL 18 è
separato e risulta PASS.

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
- Piano e casi storici: [`docs/test/TEST_PLAN.md`](../test/TEST_PLAN.md),
  [`docs/test/TEST_CASES.md`](../test/TEST_CASES.md),
  [`TEST_EVIDENCE.md`](../TEST_EVIDENCE.md).

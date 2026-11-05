# Rapporto dei test di sicurezza

**Data:** 04/11/2026 · **Ambito:** backend FORNITORI (base `62e0943`) · **Correzioni:** commit `a5be483`

## Risultati

| Prova | Prima delle correzioni | Dopo |
|---|---|---|
| `./mvnw clean verify`, Java 25, PostgreSQL 18.6 | 134 test: 16 falliti, 5 in errore | **138 test: 0 falliti, 0 errori, 0 saltati** |
| Stessi test con ruolo a soli DML, come nella pipeline, dopo la fascia delle 22:00 | — | **139 test: 0 falliti, 0 errori, 0 saltati** |
| Test del chatbot | Assenti | 6 superati |
| Flussi HTTP reali sul jar | — | 154 su 154 |
| Stessi flussi con ruolo DML, senza script 09 | — | 154 su 154; DDL negato |
| Controlli nel browser Chromium, con CSP attiva | — | 41 su 41 |
| Motore del bot reale con backend reale, invio simulato | — | Superato, anche con lookup scaduto |
| Script 01–09 su DB vuoto; 09 applicato dopo l'uso e rieseguito | — | Superato |

I comandi sono stati eseguiti con `./mvnw` su macOS.

## Ultima esecuzione

Il 05/11/2026, su `main` dopo l'integrazione dell'operatività missioni, con Java 25 e PostgreSQL 18.6 su Windows.

| Prova | Esito |
|---|---|
| `.\mvnw.cmd clean verify` con `FORNITORI_TEST_DATABASE=isolated` e ruolo a soli DML | **176 test: 0 falliti, 0 errori, 0 saltati** |
| Test del chatbot, `python -m unittest discover` | 30 superati |
| Script 01–11 su database vuoto | Superato |
| Pipeline GitHub Actions su `main` | Superata |

Sono ripetuti solo i test automatici: i flussi HTTP e i controlli nel browser della tabella precedente non sono stati rifatti.

## Difetti riprodotti prima delle correzioni

| Test | Atteso | Ottenuto | Voce |
|---|---|---|---|
| `aLookupAuthorizesOnlyOneNewRequestButRetriesStayIdempotent` | 403 | 201 | V-02 |
| `aKnownPlateDoesNotResetTheFailureCount` | 429 | 200 | V-03 |
| `knownPlatesCannotBeHarvestedInOneSession` | 429 | 200 | V-03 |
| `aStaleDispatcherCannotOverwriteTheCurrentAttempt` | Esito ignorato | Campo assente | V-04 |
| `acceptanceQueuesOneNotificationAndClaimReservesIt` | Campo `tentativo` | Assente | V-04 |
| `failuresRetryAfterADelayAndStopAfterFiveAttempts` | Campo `tentativo` | Assente | V-04 |
| `unknownApiPathGivesNotFoundToAnAuthorizedUser` | 404 | Stato errato | V-05 |
| `unexpectedErrorsGiveAGenericJsonWithoutInternals` | 500 | Stato errato | V-05 |
| `repeatedFailuresOnOneAccountAreLimitedAcrossAddresses` | 429 | 401 | V-06 |
| `oversizedBodyIsRefusedBeforeReachingTheController` | 413 | Altro | V-07 |
| `chunkedBodyWithoutDeclaredLengthIsRefused` | 411 | Altro | V-07 |
| `securityHeadersAreSentOnPagesAndApis` | CSP presente | Assente | V-08 |
| `aMissionWithoutOpenProposalDoesNotBlockOtherExpirations` | Scadenze eseguite | Eccezione 409 | V-09 |
| `invalidMissionIsRejected`, coordinata `1E-100000` | 400 | 409 | V-10 |
| `geocoderIsNotCalledWithoutAValidLookup` | 403 senza chiamate | 422 dopo la chiamata | V-11 |
| `driverCannotAddNotesBeforeAccepting` | 409 | 201 | V-12 |
| `customerSearchTreatsInputAsText` | 0 risultati con `%` | 40 | V-16 |
| `customerInputIsValidated` | 400 | 201 | V-17 |
| `oversizedCredentialsAreRejectedBeforeHashing` | 400 | 401 | V-18 |

Altri 2 esiti negativi erano errori dei test (espressione sulle chiamate del frontend, risposta del provider simulato senza tipo). Dopo averli
corretti, `invalidProviderValuesAreRejected` ha rivelato V-20.

## Copertura dei punti richiesti

| Punto | Test |
|---|---|
| 1-4 Login, autenticazione, ruoli, sessioni e CSRF | `AuthAndAuthorizationTests`, `ErrorHandlingAndHttpTests` |
| 5, 14 IDOR e dati personali | `AuthAndAuthorizationTests` |
| 6-13 Missioni, proposta, rifiuto, scadenza, riassegnazione, accettazione, chiusura, costi | `MissionLifecycleTests` |
| 15 Chatbot e autorizzazioni | `ChatAndNotificationTests` |
| 16 Notifiche concorrenti | `ChatAndNotificationTests`, 4 dispatcher in parallelo |
| 17 Integrità del database | `MissionLifecycleTests`, SQL diretto e transazioni concorrenti |
| 18 Eccezioni | `ErrorHandlingAndHttpTests` |
| 19 Compatibilità frontend-backend | `ContractAndProviderTests`, 41 controlli nel browser |
| 20 Regressione | 154 flussi HTTP |

## Come ripeterli

I test su database cancellano i dati live: partono solo con `FORNITORI_TEST_DATABASE=isolated` e si fermano se il database non si chiama `fornitori_e2e*`.

```bash
createdb fornitori_e2e_test
```

Applicare gli script di `database/`, impostare `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, quindi:

```bash
FORNITORI_TEST_DATABASE=isolated ./mvnw clean verify
```

Dalla cartella `chatbot/`:

```bash
python3 -m unittest discover
```

Senza quella variabile girano solo i test che non usano il database. I flussi HTTP, i controlli nel browser e la prova del bot usano script temporanei che non sono nel repository.

## Non verificato

| Elemento | Stato |
|---|---|
| Telegram reale | NON VERIFICATO: invio simulato |
| HTTPS e profilo `prod` su un server reale | NON VERIFICATO: provato solo il controllo all'avvio |
| Timeout dei provider | NON VERIFICATO: durata configurata, non provata |
| Script 09 sul database `fornitori` | NON ESEGUITO per regola; provato solo sul seed e su dati di prova |
| Browser diversi da Chromium, accessibilità | NON VERIFICATO |
| Vulnerabilità delle dipendenze | NON VERIFICATO: lo strumento è stato rimosso |

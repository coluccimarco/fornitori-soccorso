# Rapporto di hardening

**Data:** 04/11/2026 · **Ambito:** backend FORNITORI, albero di lavoro locale (base `62e0943`) · **Modifiche:** non committate

## Modifiche applicate

| Area | Modifica | File | Voce |
|---|---|---|---|
| Sessione | Cookie rimosso al logout, sessione solo da cookie | `AuthController.java`, `application.properties` | V-19 |
| Login | 10 falliti al minuto per account, credenziali limitate, log dei falliti senza email | `AuthController.java`, `AuthDtos.java`, `RequestRateLimiter.java` | V-06, V-18 |
| HTTP | CSP, Referrer-Policy, Permissions-Policy, log degli accessi negati | `SecurityConfig.java` | V-08 |
| HTTP | Corpo massimo 64 KiB (413), lunghezza obbligatoria (411) | `RequestBodyLimitFilter.java` (nuovo) | V-07 |
| Errori | Gestore finale con riferimento nei log, database non raggiungibile 503, concorrenza 409 | `ApiExceptionHandler.java` | V-05 |
| Avvio | Controllo della configurazione: blocca in `prod`, avvisa altrove, non stampa valori | `SecureConfigurationCheck.java` (nuovo) | V-15 |
| Chat | Autorizzazione monouso, contatore non più azzerato, 10 ricerche al minuto per sessione, controllo prima del geocoder, formato degli ID | `ChatService.java`, `ChatLookupLimiter.java`, `ChatDtos.java` | V-02, V-03, V-11 |
| Notifiche | L'esito vale solo per il `tentativo` della riserva in corso | `ChatService.java`, `ChatDtos.java`, `notifications.py`, `http_backend.py`, `backend.py`, `fake_backend.py` | V-04 |
| Missioni | Scheduler tollerante alle righe incoerenti, note dell'autista solo su missione accettata, coordinate limitate | `MissionService.java`, `MissionDtos.java` | V-09, V-10, V-12 |
| Anagrafiche | Ricerca letterale e limitata, formati di telefono e targa | `RegistryService.java`, `CustomerRepository.java`, `RegistryDtos.java` | V-16, V-17 |
| Provider | Distanza negativa rifiutata | `OsrmRoutingClient.java` | V-20 |
| Database | Vincoli e 6 indici, additivi e rieseguibili | `09_integrity_and_indexes.sql` (nuovo) | — |
| Frontend | Solo controllo d'integrità su Bootstrap, hash verificati sui file della CDN | `index.html` | V-08 |
| Test | Nuova suite, dipendenze di test | `src/test/`, `chatbot/tests/`, `pom.xml` | — |
| Contratto | Campo `tentativo`, limiti e formati | `api/openapi.yaml` | — |

## Compatibilità

| Consumer | Effetto |
|---|---|
| Frontend | Nessun cambio di percorsi o campi. Un telefono in formato errato ora dà 400. 41 controlli nel browser passati, nessuna violazione CSP |
| Chatbot | **Cambio di contratto:** `POST /chat/notifiche/{id}/esito` richiede `tentativo` e `claim` lo restituisce. Il bot è già adattato: **backend e bot vanno riavviati insieme** |
| Dialogo del bot | Invariato |
| Database esistente | Nessuna colonna nuova: l'app parte anche senza lo script 09, provato |

## Rischi residui

V-01, V-13, V-14, V-15, V-21, V-22, V-23, V-24, V-25: motivi nel [registro](./VULNERABILITY_REGISTER.md).

# Technical Architecture Document — FORNITORI

**Codice:** FOR-TAD-001 · **Versione:** 1.1 · **Data:** 03/11/2026
**Repository baseline:** `main` / `f763d53` · **Responsabile:** M1 tecnico; approvazione architetturale da confermare

## 1. Sintesi

FORNITORI comprende backend Java/Spring con API REST e PostgreSQL, client web
statico servito da Spring Boot e chatbot Python/Telegram. Il backend copre
autenticazione operatore, anagrafiche, missioni, tariffazione, audit, chat e API
di geocodifica/routing. Il client offre pagina pubblica, login, centrale e area
autista; non integra ancora la vista cartografica né tutti i flussi missione.
Telegram usa long polling. La CI verifica Java 25, OpenAPI, schema/seed e test
PostgreSQL 18; l'ultimo run verificato è `41656606659` su `f763d53`.

Questa descrizione separa la presenza del codice dalla verifica runtime/browser,
dall'integrazione esterna e dall'approvazione dei requisiti. Non certifica la
conformità ai documenti del docente né l'idoneità alla produzione.

## 2. Stack e stato

| Tecnologia | Evidenza | Stato |
|---|---|---|
| Java 25 / Spring Boot 4.1.2 | `pom.xml`, workflow Temurin | Implementato; CI PASS |
| Maven Wrapper 3.9.9 | wrapper del repository | Build CI PASS |
| Spring Security / JPA / Validation / WebMVC | starter e configurazione | Implementati |
| PostgreSQL 18 | servizio del workflow | Schema, seed, assertion e test CI PASS |
| Chatbot Python >=3.10 | `chatbot/`, dipendenze stdlib | Codice e test presenti; suite locale PASS, Telegram live non verificato qui |
| OpenAPI 3.0.3 | `api/openapi.yaml` | 34 operation: 30 implementate, 4 progettate |
| Client web | HTML/CSS/JavaScript, Bootstrap 5.3 in `src/main/resources/static/` | Presente; 2 scenari Chromium/PostgreSQL PASS locali (04/11) |
| Mappe | `MapController`, `MapService`, client Nominatim/OSRM | API implementata e testata in CI; non integrata nella UI; provider live non verificato |
| WhatsApp e webhook in ingresso | operazioni OpenAPI `designed` | Non implementati |

## 3. Contesto e container

Utenti responsabile/autista accedono alla pagina pubblica e al client web servito
dallo stesso backend. Il browser invia richieste same-origin con cookie di sessione
e token CSRF. Il backend accede a PostgreSQL; il bot Python usa le API
`/api/v1/chat/*` con `FORNITORI_CHAT_TOKEN` e Telegram long polling. Solo la richiesta
di route invoca Nominatim/OSRM; il client web non chiama l'API mappe.

![Diagramma System Context](../diagrams/rendered/system_context.png)

![Diagramma dei container](../diagrams/rendered/container.png)

## 4. Componenti e flussi

```text
Browser statico -> sessione + CSRF -> REST controller -> DTO/validazione
                                      -> service transazionale -> repository JPA -> PostgreSQL
                                      -> MapService -> Nominatim / OSRM
Python bot -> Fake/HTTP backend -> /api/v1/chat/* -> service backend -> outbox
Telegram long polling -> dialog engine -> outbound notification dispatcher -> Telegram
```

| Componente | Responsabilità | Stato/evidenza |
|---|---|---|
| `controller/`, `dto/` | REST, permessi, validazione e payload | Implementati |
| `service/`, `repository/` | Logica applicativa, query e lock pessimistici | Implementati |
| `entity/`, `enums/` | Persistenza, stati e ruoli | Implementati |
| `config/`, `common/` | Security filter chain e errori API | Implementati |
| `database/` | Schema, seed, verifiche e migrazioni additive chatbot | Implementati |
| `chatbot/fornitori_chatbot/` | Dialogo, fake/HTTP backend, Telegram e notifiche | Implementati; limiti documentati nel README del modulo |
| `src/main/resources/static/` | Pagina pubblica, login, dashboard centrale, anagrafiche, missioni e area autista | Presente; 2 scenari Chromium/PostgreSQL PASS locali (04/11) |
| `MapController` / `MapService` | Geocodifica origine, route base-sinistro-officina e km stimati | API presente; non invocata dal client |
| `NominatimGeocodingClient` / `OsrmRoutingClient` | Geocodifica e routing esterni | Implementati; soggetti a limiti e disponibilità del provider |

## 5. Client web

La pagina `index.html` contiene la vista pubblica, login e area riservata. `app.js`
carica identità e token CSRF, interroga le API same-origin e gestisce gli errori
HTTP. Il client offre dashboard e registri, ricerca/creazione cliente, creazione
missione, proposta ad autisti, azioni autista (accettazione/rifiuto/completamento),
note e storico. Il layout è responsive.

Non risultano chiamate browser a `/api/v1/maps/route`, rendering di una mappa,
rettifica costo/intervento o test automatizzati di browser. La presenza dei file
statici non dimostra che il flusso sia stato provato con un account e database
configurati; screenshot e prova UX restano NOT RUN.

## 6. Backend, sicurezza e workflow

Le API operatore usano sessione HTTP, password BCrypt e protezione CSRF sulle
mutazioni. I ruoli RESPONSABILE/AUTISTA e l'associazione missione-driver sono
verificati lato server; il cookie è HttpOnly/SameSite Lax e l'opzione Secure è
configurabile. `/api/v1/chat/**` è stateless e richiede `FORNITORI_CHAT_TOKEN`.
Il client non accede direttamente al database.

`MissionService` gestisce mutazioni transazionali; repository e vincoli parziali
PostgreSQL proteggono concorrenza/assegnazioni live. Un clock iniettato consente
test deterministici. `MissionExpiryScheduler` opera sulle righe live; le fixture
`is_fixture=TRUE` restano distinte. Stati, proposte e transizioni sono descritti in
[`mission_states.mmd`](../diagrams/mission_states.mmd).

La configurazione del seed è €50 di uscita, €1,20/km, supplemento 0,25 e fascia
20:00–08:00; il servizio usa `Europe/Rome` e `HALF_UP`. Il timestamp applicato va
confermato col docente: il foglio e i log descrivono l'accettazione, il PDF non è
disponibile. L'outbox scrive eventi nella transazione di dominio; il dispatcher
Telegram non garantisce exactly-once. WhatsApp non è implementato.

## 7. Database

Schema in `database/01_schema.sql`, seed in `02_seed.sql`, verifiche in
`03_verifiche.sql` e `04_assert_baseline.sql`; `05_chat_requests.sql` e
`06_chat_recipient.sql` aggiungono schema chatbot. Hibernate usa `validate`, non
auto-update. Le tabelle comprendono utenti, mezzi/autisti, clienti/compagnie,
officine, missioni, proposte, note, eventi, pricing, sessioni chat e outbox.

Importi e distanze usano `NUMERIC`; istanti `TIMESTAMPTZ`; i vincoli comprendono
FK, stati e coordinate, targa normalizzata univoca e protezioni su assegnazioni.
Il servizio mappe può salvare coordinate geocodificate e chilometri stimati,
distinti da quelli effettivi inseriti alla chiusura. Vedi
[`DATABASE_DESIGN.md`](./DATABASE_DESIGN.md), che include il dizionario.

## 8. API e integrazioni

`api/openapi.yaml` è il contratto dettagliato. Contiene 34 operation: 30
`implemented` e 4 `designed` (gestione utenti e webhook in ingresso). L'operation
`GET /api/v1/maps/route` è implementata: riceve missione e, facoltativamente,
autista; controlla autorizzazioni e ownership, geocodifica l'origine se necessario,
calcola il percorso OSRM attraverso base, sinistro e, per traino, officina, quindi
persiste i km stimati.

Nominatim richiede User-Agent e limita il ritmo delle richieste; entrambi i provider
sono servizi pubblici con timeout e disponibilità non garantita. L'indirizzo viene
trasmesso al geocoder esterno. Il workflow CI testa il codice con test automatici,
ma non verifica disponibilità provider live. La UI non consuma l'endpoint e non
mostra una mappa. Webhook Telegram/WhatsApp e gestione utenti restano progettati.
Vedi [`API_SPECIFICATION.md`](./API_SPECIFICATION.md) e
i [contratti d'integrazione](../team/INTEGRATION_CONTRACTS.md).

## 9. Errori, configurazione e logging

Le API restituiscono errori HTTP strutturati, inclusi `401/403` per autenticazione
e autorizzazione. Le credenziali datasource e il token chatbot sono configurati
tramite environment; non registrare password, bearer token o PII non necessarie.
La configurazione locale usa `application.properties`. Centralizzazione log,
metriche, tracing, retention e configurazione production non risultano dimostrati.

## 10. Build, test, CI e deployment

La CI `.github/workflows/backend-ci.yml` ha job Java 25 build/test, package layout
e validazione YAML, più job PostgreSQL 18 con script, assertion baseline e test di
integrazione. Il run `41656606659` sul commit `f763d53` è verde in entrambi i job,
inclusi i test mappa aggiunti. Non certifica browser E2E, provider esterni,
WhatsApp o messa in produzione.

## 11. Limiti e decisioni aperte

- PDF/Excel originali e approvazione docente non disponibili per verifica.
- La lookup chatbot tramite targa non prova la titolarità; la conversazione può
  perdere stato volatile al riavvio.
- UI e workflow browser presenti nel codice, ma non collaudati; rettifica
  intervento/costo e vista cartografica non sono esposti dal client attuale.
- Nominatim/OSRM sono provider pubblici; privacy, rate limit e availability vanno
  valutati prima di un uso reale.
- WhatsApp, webhook ingressi, deployment, backup/restore e osservabilità production
  non risultano implementati o definiti.

## 12. Evoluzioni

Acquisire fonti docente e convalidare l'identità; provare i flussi UI in ambiente
isolato; decidere se integrare visualizzazione mappa, rettifica costo/intervento,
WhatsApp, persistenza conversazioni e infrastruttura. Le alternative non sono
decisioni approvate. Questo documento non dichiara conformità a standard.

## Aggiornamento hardening e prove del 04/11/2026

Il monolite e i layer esistenti restano invariati. `ApiError` risiede in common,
non nel controller auth. I DTO hanno limiti coerenti con SQL; il mapper missione
espone contatti operativi solo negli stati live proposta/accettata. Il service
verifica il driver corrente e la deadline anche su reject. Le chiamate provider
restano fuori dalle transazioni; salvataggi brevi verificano lo snapshot letto.
`ChatLookupGrantRepository` usa SQL parametrizzato nella transazione service,
accanto ai repository JPA, per grant con upsert/expiry. Nessuna nuova dipendenza
runtime. Liste non paginate, aggregati in memoria e service ampio restano debito
esplicito, senza dichiarazioni di miglioramenti prestazionali non misurati.

# Technical Architecture Document — FORNITORI

**Codice:** FOR-TAD-001 · **Versione:** 2.0 · **Data:** 04/11/2026
**Responsabile:** Vincenzo Minervini

## 1. Sintesi

FORNITORI comprende un backend Java/Spring con API REST e PostgreSQL, un client web statico servito da Spring Boot, con
mappa, e un chatbot Python/Telegram. Il backend copre autenticazione, anagrafiche, missioni, tariffazione, audit, chat,
notifiche, geocodifica e percorsi. Il client offre pagina pubblica, login, centrale e area autista. Telegram usa il long
polling. La descrizione non certifica la conformità ai documenti del docente né l'idoneità alla produzione.

## 2. Stack

| Tecnologia | Evidenza | Stato |
|---|---|---|
| Java 25 / Spring Boot 4.1.2 | `pom.xml` | Implementato |
| Maven Wrapper 3.9.9 | wrapper del repository | Implementato |
| Spring Security / JPA / Validation / WebMVC | starter e configurazione | Implementati |
| PostgreSQL 18 | Script in `database/` | Implementato |
| Chatbot Python >=3.10 | `chatbot/`, sola libreria standard | Implementato |
| OpenAPI 3.0.3 | `api/openapi.yaml` | 38 operazioni: 34 implementate, 4 fuori ambito |
| Client web | HTML/CSS/JavaScript, Bootstrap 5.3, Leaflet 1.9 | Implementato |
| Mappe | `MapController`, `MapService`, client Nominatim/OSRM | Implementato |
| WhatsApp e webhook in ingresso | operazioni `designed` | Fuori ambito |

## 3. Contesto e container

Responsabile e autista usano la pagina servita dallo stesso backend, con richieste same-origin, cookie di sessione e token
CSRF. Il backend accede a PostgreSQL; il bot Python usa `/api/v1/chat/*` con `FORNITORI_CHAT_TOKEN` e Telegram in long polling.
Il backend chiama Nominatim e OSRM; il browser scarica i tile della mappa da OpenStreetMap.

![Diagramma System Context](../diagrams/rendered/system_context.png)

![Diagramma dei container](../diagrams/rendered/container.png)

## 4. Componenti e flussi

```text
Browser statico -> sessione + CSRF -> REST controller -> DTO/validazione
                                      -> service transazionale -> repository JPA -> PostgreSQL
                                      -> MapService -> Nominatim / OSRM
Python bot -> Fake/HTTP backend -> /api/v1/chat/* -> service backend -> outbox
Telegram long polling -> dialog engine -> notification dispatcher -> Telegram
```

| Componente | Responsabilità |
|---|---|
| `controller/`, `dto/` | REST, validazione e payload; 6 controller |
| `service/`, `repository/` | Logica applicativa, query e lock pessimistici |
| `AssignmentController`, `NearestDriverService`, `CentreAlertService` | Proposta al più vicino e avvisi alla centrale |
| `NearestWorkshopResolver`, `GeoDistance` | Officina più vicina in linea d'aria per il traino da chat |
| `entity/`, `enums/` | Persistenza, stati e ruoli |
| `config/`, `common/` | Security filter chain ed errori API |
| `database/` | Schema, seed, verifiche e migrazioni |
| `chatbot/fornitori_chatbot/` | Dialogo, backend fake e HTTP, Telegram e notifiche |
| `src/main/resources/static/` | Pagina pubblica, centrale, area autista e mappa |
| `MapController`, `MapService` | Geocodifica, percorso base-sinistro-officina e km stimati |
| `NominatimGeocodingClient`, `OsrmRoutingClient` | Provider esterni, con una richiesta al secondo verso Nominatim |

## 5. Client web

`index.html` contiene vista pubblica, login e area riservata. `app.js` carica identità e token CSRF, interroga le API
same-origin e mostra gli errori del server. Offre dashboard e registri, ricerca e creazione cliente, creazione missione,
proposta manuale o al più vicino, correzione di intervento e officina, storico proposte, rettifica costo, avvisi, mappa
Leaflet e, per l'autista, accettazione, rifiuto, note, chiusura e storico. Avvisi e proposte si aggiornano ogni 15 secondi. Il
layout è responsive.

## 6. Backend, sicurezza e workflow

Le API operatore usano sessione HTTP, BCrypt e CSRF. Ruoli e associazione missione-autista sono verificati dal server. Il cookie è
HttpOnly e SameSite Lax, con `Secure` nel profilo `prod`. `/api/v1/chat/**` è stateless e richiede `FORNITORI_CHAT_TOKEN`.

Le risposte portano CSP, Referrer-Policy e Permissions-Policy; il corpo delle richieste è limitato a 64 KiB. Il login ammette 20
tentativi al minuto per IP e 10 falliti per account; il canale chat limita le ricerche di targa per sessione e ogni riconoscimento vale per
una sola richiesta. Nel profilo `prod` l'avvio si ferma se la configurazione non è sicura; altrove registra un avviso. Il dettaglio
è in [`SECURITY_DESIGN.md`](./SECURITY_DESIGN.md) e in [`docs/security`](../security/BACKEND_SECURITY_AUDIT.md).

`MissionService` gestisce le mutazioni transazionali; repository e vincoli parziali proteggono concorrenza e assegnazioni. Un
clock iniettato gestisce gli istanti. `MissionExpiryScheduler` opera sulle righe live; le fixture restano immutabili. Stati e
transizioni sono in [`mission_states.mmd`](../diagrams/mission_states.mmd).

Il seed prevede €50 di uscita, €1,20/km, supplemento 0,25 e fascia 22:00–08:00 sull'ora di accettazione, in `Europe/Rome`, con
`HALF_UP`; il timestamp e l'inizio alle 22:00, diverso dalle 20:00 del PDF, vanno confermati col docente.

L'outbox scrive gli eventi nella transazione di dominio. Il dispatcher riserva le righe con `POST /chat/notifiche/claim`
(`FOR UPDATE SKIP LOCKED`, riserva di 5 minuti) e registra l'esito, legato al numero di tentativo: un dispatcher in ritardo non può
modificare una riserva successiva. La consegna è "almeno una volta" in caso di crash. Le missioni
della centrale generano righe `SKIPPED`.

## 7. Database

Script in `database/`; Hibernate usa `validate`. Le tabelle comprendono utenti, mezzi e autisti, clienti e compagnie, officine,
missioni, proposte, note, eventi, pricing, richieste chat, autorizzazioni di lookup e outbox. Importi e distanze usano `NUMERIC`,
gli istanti `TIMESTAMPTZ`. Vedi [`DATABASE_DESIGN.md`](./DATABASE_DESIGN.md).

## 8. API e integrazioni

`api/openapi.yaml` è il contratto. Nominatim richiede User-Agent e limita il ritmo; entrambi i provider sono pubblici, senza
disponibilità garantita, e l'indirizzo viene trasmesso al geocoder. Vedi [`API_SPECIFICATION.md`](./API_SPECIFICATION.md) e i
[contratti d'integrazione](../team/INTEGRATION_CONTRACTS.md).

## 9. Errori, configurazione e logging

Gli errori hanno la forma `{status, error, message}`; un errore inatteso dà `500` con un riferimento da cercare nel log, senza
dettagli interni. Credenziali e token arrivano dall'ambiente; non si registrano password,
bearer token o dati personali non necessari. Centralizzazione dei log, metriche e configurazione di produzione sono fuori ambito.

## 10. Build e pipeline

`./mvnw clean package` esegue i test e produce il jar; i test su database partono solo con `FORNITORI_TEST_DATABASE=isolated`.
`.github/workflows/backend-ci.yml` compila il progetto, applica gli script a un PostgreSQL 18 temporaneo, esegue tutti i test su un
database `fornitori_e2e_ci` e verifica l'avvio, sempre con un ruolo senza privilegi di amministratore.

## 11. Limiti e decisioni aperte

- PDF/Excel originali e approvazione del docente non sono disponibili.
- La sola targa identifica il cliente, limite noto della demo; la conversazione perde lo stato al riavvio.
- Nominatim e OSRM sono pubblici: privacy e limiti vanno rivalutati prima di un uso reale.
- WhatsApp, webhook, gestione utenti, deployment, backup e osservabilità sono fuori ambito.
- Liste non paginate, aggregati in memoria e un `MissionService` ampio sono debito tecnico noto.
- Punti di sicurezza ancora aperti: vedi il [registro delle vulnerabilità](../security/VULNERABILITY_REGISTER.md).

# Guide del team e contratti condivisi

**Codice:** FOR-TEAM-001 · **Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabili:** team

FORNITORI gestisce richiesta, assegnazione e chiusura del soccorso stradale,
distinguendo centrale e autista e mantenendo uno storico verificabile. Requisiti
originali e acceptance del docente restano da confermare: non sono nel repository.

## Responsabili

| Membro | Area | Responsabili | Moduli | Guida |
|---|---|---|---|---|
| Membro 1 | Backend | Vincenzo Minervini | `src/main/java`, `database`, `api` | [Backend](./MEMBRO_1_BACKEND.md) |
| Membro 2 | Frontend | Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio | `src/main/resources/static/` | [Frontend](./MEMBRO_2_FRONTEND.md) |
| Membro 3 | Chatbot | Marco Colucci | `chatbot/fornitori_chatbot` | [Chatbot](./MEMBRO_3_CHATBOT.md) |
| Membro 4 | Mappe | Marco Colucci | `MapController`, `MapService`, client Nominatim/OSRM | [Mappe](./MEMBRO_4_MAPS.md) |
| Membro 5 | Notifiche | Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi | Outbox backend, dispatcher del bot | [Notifiche](./MEMBRO_5_NOTIFICATIONS.md) |

## Stack

| Area | Baseline effettiva |
|---|---|
| Backend | Java 25, Spring Boot 4.1.2, Maven Wrapper |
| Database | PostgreSQL 18, script in `database/` |
| API | REST JSON; `api/openapi.yaml` è la fonte normativa |
| UI | HTML/CSS/JavaScript statici serviti dall'app, mappa Leaflet |
| Mappe | Nominatim e OSRM tramite il backend |
| Chat e notifiche | Bot Python su Telegram; WhatsApp e webhook fuori ambito |

## Regole di collaborazione

- Una PR dichiara scope, requisito, prove manuali eseguite e limiti; i consumer
  rivedono ogni cambio di contratto.
- Autorizzazioni, tariffe e assegnazioni restano sul server, mai nel client.
- Le fixture storiche non sono dati live e non vengono mutate.
- Prima dell'integrazione: OpenAPI aggiornato, guide collegate e `git diff --check`.
  Segreti e dati personali non vanno in commit o log.

## Confini

- Il Membro 1 mantiene schema, servizi, sicurezza server-side e contratto OpenAPI.
- Il chatbot usa `HttpBackend`, non connessioni PostgreSQL. La sola targa è un limite
  noto della demo: non prova l'identità del cliente.
- Le mappe non trasformano coordinate mancanti in posizioni inventate.
- Le notifiche separano evento transazionale, invio e provider.

## Fuori ambito

WhatsApp, webhook in ingresso e gestione utenti da interfaccia non fanno parte della
consegna. Le relative operazioni restano `designed` in OpenAPI e il backend non le
espone. I dettagli sono in [INTEGRATION_CONTRACTS.md](./INTEGRATION_CONTRACTS.md).

## Checklist prima della PR

- Requisito `BR-*` o storia collegati; responsabile e consumer identificati.
- API e schema coordinati, OpenAPI aggiornato se il contratto cambia.
- `./mvnw clean package` riuscito e flusso interessato provato a mano.
- Nessun token, password, hash, dato personale o materiale riservato del docente nel commit.
- Documenti correlati aggiornati; nessuna funzione futura presentata come implementata.

# Guide del team e contratti condivisi

FORNITORI gestisce richiesta, assegnazione e chiusura del soccorso stradale,
distinguendo centrale e autista e mantenendo uno storico verificabile. Requisiti
originali e acceptance del docente restano da confermare; non sono nel checkout.

## Stack e collaborazione

| Area | Baseline effettiva |
|---|---|
| Backend | Java 25, Spring Boot come definito in `pom.xml`, Maven Wrapper |
| Database | PostgreSQL 18 nella CI; setup locale e credenziali autorizzate separati |
| API | REST JSON; `api/openapi.yaml` è la fonte normativa |
| UI | HTML/CSS/JavaScript statici serviti dall'app; browser E2E non eseguito |
| Mappe | API backend Nominatim/OSRM; vista mappa nel client non integrata |
| Chat/notifiche | Chatbot Python e Telegram presenti; WhatsApp/webhook non integrati |

- Una PR dichiara scope, requisito/storia, test eseguiti e limiti; i consumer
  rivedono ogni cambio di contratto.
- Non duplicare authorization, tariffe o assegnazioni nel client; fixture storiche
  non sono dati live e non devono essere mutate da scheduler o test.
- Prima dell'integrazione aggiornare OpenAPI, test pertinenti, guide collegate e
  `git diff --check`. Segreti e dati personali non vanno in commit o log.
- Le schede Sprint sono piani, non verbali. Registrare review/retro solo se svolte;
  usare i [template Scrum](../agile/TEMPLATES.md).

La cadenza storica ipotizzava quattro Sprint giornalieri; non è una pianificazione
approvata né prova di meeting o demo. Per stato ed evidenze vedere il
[registro Scrum](../agile/README.md).

| Membro | Ownership | Moduli presenti | Stato e guida |
|---|---|---|---|
| M1 — Backend/DB | Java/Spring, DB, API, missioni, pricing, auth | `src/main/java`, `database`, `api` | [Guida M1](./MEMBRO_1_BACKEND.md); implementato e CI verificata |
| M2 — Frontend | UI centrale e autista | `src/main/resources/static/` | [Guida M2](./MEMBRO_2_FRONTEND.md); codice presente, browser E2E NOT RUN |
| M3 — Chatbot | Dialogo Python e canale Telegram | `chatbot/fornitori_chatbot`, `chatbot/tests` | [Guida M3](./MEMBRO_3_CHATBOT.md); implementato, limiti identità da valutare |
| M4 — Mappe | Geolocalizzazione e routing | `MapController`, `MapService`, client Nominatim/OSRM | [Guida M4](./MEMBRO_4_MAPS.md); API implementata, vista browser assente |
| M5 — Notifiche | Adapter provider, outbox e canali | Backend outbox + dispatcher bot Telegram; webhook/WhatsApp assenti | [Guida M5](./MEMBRO_5_NOTIFICATIONS.md) |

## Ownership e confini

- M1 mantiene schema, servizi, sicurezza server-side e il contratto OpenAPI;
  il consumer interessato rivede ogni breaking change.
- M2 non replica autorizzazione, tariffa o assegnazione nel client; la UI esiste,
  ma la vista mappe e alcuni flussi centrali non sono collegati.
- M3 usa `Backend`/`HttpBackend`, non connessioni PostgreSQL; non identificare il
  cliente solo dalla targa come prova sufficiente.
- M4 non trasforma coordinate mancanti in posizioni inventate; API di routing
  implementata con provider pubblici, soggetti a limiti e disponibilità.
- M5 separa evento transazionale, dispatch e provider; il dispatcher Telegram è
  presente, WhatsApp no.

## Checklist condivisa prima della PR

- Scope e requisito `BR-*`/story collegati; owner e consumer identificati.
- API/schema coordinati e documento OpenAPI aggiornato se il contratto cambia.
- Test pertinenti passati; test non eseguiti esplicitamente marcati BLOCKED/NOT RUN.
- Nessun token, password, hash, PII o dato docente riservato in commit/log.
- `git diff --check`; build/test del modulo e CI controllati.
- Link e stato dei documenti correlati aggiornati; nessuna capability futura
  presentata come implementata.

Le guide individuali sono mantenute nelle pagine collegate;
[INTEGRATION_CONTRACTS.md](./INTEGRATION_CONTRACTS.md) descrive le dipendenze tra
componenti. Nomi degli studenti e assegnazione formale sono da confermare.

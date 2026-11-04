# Manuale tecnico FORNITORI

**Codice:** FOR-TECH-001 · **Versione:** 1.1 · **Data:** 03/11/2026
**Destinatari:** sviluppatori M1–M5 · **Baseline applicativa:** `main` / `f763d53`

## Prerequisiti

- Git e JDK 25; verificare `java -version` e `javac -version`.
- PostgreSQL 18 o compatibile e `psql`; database e credenziali autorizzati.
- Maven Wrapper incluso; non serve installare Maven separatamente.
- Python 3.10+ per chatbot; la suite usa la libreria standard.
- Node.js non è necessario: il frontend HTML/CSS/JavaScript è servito dalle
  risorse statiche Spring Boot e non ha un package manager frontend.

## Configurazione e avvio backend/web

Da PowerShell, nella root del repository:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fornitori"
$env:SPRING_DATASOURCE_USERNAME = "fornitori_app"
$env:SPRING_DATASOURCE_PASSWORD = "<password autorizzata>"
```

Usare credenziali provisionate dall'amministratore. Non incollarle in shell
history, Git, screenshot o log. Hibernate usa `validate`: schema e seed devono
essere già stati applicati. Per un database nuovo, usare gli script nell'ordine
indicato dal [README](../../README.md): `01_schema.sql`, `02_seed.sql`,
`03_verifiche.sql`, `04_assert_baseline.sql`, `05_chat_requests.sql`,
`06_chat_recipient.sql`. Su un database già esistente eseguire soltanto gli script
additivi non ancora applicati; non riapplicare schema/seed senza verificare il
contesto.

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
.\mvnw.cmd spring-boot:run
```

Aprire `http://localhost:8080/`. Spring Boot serve la pagina pubblica e il client
riservato da `src/main/resources/static/`; login, sessione e CSRF sono same-origin.
Non esiste un build Node/npm separato. Per usare l'area riservata servono utenti
validi nel database. L'accesso locale a un DB con credenziali non configurate non
è un test superato.

## Struttura frontend

| File | Responsabilità |
|---|---|
| `src/main/resources/static/index.html` | Pagina pubblica, login, workspace responsabile/autista, modali |
| `src/main/resources/static/app.js` | API fetch, sessione/CSRF, dashboard, registri e flussi missione |
| `src/main/resources/static/styles.css` | Layout area riservata, responsive |
| `src/main/resources/static/public-site.css` | Presentazione del sito pubblico |
| `src/main/resources/static/images/soccorso-stradale.svg` | Illustrazione locale |

Il client comprende consultazione registri, ricerca/creazione clienti, creazione
missione, proposta, accept/reject, note e completamento con km effettivi. La
rettifica costo/intervento e la vista mappa non sono collegate al browser. Non sono
presenti test Playwright o browser automatizzati; non dichiarare l'E2E verificato.

## Mappe e routing

`GET /api/v1/maps/route?missionId=...&driverId=...` è un endpoint backend
autenticato. `driverId` è facoltativo per l'autista; un responsabile può indicare
l'autista. `MapService` compone base del mezzo, sinistro e, per traino, officina.
Se le coordinate dell'origine mancano, `NominatimGeocodingClient` usa Nominatim;
`OsrmRoutingClient` interroga OSRM e restituisce km stimati/geometry. Il backend
può salvare coordinate e km stimati, distinti dai km effettivi di chiusura.

I provider pubblici hanno limiti/availability non garantiti e ricevono l'indirizzo
da geocodificare. Le proprietà configurabili sono `fornitori.maps.nominatim-url`,
`fornitori.maps.user-agent`, `fornitori.maps.osrm-url` e `fornitori.maps.viewbox`; i default sono nel codice.
La ricerca è limitata alla Puglia (`15.8,42.2,18.6,39.7`, cioè `lonMin,latMax,lonMax,latMin`):
senza limite Nominatim può restituire una via omonima di un'altra regione. Impostare la
proprietà vuota per cercare in tutta Italia; fuori area un indirizzo risulta "non trovato".
Gli errori provider sono riportati come servizio non disponibile o indirizzo non
trovato. I test automatizzati coprono client e integrazione con provider simulati;
una chiamata live e la visualizzazione cartografica nel browser restano NOT RUN.

## Suite e controlli

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

La suite Python si esegue dalla cartella `chatbot/`:

```powershell
python -m unittest discover
```

Per rigenerare deliverable documentali: dalla root `python tools/build_docs.py`;
validare con `python tools/validate_docs.py`. Diagrammi: `powershell
-ExecutionPolicy Bypass -File tools/build_diagrams.ps1`. Vedi
[`quality/TEST_PLAN.md`](../quality/TEST_PLAN.md) per i livelli e
[`quality/TEST_EVIDENCE.md`](../quality/TEST_EVIDENCE.md) per i risultati per commit.

## Chatbot

Da `chatbot/`, seguire [`chatbot/README.md`](../../chatbot/README.md) per console,
backend HTTP e Telegram. Backend e bot condividono `FORNITORI_CHAT_TOKEN`; il
segreto non deve essere scritto nel repository o in screenshot. Il client bot usa
solo `/api/v1/chat/*`, mai il database direttamente.

## Sicurezza e troubleshooting

- `401/403`: verificare sessione, token e ruolo; non aggirare i permessi.
- `400/409`: leggere il messaggio e lo stato prima di ritentare una mutazione.
- Errore connessione/avvio DB: controllare servizio, schema, URL e credenziali
  autorizzate; non disabilitare `validate` o l'autenticazione.
- `503` mappe: servizio esterno non disponibile; non assumere distanza e non
  confondere km stimati con km effettivi.
- Non registrare password, bearer token, messaggi Telegram o PII non necessarie.

Branch e PR seguono [`07_GIT_WORKFLOW.md`](../07_GIT_WORKFLOW.md). Per una modifica
coordinare story, contratto API/schema, test, manuali e diagrammi nello stesso
change. Gli esiti non eseguiti vanno indicati come BLOCKED o NOT RUN.

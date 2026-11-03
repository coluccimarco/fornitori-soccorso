# Guida generale del team

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Team Lead<br>
**Indice:** obiettivo · requisiti · componenti · stack · collaborazione · sprint

## Scopo

Fornire convenzioni comuni ai cinque sviluppatori ITS Developer 4.0, riducendo
duplicazioni e conflitti tra componenti.

## Obiettivi e requisiti

FORNITORI coordina richiesta, assegnazione e chiusura del soccorso stradale. Il
sistema dovrà distinguere centrale/autista, mantenere storico verificabile,
calcolare i costi sul backend, proteggere dati personali e integrare canali esterni.
La specifica del corso e il file Excel originale sono fonti di requisiti; i conteggi
attesi non dimostrano l'importazione effettiva nel DB.

Componenti:

1. backend e database;
2. frontend centrale/autista;
3. chatbot a menu guidato;
4. mappe, geocoding e routing;
5. notifiche e adattatori canale.

## Stack condiviso

| Tecnologia | Versione / ruolo |
|---|---|
| Java | 25 LTS obbligatorio per backend |
| Spring Boot | 4.1.2, mantenuto dal POM attuale; nessun upgrade automatico |
| Maven | Wrapper 3.9.9 |
| PostgreSQL | 18 o compatibile |
| API | REST JSON, OpenAPI 3.0.3 in `api/openapi.yaml` |
| UI | HTML/CSS/JavaScript/Bootstrap proposti; selezione da coordinare |
| Mappe | Leaflet + OpenStreetMap; routing OSRM o equivalente da valutare |
| Chat/notifiche | Menu conversazionale; WhatsApp Business/Twilio o Telegram secondo autorizzazione |

## Collaborazione e quality gate

- Un componente ha un responsabile, ma contratti e backend sono condivisi.
- La PR dichiara scope, test eseguiti, limiti e impatti sugli altri componenti.
- `main` contiene solo lavoro integrato e verificato; i branch individuali non
  fanno merge diretto senza review.
- Aggiorna OpenAPI prima di implementare un payload incompatibile.
- Conserva fixture Excel: i dati storici non sono dati live e non devono essere
  modificati da scheduler o test distruttivi.

## Scrum

Il gruppo lavora in quattro Sprint di una giornata: fondamenti, missioni,
integrazioni, QA/presentazione. Stand-up breve, review dimostrabile e retrospettiva
sono registrati nei template di `docs/agile/`.

**Dipendenze:** specifica del corso, OpenAPI, PostgreSQL locale e accesso al
repository GitHub.<br>
**Completamento:** i cinque membri hanno branch e task concordati, rispettano i
contratti, eseguono i quality gate del proprio componente.<br>
**Riferimenti:** [Start here](./00_START_HERE.md), [Git](./07_GIT_WORKFLOW.md),
[contratti](./team/INTEGRATION_CONTRACTS.md).

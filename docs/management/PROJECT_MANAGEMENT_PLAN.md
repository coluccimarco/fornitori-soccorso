# Piano di gestione del progetto

**Codice:** FOR-PMP-001 · **Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** team

## Metodo

Cinque aree di responsabilità e quattro Sprint secondo Scrum adattato. Le schede `docs/agile/SPRINT_1.md`–`SPRINT_4.md`
descrivono obiettivi e attività; per lo stato vedi il [registro Sprint](../agile/README.md).

## Governance

- **Fonte implementativa:** codice sorgente e configurazione.
- **Fonte API:** `api/openapi.yaml`; modifiche concordate tra provider e consumer.
- **Fonte DB:** script versionati in `database/`.
- **Fonte requisiti:** specifica del docente, da acquisire; il BUR è una baseline provvisoria.

## Cadenza operativa

1. Selezionare gli item del backlog dopo aver chiarito valore, responsabile, dipendenze e acceptance.
2. Aggiornare issue e impedimenti, senza cambiare scope in silenzio.
3. Aprire PR con contratti e documentazione, con revisione dei consumer per i cambi di API e schema.
4. Alla review dimostrare la build concreta, separando simulazioni e provider reali.
5. Registrare le retrospettive solo con un verbale reale.

## Gate

| Gate | Condizione minima | Stato |
|---|---|---|
| Specifica | PDF/Excel acquisiti e requisiti confermati | Bloccato sul docente |
| Backend | Build, avvio e script DB | Soddisfatto |
| Contratto | Stati e ruoli OpenAPI coerenti con i controller | 38 operazioni: 34 implementate, 4 fuori ambito |
| Chatbot | Canale dichiarato e provato | Implementato; prova da telefono da ripetere |
| UI e mappe | Prodotto presente e provato | Soddisfatto |
| Rilascio didattico | Acceptance del docente e demo ripetibile | Da confermare |

## Responsabilità e cambi

La [matrice RACI](./RACI_MATRIX.md) individua i responsabili. Ogni variazione ai requisiti aggiorna insieme requisito,
OpenAPI e guide.

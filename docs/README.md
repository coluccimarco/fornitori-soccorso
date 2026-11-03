# FORNITORI — documentazione

Documentazione tecnica e di progetto del Project Work ITS Developer 4.0.
L'indice normativo e il catalogo aggiornato sono in
[00_INDICE_GENERALE.md](./00_INDICE_GENERALE.md).

## Percorsi rapidi

- **Docente e project manager:** [requisiti](./requirements/BUR_FORNITORI.md),
  [funzioni](./requirements/FUD_FORNITORI.md), [architettura](./architecture/TAD_FORNITORI.md),
  [stato e audit](./management/DOCUMENTATION_AUDIT.md), [test](./quality/TEST_PLAN.md).
- **Sviluppatori:** [manuale tecnico](./manuals/MANUALE_TECNICO.md),
  [guide dei membri](./team/), [specifica API](../api/openapi.yaml),
  [Scrum](./agile/SPRINT_1.md).
- **Utenti e demo:** [manuale d'uso](./manuals/MANUALE_UTENTE.md).
- **Consegna editabile:** [documenti Word](./word/); rigenerazione con
  `python tools/build_docs.py`.

## Struttura

| Area | Scopo |
|---|---|
| [management/](./management/) | Audit, governance, rischi, decisioni e registro documentale |
| [requirements/](./requirements/) | Requisiti business, flussi e tracciabilità |
| [architecture/](./architecture/) | Architettura, database, API e sicurezza |
| [team/](./team/) | Responsabilità, onboarding e contratti d'integrazione |
| [agile/](./agile/) | Backlog, schede Sprint e template |
| [quality/](./quality/) | Piano test, evidenze e tracciabilità |
| [manuals/](./manuals/) | Istruzioni utente e tecniche |
| [diagrams/](./diagrams/) | Sorgenti Mermaid; la cartella conserva anche i diagrammi storici |
| [word/](./word/) | File `.docx` generati dai Markdown |
| [templates/](./templates/) | Modelli e registro |

I documenti numerati e i report Sprint nella radice di `docs/` sono conservati
per compatibilità e come evidenze storiche. Quando un documento aggiornato esiste
nelle aree sopra, prevale quello nuovo; la documentazione API normativa resta
[api/openapi.yaml](../api/openapi.yaml) e lo schema resta in
[database/](../database/).

## Regole di lettura

Gli stati sono riferiti alla revisione indicata nel documento:
**IMPLEMENTATO** = codice presente; **VERIFICATO** = esito di una prova identificata;
**PARZIALE**, **PIANIFICATO**, **BLOCCATO** e **DA CONFERMARE** qualificano i limiti.
Codice, contratto e prova sono fonti distinte. L'assenza degli elaborati originali
del docente è registrata nell'audit, non compensata da requisiti inventati.

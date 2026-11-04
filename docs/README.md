# FORNITORI — documentazione

Documentazione tecnica e di progetto del Project Work ITS Developer 4.0. L'indice e il catalogo sono in
[00_INDICE_GENERALE.md](./00_INDICE_GENERALE.md).

## Percorsi rapidi

- **Docente e project manager:** [requisiti](./requirements/BUR_FORNITORI.md),
  [funzioni](./requirements/FUD_FORNITORI.md), [architettura](./architecture/TAD_FORNITORI.md).
- **Sviluppatori:** [manuale tecnico](./manuals/MANUALE_TECNICO.md), [guide del team](./team/),
  [specifica API](../api/openapi.yaml), [registro Scrum](./agile/README.md).
- **Utenti e demo:** [manuale d'uso](./manuals/MANUALE_UTENTE.md).
- **Sicurezza:** [design](./architecture/SECURITY_DESIGN.md), [revisione](./quality/SECURITY_REVIEW.md).
- **Consegna editabile:** [documenti Word](./word/); rigenerazione con `python tools/build_docs.py`.

## Struttura

| Area | Scopo |
|---|---|
| [management/](./management/) | Governance, rischi, decisioni e modifiche |
| [requirements/](./requirements/) | Requisiti business, flussi e tracciabilità |
| [architecture/](./architecture/) | Architettura, database, API e sicurezza |
| [team/](./team/) | Responsabili, guide e contratti d'integrazione |
| [agile/](./agile/) | Registro Scrum, backlog, schede Sprint e template |
| [quality/](./quality/) | Revisione di sicurezza e registri Excel |
| [manuals/](./manuals/) | Istruzioni utente e tecniche |
| [diagrams/](./diagrams/) | Sorgenti Mermaid e rendering |
| [word/](./word/) | File `.docx` generati dai Markdown |
| [templates/](./templates/) | Modelli e registro |

## Regole di lettura

**IMPLEMENTATO** = funzione presente nel codice; **FUORI AMBITO** = esclusa dalla consegna;
**DA CONFERMARE** = dipende dal docente. L'assenza degli elaborati originali del docente non è compensata
da requisiti inventati.

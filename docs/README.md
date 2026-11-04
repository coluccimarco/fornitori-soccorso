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
  [registro Scrum](./agile/README.md).
- **Utenti e demo:** [manuale d'uso](./manuals/MANUALE_UTENTE.md).
- **Qualità:** [piano test](./quality/TEST_PLAN.md),
  [casi](./quality/TEST_CASES.md), [evidenze](./quality/TEST_EVIDENCE.md).
- **Consegna editabile:** [documenti Word](./word/); rigenerazione con
  `python tools/build_docs.py`.

## Struttura

| Area | Scopo |
|---|---|
| [management/](./management/) | Audit, report finale, governance, rischi e decisioni |
| [requirements/](./requirements/) | Requisiti business, flussi e tracciabilità |
| [architecture/](./architecture/) | Architettura, database, API e sicurezza |
| [team/](./team/) | Responsabilità, onboarding e contratti d'integrazione |
| [agile/](./agile/) | Registro Scrum, backlog, schede Sprint e template consolidati |
| [quality/](./quality/) | Piano, casi ed evidenze test, tracciabilità |
| [manuals/](./manuals/) | Istruzioni utente e tecniche |
| [diagrams/](./diagrams/) | Sorgenti Mermaid; la cartella conserva anche i diagrammi storici |
| [word/](./word/) | File `.docx` generati dai Markdown |
| [templates/](./templates/) | Modelli e registro |

I report Sprint e gli artefatti originali utili sono conservati come evidenza
storica; le guide duplicate e i rimandi superati sono stati consolidati nelle
aree sopra. La documentazione API normativa resta
[api/openapi.yaml](../api/openapi.yaml) e lo schema resta in
[database/](../database/).

## Regole di lettura

Gli stati sono riferiti alla revisione indicata nel documento:
**IMPLEMENTATO** = codice presente; **VERIFICATO** = esito di una prova identificata;
**PARZIALE**, **PIANIFICATO**, **BLOCCATO** e **DA CONFERMARE** qualificano i limiti.
Codice, contratto e prova sono fonti distinte. L'assenza degli elaborati originali
del docente è registrata nell'audit, non compensata da requisiti inventati.

## Revisione backend 04/11/2026

- [Audit iniziale](quality/BACKEND_INITIAL_AUDIT.md): fotografia della fase 1.
- [Audit end-to-end](quality/BACKEND_E2E_AUDIT.md): correzioni, matrice flussi, prove e limiti.
- [Security review](quality/SECURITY_REVIEW.md): gravità, mitigazioni e gate aperti.

Gli aggiornamenti datati 04/11 nei canonici prevalgono sulle descrizioni della
baseline 03/11 per i soli componenti modificati. Le fonti docente restano assenti.

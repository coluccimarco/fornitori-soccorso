# FORNITORI — indice generale e registro documentale

**Codice:** FOR-IDX-001 · **Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabili:** team
**Stato:** baseline documentale, non approvata dal docente
**Repository:** [coluccimarco/fornitori-soccorso](https://github.com/coluccimarco/fornitori-soccorso)

## Per iniziare

- [Business Requirements (BUR)](./requirements/BUR_FORNITORI.md)
- [Functional Design (FUD)](./requirements/FUD_FORNITORI.md)
- [Technical Architecture (TAD)](./architecture/TAD_FORNITORI.md)
- [Sprint](./agile/README.md) · [backlog](./agile/PRODUCT_BACKLOG.md)
- [Matrice di tracciabilità](./requirements/REQUIREMENTS_TRACEABILITY.md)
- [Manuale utente](./manuals/MANUALE_UTENTE.md) · [Manuale tecnico](./manuals/MANUALE_TECNICO.md)
- [Guide del team](./team/) · [Word modificabili](./word/)
- [Checklist per l'ambiente reale](./deploy/CHECKLIST_AMBIENTE_REALE.md)

## Catalogo

| Codice | Titolo | Categoria | Markdown autorevole | Word | Responsabili |
|---|---|---|---|---|---|
| FOR-IDX-001 | Indice generale e registro | Navigazione | `docs/00_INDICE_GENERALE.md` | `docs/word/00_INDICE_DOCUMENTAZIONE.docx` | Team |
| FOR-BUR-001 | Business Requirements | Requisiti | `docs/requirements/BUR_FORNITORI.md` | `docs/word/BUR_FORNITORI.docx` | Team, da confermare col docente |
| FOR-FUD-001 | Functional Design | Requisiti | `docs/requirements/FUD_FORNITORI.md` | `docs/word/FUD_FORNITORI.docx` | Team |
| FOR-TAD-001 | Technical Architecture | Architettura | `docs/architecture/TAD_FORNITORI.md` | `docs/word/TAD_FORNITORI.docx` | Vincenzo Minervini |
| FOR-PMP-001 | Piano gestione progetto | Management | `docs/management/PROJECT_MANAGEMENT_PLAN.md` | `docs/word/PROJECT_MANAGEMENT_PLAN.docx` | Team |
| FOR-TEAM-001 | Guide del team | Operativo | `docs/team/` | `docs/word/GUIDA_TEAM.docx` | Membri 1-5 |
| FOR-USER-001 | Manuale utente | Manuale | `docs/manuals/MANUALE_UTENTE.md` | `docs/word/MANUALE_UTENTE.docx` | Frontend e chatbot |
| FOR-TECH-001 | Manuale tecnico | Manuale | `docs/manuals/MANUALE_TECNICO.md` | `docs/word/MANUALE_TECNICO.docx` | Vincenzo Minervini |
| FOR-SEC-002 | Audit di sicurezza del backend | Sicurezza | `docs/security/` | `docs/word/AUDIT_SICUREZZA.docx` | Vincenzo Minervini |
| FOR-TRACE-001 | Matrice requisiti | Tracciabilità | `docs/requirements/REQUIREMENTS_TRACEABILITY.md` | — | Team |
| FOR-RSK-001 | Registro rischi | Management | `docs/management/RISK_REGISTER.md` | — | Team |
| FOR-DEC-001 | Registro decisioni | Management | `docs/management/DECISION_LOG.md` | — | Team |
| FOR-DATA-001 | Matrici di lavoro | Workbook | `docs/quality/registri.xlsx` | — | Team |

## Fonti

| Fonte | Autorità | Limiti |
|---|---|---|
| `pom.xml`, codice Java e Python, configurazione | Implementazione presente | — |
| `api/openapi.yaml` | Contratto HTTP | Le operazioni `designed` sono fuori ambito |
| `database/*.sql` | Schema e dati dimostrativi | Da applicare in ordine |
| Specifiche PDF/Excel del docente | Fonte primaria prevista | Non presenti nel repository: requisiti da confermare |

## Word

L'indice dei Word è un campo aggiornabile: in Microsoft Word, clic destro sull'indice,
**Aggiorna campo → Aggiorna intero indice**.

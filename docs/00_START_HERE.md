# FORNITORI — start here

Per la navigazione aggiornata aprire l'[indice generale](./00_INDICE_GENERALE.md)
o la [documentazione per area](./README.md).

## Stato integrato

Baseline documentale aggiornata al commit `25c613b` su `main`:

- **IMPLEMENTATO:** backend Java 25/Spring Boot, API missioni/chat, PostgreSQL,
  bot Python console/Telegram e notifiche Telegram supportate.
- **VERIFICATO:** i due job CI Java/OpenAPI e PostgreSQL 18 del run `41651140562`
  sono verdi; questo non verifica UI o provider non inclusi nei job.
- **PIANIFICATO/non presente:** frontend web, mappe/routing e WhatsApp.
- **DA CONFERMARE:** requisiti originali del docente; il PDF e l'Excel non erano
  disponibili nella working tree sottoposta ad audit.

Il repository non contiene una UI browser: non cercare schermate o procedure
operative della centrale/autista come se fossero già disponibili. Il bot Telegram
è descritto in [chatbot/README.md](../chatbot/README.md); la prova reale è indicata
nel documento del modulo e non è stata ripetuta dall'audit.

## Percorsi

- **Project manager/docente:** [BUR](./requirements/BUR_FORNITORI.md),
  [FUD](./requirements/FUD_FORNITORI.md), [TAD](./architecture/TAD_FORNITORI.md),
  [audit](./management/DOCUMENTATION_AUDIT.md),
  [report conclusivo](./management/FINAL_REVIEW_REPORT.md), [Scrum](./agile/README.md).
- **Sviluppatori:** [manuale tecnico](./manuals/MANUALE_TECNICO.md),
  [guide membri](./team/), [OpenAPI](../api/openapi.yaml), [piano test](./quality/TEST_PLAN.md).
- **Consegna:** [Word modificabili](./word/) e
  [rigenerazione](./templates/WORD_STYLE_GUIDE.md).

**Branch di documentazione:** `documentazione`, creato dopo il passaggio a `main`
aggiornato (fast-forward a `25c613b`); non è stato eseguito push o merge.

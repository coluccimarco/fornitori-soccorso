# Ambito e criteri di governo dei requisiti

**Codice:** FOR-SCP-001 · **Versione:** 2.0 · **Data:** 04/11/2026
**Stato:** baseline estratta dal repository, non specifica approvata

## Perimetro

Il backend implementa autenticazione e sessione, ruoli, anagrafiche, workflow missioni, proposte e scadenze, costi,
note, dashboard, storia autista, storico proposte, API chatbot e API mappe. Il client web serve responsabile e autista
con mappa, rettifica costo e correzione officina. Il chatbot Python funziona su console e Telegram con dispatcher delle
notifiche.

WhatsApp, webhook in ingresso e gestione utenti da interfaccia sono fuori ambito.

## Regole di priorità

1. Requisiti obbligatori solo quando la fonte del docente lo dimostra.
2. I valori del seed non equivalgono a regole approvate.
3. Funzioni senza endpoint restano fuori ambito anche se descritte in OpenAPI come `designed`.
4. L'accettazione richiede requisito, criterio osservabile e demo.

Per ID stabili e rimandi: [BUR](../requirements/BUR_FORNITORI.md) e
[matrice](../requirements/REQUIREMENTS_TRACEABILITY.md).

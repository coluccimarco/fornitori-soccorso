# Ambito e criteri di governo dei requisiti

**Codice:** FOR-SCP-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Stato:** baseline estratta dal repository, non specifica approvata

## Perimetro osservabile

Il backend implementa autenticazione/sessione, ruoli, anagrafiche, workflow
missioni, proposte/scadenze, costi, note, dashboard, storia autista, API chatbot
e API mappe Nominatim/OSRM. È presente un client web statico responsabile/autista
e un client chatbot Python per console/Telegram, con backend fake o HTTP e
dispatcher notifiche. CI usa Java 25 e PostgreSQL 18.

Non risultano mappa visuale integrata nel browser, alcuni flussi centrali,
verifica E2E/browser o provider live; WhatsApp e webhook restano progettati.
Non si presume che gli elementi siano richiesti o facoltativi senza la traccia
del docente.

## Regole di priorità

1. Requisiti obbligatori solo quando la fonte docente lo dimostra.
2. Valori del seed/dataset non equivalgono a regole business approvate.
3. Stato `implemented` in OpenAPI descrive il contratto, non è prova di E2E.
4. Funzioni senza endpoint/runtime restano progettate anche se descritte.
5. Accettazione richiede requisito, criterio osservabile, test e prova versionata.

Per ID stabili e rimandi: [BUR](../requirements/BUR_FORNITORI.md) e
[matrice](../requirements/REQUIREMENTS_TRACEABILITY.md).

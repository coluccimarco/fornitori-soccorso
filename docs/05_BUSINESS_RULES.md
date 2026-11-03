# Regole di business

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Product/Backend Lead<br>
**Indice:** stati · assegnazione · timeout · costi · permessi · decisioni

## Scopo e maturità

Le regole sono implementate nel backend del branch `feature/backend` e verificate
dal job PostgreSQL CI.
Le fixture storiche Sprint 1 restano immutabili. Diagramma:
[`diagrams/mission_states.mmd`](./diagrams/mission_states.mmd).

## Stati e transizioni progettate

`nuova → proposta → accettata → eseguita`; da proposta può seguire
`non_accettata` (rifiuto) o `scaduta` (timeout), quindi nuova proposta. Ogni
proposta conserva esito e timestamps. Le transizioni sono esposte dagli endpoint
REST missione e protette da transazioni.

## Assegnazione e concorrenza

- Solo autista live senza altra missione in `proposta`/`accettata` può ricevere proposta.
- Una transazione deve bloccare/validare missione e autista; l'indice parziale DB è
  ulteriore garanzia contro assegnazioni concorrenti.
- Deadline proposta: configurata in `pricing_config` (seed: 10 minuti);
  accettazione rifiutata se `now >= deadline`, anche se scheduler non ha ancora lavorato.
- Scheduler modifica esclusivamente righe `is_fixture=FALSE`; lock pessimisti
  missione/autista e indice parziale proteggono l'assegnazione concorrente.
- Rifiuto e scadenza chiudono la proposta e mantengono lo storico.

## Chiusura e costo

Formula: `(uscita + tariffa_km × km effettivi) × coefficiente_notturno`. Il seed
configura €50 + €1,20/km, moltiplicatore 1,25 nella fascia 20:00–08:00 Europe/Rome,
altrimenti 1. Il backend usa il timestamp di accettazione (decisione D-002 ancora
da confermare col docente), `BigDecimal` e arrotondamento finale `HALF_UP` ai
centesimi. Rettifica solo RESPONSABILE, con motivo e audit, senza sovrascrivere
il costo calcolato.

Dashboard e storico separano fixture e live e aggregano missioni completate
usando il costo finale una sola volta; lo storico dell'autista è limitato al
principal autenticato.

## Autorizzazione

RESPONSABILE crea/assegna/consulta centrale/rettifica; AUTISTA vede solo le proprie
proposte e missioni, risponde alla proposta assegnata e chiude la missione accettata.
La verifica è server-side, non solo UI. Requisiti endpoint sono in OpenAPI e
[contratti](./team/INTEGRATION_CONTRACTS.md).

**Dipendenze:** D-002 e D-003 nel [decision log](./DECISION_LOG.md), lock PostgreSQL,
`Clock` iniettato per test temporali. **Criterio:** testare transizioni valide e
invalide, deadline, concorrenza, rounding, isolamento fixture e permessi. I test
unit/MVC e PostgreSQL CI sono passati; la verifica PostgreSQL locale resta bloccata
dall'assenza di credenziali in questa sessione.

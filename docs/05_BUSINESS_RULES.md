# Regole di business

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Product/Backend Lead<br>
**Indice:** stati · assegnazione · timeout · costi · permessi · decisioni

## Scopo e maturità

Queste regole definiscono il target concordato, non certificano feature implementate.
Mission workflow, timeout e pricing sono **PIANIFICATI per Sprint 2**; fixture
storiche Sprint 1 restano immutabili. Diagramma:
[`diagrams/mission_states.mmd`](./diagrams/mission_states.mmd).

## Stati e transizioni progettate

`nuova → proposta → accettata → eseguita`; da proposta può seguire
`non_accettata` (rifiuto) o `scaduta` (timeout), quindi nuova proposta secondo policy.
Ogni proposta conserva il proprio esito e timestamps. Nessun endpoint corrente
esegue tali transizioni.

## Assegnazione e concorrenza

- Solo autista live senza altra missione in `proposta`/`accettata` può ricevere proposta.
- Una transazione deve bloccare/validare missione e autista; l'indice parziale DB è
  ulteriore garanzia contro assegnazioni concorrenti.
- Deadline proposta: 10 minuti; accettazione rifiutata se `now >= deadline`, anche
  se scheduler non ha ancora lavorato.
- Scheduler modifica esclusivamente righe `is_fixture=FALSE`.
- Rifiuto e scadenza chiudono la proposta e mantengono lo storico.

## Chiusura e costo

Formula attesa: `(50 + 1,20 × km) × coefficiente_notturno`. Il coefficiente è 1,25
nella fascia 20:00–08:00, altrimenti 1. La scelta del timestamp è provvisoriamente
l'ora accettazione secondo Excel; PDF non chiarisce l'istante: **DA CONFERMARE**.
Calcolo server-side con `BigDecimal`, tariffa da configurazione, rounding esplicito
da definire contro esempi Excel. Rettifica solo RESPONSABILE, con motivo e audit,
senza sovrascrivere il costo calcolato.

Dashboard/storico devono separare fixture e live e aggregare solo missioni
completate secondo filtri espliciti. Non sommare due volte costi rettificati.

## Autorizzazione

RESPONSABILE crea/assegna/consulta centrale/rettifica; AUTISTA vede solo le proprie
proposte e missioni, risponde alla proposta assegnata e chiude la missione accettata.
La verifica è server-side, non solo UI. Requisiti endpoint sono in OpenAPI e
[contratti](./team/INTEGRATION_CONTRACTS.md).

**Dipendenze:** D-002 e D-003 nel [decision log](./DECISION_LOG.md), lock PostgreSQL,
`Clock` iniettato per test temporali. **Criterio:** testare transizioni valide e
invalide, deadline, concorrenza, rounding, isolamento fixture e permessi prima di
dichiarare la feature **IMPLEMENTATA**.

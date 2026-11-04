# Report Sprint 2 — backend missioni

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)

## Risultato

Lo Sprint ha implementato il workflow centrale/autista sopra la baseline dello Sprint 1, integrato in `main`.

## Funzionalità

- Creazione e consultazione di missioni live e di riferimento, con validazione di cliente, coordinate, officina,
  canale e chilometri stimati.
- Proposta e riproposizione ad autista libero con timeout configurabile, storico delle risposte, lock pessimisti e
  scadenza schedulata solo sulle missioni live.
- Accettazione con controllo sulla deadline, rifiuto e chiusura protetti dalle transizioni di stato; doppia
  assegnazione impedita dal service e dal DB.
- Costi `BigDecimal` in Europe/Rome, fascia 20:00–08:00, `HALF_UP`; rettifica centrale motivata con audit.
- Note con controllo su ruolo e autista.
- Dashboard reference e live, totale e dettaglio per autista; storico e guadagno dell'autista autenticato.
- Eventi e outbox atomici per accettazione e completamento.

Questo report descrive lo Sprint; lo stato attuale è nel [registro Scrum](./agile/README.md).

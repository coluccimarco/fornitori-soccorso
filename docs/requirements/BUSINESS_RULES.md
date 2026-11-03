# Regole di business — fonte consolidata

Le regole del modello persistito e le transizioni implementate sono descritte nel
[Functional Design](./FUD_FORNITORI.md); le scelte, l'origine e le questioni
aperte sono nel [registro decisioni](../management/DECISION_LOG.md).

## Valori attualmente implementati (non approvazione docente)

- Stati missione: `nuova`, `proposta`, `accettata`, `eseguita`, `non_accettata`,
  `scaduta`; le fixture storiche restano isolate da quelle live.
- Scadenza iniziale: valore seed 10 minuti; scheduler valuta solo missioni live.
- Tariffa seed: uscita €50, €1,20/km, supplemento 25%, fascia 20:00–08:00;
  servizio in `Europe/Rome`, arrotondamento finale `HALF_UP`.
- Accettazione scaduta è impedita dal controllo del tempo; riproposizione conserva
  ogni esito di proposta.
- Rettifica richiede ruolo centrale e motivazione; il costo calcolato e quello
  finale sono distinguibili.

Il timestamp di riferimento per la tariffa e gli estremi della fascia richiedono
conferma contro la specifica docente, che non è disponibile in questa baseline.

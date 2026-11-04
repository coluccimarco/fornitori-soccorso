# Regole di business — fonte consolidata

Le regole del modello persistito e le transizioni sono descritte nel
[Functional Design](./FUD_FORNITORI.md); scelte e questioni aperte nel
[registro decisioni](../management/DECISION_LOG.md).

## Valori implementati (non approvazione docente)

- Stati missione: `nuova`, `proposta`, `accettata`, `eseguita`, `non_accettata`, `scaduta`;
  le fixture storiche restano isolate e immutabili, note comprese.
- Scadenza proposta: valore seed 10 minuti; lo scheduler valuta solo le missioni live.
- Tariffa seed: uscita €50, €1,20/km, supplemento 25%. Fascia notturna dalle 20:00 incluse
  alle 08:00 escluse, sull'ora di accettazione, in `Europe/Rome`; arrotondamento `HALF_UP`.
- Un'accettazione scaduta è impedita; la riproposizione conserva ogni esito di proposta.
- La rettifica richiede ruolo centrale e motivazione; costo calcolato e finale sono distinti.
- L'intervento e l'officina sono correggibili solo su missioni nuove, rifiutate o scadute.
- Contatti del cliente visibili all'autista solo dopo l'accettazione.
- Notifiche al cliente solo per le missioni create dal bot.

Il timestamp di riferimento per la tariffa e gli estremi della fascia richiedono conferma
contro la specifica del docente.

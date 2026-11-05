# Backend: operatività missioni

> **Record storico:** branch già fuso in `main`. Descrive la situazione al momento del merge: lo stato attuale è nel [TAD](../architecture/TAD_FORNITORI.md) e nel [manuale tecnico](../manuals/MANUALE_TECNICO.md).

Branch `feature/backend-operativita`, da fondere **per primo**.

**Hash di partenza:** `d6ab902`. L'atteso era `eb62c65`: i due commit in mezzo aggiungono e tolgono `docs/prompts/`, quindi `git diff eb62c65 d6ab902` è vuoto. I tre branch partono dagli stessi contenuti.

## Migrazione

`database/11_operativita_missioni.sql`: additiva, rieseguibile, non riscrive le missioni esistenti. In CI parte da sola, perché il workflow esegue `database/*.sql` in ordine. **Va applicata prima di avviare il backend**, altrimenti `ddl-auto=validate` blocca l'avvio.

Oltre a quanto chiesto, rende nullable `chat_lookup_grants.customer_id` e aggiunge `targa_norm`: serve al grant `DA_VERIFICARE`, legato a sessione e targa.

`notifications_outbox` non ha una colonna `event_type` con CHECK, quindi `MISSION_CANCELLED` non richiede modifiche.

## Modifiche

| Area | Cosa cambia |
|---|---|
| Stati | `verifica_polizza`, `gestione_manuale`, `annullata`. Al terzo autista distinto che rifiuta o lascia scadere: `gestione_manuale`, evento `MISSION_MANUAL_HANDLING` (`MAX_ESITI_NEGATIVI = 3`) |
| Assegnazione | `AutoAssignmentService`: dopo il commit, su un altro thread, per creazione, rifiuto, scadenza e conferma polizza. Scheduler ogni 60 s. Si spegne con `fornitori.missions.auto-assign=false` |
| Candidati | `disponibile`, senza missione attiva, mai proposti per quella missione, ordinati per distanza dall'officina di riferimento (o dalla base del mezzo) |
| Annullamento | `POST /missioni/{id}/annulla` con le regole economiche del contratto; `adjustCost` esteso agli annullamenti con costo attribuito |
| Km | `/maps/route` include il ritorno: `kmAndata`, `kmTraino`, `kmRitorno`, `kmTotali`, `km`. `km_stimati` salva il totale |
| Soldi | Dashboard, storico autista e stima guadagno usano gli stessi importi |
| Supporto | Note con `tipo`, `POST .../notes/{noteId}/gestita`, `SUPPORTO` in testa a `/centrale/avvisi` |
| Dashboard | `categorie`, `distribuzioneStati`, `codaCritica`, `costi`, rispettando `mode` |
| Chat | `esito`, `targa`, `telefonoSulPosto`, `tipoProblema` (enum `ProblemType`), `inVerifica`, `verifica-polizza`; nome autista sempre `null` |
| Autista | `PATCH /autista/disponibilita`, `disponibile` in `/auth/me`, contatti visibili dalla proposta |
| Clienti | `cognome` in anagrafica, creazione e ricerca |

## Decisioni sui punti ambigui

- **`tipoProblema` obbligatorio e compatibilità col vecchio bot:** il contratto chiede entrambe. Senza `tipoProblema` si accetta il vecchio formato, completo di `clienteId`, `tipoIntervento` e `descrizioneProblema`; con `tipoProblema` sono obbligatori `targa` e `telefonoSulPosto`. Dopo il merge del bot il ramo vecchio si può togliere.
- **Targa all'autista:** il contratto cita nome, cognome e telefoni dalla proposta, non la targa. Resta visibile solo dopo l'accettazione.
- **Contatti dopo la chiusura:** nello storico dell'autista non compaiono.
- **Proposta manuale a un autista non disponibile:** 409, come per chi è occupato. Il contratto parla di `disponibile` solo per l'automatica.
- **Conferma polizza con polizza non attiva:** 409. Cliente mancante o inesistente: 400.
- **Urgenza di `codaCritica`:** verifica polizza, tre rifiuti, supporto, poi anzianità.
- **Supporto su missione chiusa:** resta aperto finché la centrale non lo segna gestito.
- **`/notes/{noteId}/gestita` ripetuto:** 200, senza cambiare chi l'ha gestita.
- **`MISSION_COMPLETED`:** anche qui niente nome autista.
- **STATO da chat:** restituisce `annullata` solo se è l'ultima pratica della chat e non ce ne sono di aperte.
- **`disponibilita` in `/autisti`:** nuovo valore `non_disponibile`.
- **Km mancanti all'annullamento con OSRM fermo:** somma in linea d'aria, ritorno incluso. Senza coordinate: 0 km. In entrambi i casi il costo resta rettificabile.

## Richieste di modifica al contratto

1. **`/centrale/avvisi`, campo `tipo`:** il contratto lo dice nuovo, ma esisteva già con valori `rifiutata`/`scaduta`, quindi cambia significato invece di essere additivo. Ho seguito il contratto (`RIFIUTO`, `SCADENZA`, `SUPPORTO`). Il vecchio `app.js` mostra ogni rifiuto come scadenza finché non si fonde il frontend.
2. **`/maps/route`, campo `km`:** il contratto lo dice esistente, ma quello esistente è `kmStimati`. Ci sono entrambi, uguali a `kmTotali`.
3. **`/centrale/avvisi`:** aggiunti `noteId` e `testo` per i `SUPPORTO`, servono a chiamare `.../gestita`.

## Impatti sugli altri branch

- **Frontend:** `tipo` degli avvisi e `non_disponibile` come sopra. Con l'assegnazione attiva una missione creata passa subito a `proposta`, quindi il pulsante di proposta manuale spesso non serve più.
- **Bot:** finché non è fuso, una targa `DA_VERIFICARE` continua a essere rifiutata lato bot, e il vecchio formato funziona. `kmStimati` include il ritorno.
- **Dati esistenti:** al primo avvio lo scheduler propone tutte le missioni live in `nuova`, `non_accettata` e `scaduta`, anche vecchie. Pulire i dati di prova prima di una demo.
- **Km già salvati:** le missioni precedenti conservano i km di sola andata finché non si riapre la mappa.
- **Documenti:** `docs/` e il PDF dicono ancora che i contatti si vedono dopo l'accettazione; il contratto v1 cambia questa regola. Da aggiornare dopo il merge.

## Limiti noti

- Un indirizzo non geocodificabile viene ritentato a ogni giro dello scheduler (una chiamata a Nominatim al minuto per missione).
- Con meno di 3 autisti che possono rispondere, una missione resta in `non_accettata`/`scaduta` e non arriva a `gestione_manuale`: serve la centrale.
- Una targa inventata apre una pratica in `verifica_polizza`: resta a carico della centrale, con il limitatore attivo.

## Test

```bash
createdb fornitori_e2e_local
for f in database/*.sql; do psql -d fornitori_e2e_local -v ON_ERROR_STOP=1 -f "$f"; done
FORNITORI_TEST_DATABASE=isolated SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fornitori_e2e_local ./mvnw clean verify
```

Nuovi: `AutoAssignmentTests`, `MissionOperationsTests`, `ChatPolicyVerificationTests`. `AutoAssignmentTests` è l'unica classe con l'assegnazione attiva. Test esistenti aggiornati dove il contratto cambia il comportamento: esito nel lookup, nome autista nullo, `tipo` degli avvisi, contatti dalla proposta.

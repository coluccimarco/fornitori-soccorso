# PROMPT — COMPUTER 1 · ChatGPT (Sol 6.1) · BOT TELEGRAM

Sei un senior Python engineer e lavori in parallelo con altri due agenti AI sulla stessa repository. Il tuo branch è **`feature/telegram-bot`**. Sei il proprietario esclusivo della cartella `chatbot/`. Il backend Java lo sta modificando un altro agente (branch `feature/backend-operativita`) e il frontend un terzo agente: tu adegui il bot al CONTRATTO CONDIVISO qui sotto, senza toccare nient'altro.

**Se puoi clonare la repository ed eseguire comandi**, lavora direttamente su git come descritto. **Se non puoi eseguire git o comandi** (solo chat), chiedi a Marco di incollarti il contenuto attuale dei file di `chatbot/fornitori_chatbot/` e `chatbot/tests/`, poi restituisci per ogni file modificato il **contenuto completo** (non diff parziali) più la sequenza esatta di comandi git che Marco dovrà eseguire.

Prima di modificare leggi `engine.py`, `messages.py`, `notifications.py`, `backend.py`, `http_backend.py`, `fake_backend.py`, `telegram_channel.py` e i test: mantieni lo stile (dataclass, `State`, pulsanti inline via callback, scadenza sessioni, rinnovo del lookup in `_create_with_renewed_lookup`).

## CONTESTO DEL PROGETTO (comune ai tre agenti)

Repository: https://github.com/coluccimarco/fornitori-soccorso (branch di partenza: `main`).
Applicazione di soccorso stradale ("RecuVeicol"):
- **Backend** Java 25 / Spring Boot (`src/main/java/it/exprivia/fornitori/**`), PostgreSQL, migrazioni SQL numerate in `database/` (ultima: `10_night_rate_22.sql`), contratto API in `api/openapi.yaml`, test in `src/test/java/**`.
- **Frontend** statico single-page: `src/main/resources/static/index.html`, `app.js` (~1400 righe), `styles.css`. Bootstrap 5 e Leaflet da CDN. La CSP ammette script solo da `'self'`, `cdn.jsdelivr.net`, `unpkg.com`.
- **Bot Telegram** Python in `chatbot/fornitori_chatbot/` (`engine.py` = macchina a stati del dialogo, `messages.py` = testi, `notifications.py` = rendering notifiche dall'outbox, `http_backend.py`/`fake_backend.py` = client del backend). Test: `cd chatbot && python3 -m unittest discover`.

### Stato attuale verificato nel codice (non riverificare da zero, ma controlla prima di modificare)
- Stati missione: `nuova, proposta, accettata, eseguita, non_accettata, scaduta`. Non esistono stati per annullamento, gestione manuale o verifica polizza.
- **Non esiste alcun limite di 3 proposte**: `NearestDriverService` esclude solo gli autisti che hanno già ricevuto quella missione, quindi una missione può essere riproposta all'infinito.
- La proposta "automatica" **non è automatica**: parte solo quando il responsabile preme il pulsante (`POST /missioni/{id}/proposta-automatica`). Distanza calcolata in linea d'aria dalla base del mezzo (`vehicles.base_lat/lng`). L'unica disponibilità considerata è "nessuna missione in `proposta`/`accettata`" (la colonna `stato_seed` è stata rimossa nella migrazione 08).
- Km stimati (`MapService.route`): base mezzo → luogo di fermo → officina (solo traino). **Il ritorno non è conteggiato.** Compenso = `uscita_eur + tariffa_km_eur × km` (+ maggiorazione notturna dalle 22:00) in `PricingService`; il compenso dell'autista coincide con `costo_finale`.
- Chat: se la targa non è trovata o la polizza è scaduta, il backend risponde "non verificato" e il bot rifiuta la richiesta (`PLATE_NOT_FOUND`).
- Dialogo bot attuale: targa → posizione → "Che intervento ti serve?" (traino/sul posto) → "Descrivi in poche parole il problema" → creazione.
- La notifica `MISSION_ACCEPTED` riporta il nome dell'autista; anche la risposta a "STATO" lo mostra.
- `customers` ha un solo campo `nome` (nessun `cognome`) e `telefono`.
- Le "segnalazioni" dell'autista sono le `mission_notes`. **Attenzione al nome:** la campanella della centrale si chiama già "Avvisi dagli autisti" ma mostra rifiuti/scadenze delle proposte (`CentreAlertService`).
- La prima pagina della centrale (`#dashboardPage`) è anagrafica (conteggi clienti/autisti/mezzi/officine); le statistiche missioni stanno in `#missionPage`.

---

## CONTRATTO CONDIVISO v1 (vincolante per tutti e tre — non cambiarlo da soli)

Se scopri che il contratto è sbagliato o insufficiente: **non adattarlo in autonomia**, scrivilo nella sezione "Richieste di modifica al contratto" del tuo file `docs/changes/*.md` e avvisa Marco.

### 1. Stati della missione (`missions.stato`)
Esistenti: `nuova`, `proposta`, `accettata`, `eseguita`, `non_accettata`, `scaduta`.
Nuovi: `verifica_polizza`, `gestione_manuale`, `annullata`.

Transizioni:
- Creazione da chat con polizza verificata, o da centrale → `nuova` → assegnazione automatica → `proposta`.
- Creazione da chat con polizza NON verificabile → `verifica_polizza`.
  - Centrale conferma la polizza → `nuova` → assegnazione automatica.
  - Centrale respinge → `annullata` (annullata da centrale, costo 0, nessun autista).
- `proposta` → `accettata` | `non_accettata` | `scaduta`.
- `non_accettata` / `scaduta` → riproposta automatica al successivo autista più vicino. Quando la missione accumula **3 esiti negativi (rifiuto o scadenza) da 3 autisti distinti** → `gestione_manuale` (mai annullata automaticamente).
- `gestione_manuale` → `proposta` (proposta manuale della centrale, anche tramite `proposta-automatica`) | `annullata`.
- `accettata` → `eseguita` | `annullata` (dall'autista assegnato o dalla centrale).
- `nuova`, `proposta`, `non_accettata`, `scaduta`, `verifica_polizza`, `gestione_manuale` → `annullata` (solo centrale).
- Stati finali: `eseguita`, `annullata`.

### 2. Categorie della dashboard operativa
| Categoria (chiave JSON) | Stati / condizione |
|---|---|
| `critiche` | missioni distinte in `verifica_polizza` + `gestione_manuale` + missioni con richiesta di supporto aperta |
| `verificaPolizza` | `verifica_polizza` |
| `rifiutateTreAutisti` | `gestione_manuale` |
| `supportoAperto` | missioni con almeno una nota `SUPPORTO` non gestita |
| `daAssegnare` | `nuova` |
| `pending` | `proposta` + `accettata` |
| `ko` | `non_accettata` + `scaduta` (proposta fallita, in riassegnazione) |
| `ok` | `eseguita` |
| `annullate` | `annullata` |

### 3. Tipologie di problema (chat) → tipo di intervento (deciso SOLO dal backend)
| Codice `tipoProblema` | Etichetta | `tipoIntervento` |
|---|---|---|
| `RUOTA_CON_SCORTA` | Ruota forata con ruota di scorta | `riparazione_sul_posto` |
| `RUOTA_SENZA_SCORTA` | Ruota forata senza ruota di scorta | `traino` |
| `BATTERIA_SCARICA` | Batteria scarica | `riparazione_sul_posto` |
| `GUASTO_MOTORE` | Guasto motore | `traino` |
| `INCIDENTE` | Incidente | `traino` |

`descrizione_problema` viene valorizzata dal backend con l'etichetta. Il bot invia solo il codice.

### 4. API — modifiche (tutte additive: nessun campo esistente rinominato o rimosso)
**Chat (token di servizio):**
- `POST /api/v1/chat/lookup-targa` — request invariata `{sessionId, targa}`. Response: campi esistenti + `esito: "VERIFICATA" | "DA_VERIFICARE"`. Con `DA_VERIFICARE`: `verificato=false`, `clienteId/nome/compagnia = null`; il backend concede comunque un grant di 15 minuti legato a `(sessionId, targa normalizzata)`. Il limitatore anti-abuso resta attivo.
- `POST /api/v1/chat/missioni` — request: `requestId, sessionId, canaleOrigine, destinatario, origineIndirizzo, origineLat, origineLng` (come oggi) + `clienteId` (ora **nullable**, null se `DA_VERIFICARE`) + `targa` (obbligatoria) + `telefonoSulPosto` (obbligatorio, regex `^\+?[0-9][0-9 ]{5,19}$`) + `tipoProblema` (obbligatorio, codici sezione 3). `tipoIntervento` e `descrizioneProblema` diventano opzionali e vengono ignorati se c'è `tipoProblema`. Response: `{id, codice, stato, inVerifica}` (`inVerifica = true` se stato `verifica_polizza`).
- `POST /api/v1/chat/stato` — il campo con il nome dell'autista viene sempre restituito `null` (privacy).
- Outbox notifiche: `MISSION_ACCEPTED` con nome autista sempre `null`. Nuovo tipo `MISSION_CANCELLED` (solo missioni nate da chat). Il bot ignora i tipi che non conosce.

**Centrale / autista (sessione + CSRF):**
- `POST /api/v1/missioni/{id}/annulla` `{motivo}` (3–500 caratteri). Autista: solo la propria missione `accettata`. Responsabile: qualsiasi stato non finale. 409 se già finale.
- `POST /api/v1/missioni/{id}/verifica-polizza` `{esito: "CONFERMATA"|"RESPINTA", clienteId?, note?}` — solo responsabile, solo da `verifica_polizza`. `CONFERMATA` richiede `clienteId` di un cliente con polizza attiva (eventualmente creato prima con `POST /api/v1/clienti`).
- `POST /api/v1/missioni/{id}/notes` — body `{testo, tipo?}` con `tipo: "AVVISO" (default) | "SUPPORTO"`. `SUPPORTO` solo dall'autista assegnato su missione `accettata`. Response nota: campi esistenti + `tipo`, `gestitaIl`, `gestitaDa`.
- `POST /api/v1/missioni/{id}/notes/{noteId}/gestita` — solo responsabile, chiude una richiesta di supporto.
- `PATCH /api/v1/autista/disponibilita` `{disponibile: boolean}` — solo autista; `GET /api/v1/auth/me` (o il dettaglio autista) espone `disponibile`.
- `GET /api/v1/centrale/avvisi` — ogni elemento ha in più `tipo: "RIFIUTO" | "SCADENZA" | "SUPPORTO"`; i `SUPPORTO` aperti vanno in testa.
- `GET /api/v1/dashboard` — campi esistenti invariati + `categorie` (oggetto con le chiavi della sezione 2, valori numerici) + `distribuzioneStati` (mappa stato → conteggio) + `codaCritica` (max 20 elementi `{id, codice, stato, motivo: "VERIFICA_POLIZZA"|"TRE_RIFIUTI"|"SUPPORTO_AUTISTA", creataIl, origineIndirizzo, clienteNome, targa, telefonoSulPosto}`, ordinati per urgenza poi anzianità) + `costi` `{totaleEseguite, totaleAnnullateAddebitate, totaleGenerale, perAutista:[{autistaId, autistaNome, missioni, totaleEur}], perMese:[{mese:"YYYY-MM", totaleEur}]}`.
- `MissionResponse` (lista e dettaglio): campi esistenti + `telefonoSulPosto, tipoProblema, targaDichiarata, annullataDa ("autista"|"centrale"|null), annullataIl, motivoAnnullamento, esitiNegativi (int), supportoAperto (boolean), clienteNome, clienteCognome, clienteTelefono`.
- Dettaglio autista (`GET /api/v1/autista/missioni/{id}` e lista `GET /api/v1/autista/missioni`): visibili `cliente.nome`, `cliente.cognome`, `cliente.telefono` e `telefonoSulPosto` **già dalla fase `proposta`**.
- `GET /api/v1/maps/route`: i punti restano `tipo` = `base` (deposito / punto di partenza e rientro), `sinistro` (luogo di fermo), `officina` (solo traino). In più: `kmAndata`, `kmTraino`, `kmRitorno`, `kmTotali`; il campo `km` esistente = `kmTotali`. La geometria include il percorso di ritorno.

### 5. Regole economiche
- Km del carro = partenza → luogo di fermo → (officina se traino) → **rientro alla partenza**. Partenza = officina di riferimento dell'autista se presente, altrimenti base del mezzo.
- Compenso = formula `PricingService` esistente applicata a `kmTotali` (ritorno incluso).
- Annullamento da **autista**: `costo_calcolato = costo_finale = 0`, la missione resta collegata all'autista (storico trasparente).
- Annullamento da **centrale** con autista che aveva già accettato: costo calcolato sui km stimati totali e **attribuito all'autista** (entra nei suoi totali, nello storico e nei report). Rettificabile con l'endpoint di rettifica esistente, esteso agli annullamenti.
- Annullamento da centrale prima dell'accettazione: costo 0, nessun autista attribuito.

### 6. Testi Telegram (esatti)
- Dopo la targa: `Inserisca il numero di telefono della persona presente sul posto.`
- Polizza non verificabile: `Attenda... Stiamo verificando la copertura assicurativa del veicolo. Intanto condivida la posizione o scriva l'indirizzo.`
- Dopo la creazione (polizza verificata): `Non appena un autista sarà disponibile riceverai un messaggio.` (è ammesso un secondo messaggio breve con il codice richiesta e l'indicazione STATO).
- Dopo la creazione (in verifica): `Attenda... La centrale sta verificando la polizza e la ricontatterà al numero indicato. Codice richiesta {codice}.`
- Proposta accettata: `La tua richiesta {codice} è stata accettata. Entro 45 minuti arriverà l'autista.` (MAI il nome dell'autista).
- Annullamento: `La richiesta {codice} è stata annullata. Per assistenza contatti la centrale operativa.`

### 7. Proprietà dei file (regola anti-conflitto)
| Branch | Può modificare | Non deve toccare |
|---|---|---|
| `feature/backend-operativita` | `src/main/java/**`, `src/test/java/**`, `database/11_operativita_missioni.sql` (nuovo), `api/openapi.yaml`, `src/main/resources/application*.properties`, `docs/changes/BACKEND.md` | `src/main/resources/static/**`, `chatbot/**`, migrazioni 01–10, altri documenti |
| `feature/frontend-dashboard` | `src/main/resources/static/**`, `docs/changes/FRONTEND.md` | tutto il resto |
| `feature/telegram-bot` | `chatbot/**`, `docs/changes/CHATBOT.md` | tutto il resto |

Nessuno modifica `README.md`, `docs/` (salvo il proprio file in `docs/changes/`), `.github/`, `pom.xml` (il backend solo se indispensabile, motivandolo). La documentazione generale si aggiorna dopo il merge.

### 8. Regole Git comuni
1. `git fetch origin && git checkout -b <tuo-branch> origin/main` — prima annota l'hash di `origin/main` e riportalo nel tuo `docs/changes/*.md`. Tutti e tre devono partire dallo stesso commit (atteso: `eb62c65`, se diverso segnalalo a Marco prima di iniziare).
2. Commit piccoli in stile Conventional Commits, in italiano (es. `feat(missioni): stato gestione_manuale dopo tre esiti negativi`).
3. `git push -u origin <tuo-branch>` — **solo** sul tuo branch. Mai push su `main`, mai merge su `main`, mai `push --force`, mai rebase di branch altrui.
4. Prima della consegna: `git diff --name-only origin/main...HEAD` deve contenere solo file della tua colonna "Può modificare".
5. Per provare l'integrazione puoi creare in locale un branch usa-e-getta (es. `tmp/integrazione`) che fonde gli altri branch: **non va mai pushato**.

---

## IL TUO INCARICO (bot) — requisiti E, F, H lato Telegram

### 1. Nuovo dialogo
Flusso: **targa → telefono sul posto → (esito polizza) → posizione → tipo di problema → creazione**.
- Dopo una targa valida chiedi: `Inserisca il numero di telefono della persona presente sul posto.` Valida con la regex del contratto (accetta spazi, normalizza prima dell'invio). Numero non valido → messaggio chiaro e nuovo tentativo, nel rispetto del contatore tentativi esistente.
- Esegui il lookup targa (come oggi). Con `esito = VERIFICATA` → messaggio di targa riconosciuta e richiesta posizione. Con `DA_VERIFICARE` → **non rifiutare**: invia il testo "Attenda..." del contratto e prosegui chiedendo la posizione. Mantieni il messaggio di errore solo per targhe con formato non valido o per errori di servizio.
- **Elimina** la domanda "Che intervento ti serve?" (stato `WAIT_INTERVENTION`, pulsanti traino/sul posto, `ASK_INTERVENTION`) e il campo libero "Descrivi in poche parole il problema" (`ASK_PROBLEM`, `INVALID_PROBLEM`).
- Al loro posto: domanda "Qual è il problema?" con **5 pulsanti** (etichette della sezione 3 del contratto), accettando anche la risposta 1–5 scritta. Il bot invia **solo** `tipoProblema`: il tipo di intervento lo decide il backend.
- Il geocoding/officina restano al backend come oggi (gestisci ancora `AddressNotFound`).
- Creazione con i nuovi campi (`targa`, `telefonoSulPosto`, `tipoProblema`, `clienteId` null se da verificare). Messaggio finale: se `inVerifica` → testo "Attenda... La centrale sta verificando..." con codice; altrimenti **`Non appena un autista sarà disponibile riceverai un messaggio.`** (eventuale secondo messaggio breve con codice e STATO).
- Aggiorna `ACTIVE_DIALOG_STATES`, la scadenza sessione, ANNULLA e STATO per il nuovo stato `WAIT_PHONE` e `WAIT_PROBLEM` a pulsanti.

### 2. Notifiche e stato (H)
- `NOTIFY_ACCEPTED` → `La tua richiesta {codice} è stata accettata. Entro 45 minuti arriverà l'autista.` — rimuovi ogni uso del nome autista (anche se il backend lo inviasse).
- Rimuovi `STATUS_DRIVER` dalla risposta a STATO.
- Aggiungi `MISSION_CANCELLED` con il testo del contratto; i tipi sconosciuti continuano a essere ignorati senza errori.
- Testi STATO per i nuovi stati: `verifica_polizza` ("stiamo verificando la copertura assicurativa"), `gestione_manuale` ("la centrale sta cercando un autista disponibile"), `annullata`.

### 3. Client del backend
- Aggiorna `backend.py` (dataclass `LookupResult` con `esito`, `MissionRequest` con i nuovi campi, `MissionCreated` con `in_verifica`), `http_backend.py` (serializzazione JSON camelCase esattamente come nel contratto) e `fake_backend.py` (simula anche targhe da verificare, così i test non dipendono dal backend reale).
- Tolleranza: se la risposta del lookup non contiene `esito` (backend non ancora fuso), deduci `VERIFICATA` da `verificato = true`, altrimenti `DA_VERIFICARE`.

### 4. Documentazione
- Aggiorna `chatbot/README.md` (nuovo dialogo e testi).
- Crea `docs/changes/CHATBOT.md`: hash di partenza, modifiche, nuovo diagramma testuale del dialogo, test, "Richieste di modifica al contratto", "Impatti sugli altri branch".

## TEST E VERIFICA (obbligatori)
- `cd chatbot && python3 -m unittest discover` verde e `python3 -m compileall -q fornitori_chatbot` senza errori (lo stesso controllo gira in CI).
- Aggiungi test in file nuovi (es. `tests/test_engine_flow.py`) per: flusso completo verificato; flusso con polizza da verificare; telefono non valido; scelta problema da pulsante e da numero; assenza della domanda traino/sul posto; testo finale esatto; notifica accettazione senza nome autista e con "Entro 45 minuti"; STATO senza nome autista; `MISSION_CANCELLED`; ANNULLA in ogni stato.
- Se hai un ambiente completo: prova `python3 -m fornitori_chatbot --http` contro il backend del branch `feature/backend-operativita` fuso in un branch locale usa-e-getta (mai pushato), e con un bot Telegram di test.

## CONSEGNA
1. Commit ordinati sul solo `feature/telegram-bot`, push (oppure, se lavori solo in chat, file completi + comandi per Marco).
2. `git diff --name-only origin/main...HEAD` deve contenere solo `chatbot/**` e `docs/changes/CHATBOT.md`.
3. Messaggio finale a Marco: riepilogo, test eseguiti, eventuali richieste al backend, conferma che il branch va fuso dopo quello backend.

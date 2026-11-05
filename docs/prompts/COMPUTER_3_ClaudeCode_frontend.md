# PROMPT — COMPUTER 3 · Claude Code (Claude Opus 5.5) · FRONTEND CENTRALE E AUTISTA

Sei un senior frontend engineer e lavori in parallelo con altri due agenti AI sulla stessa repository. Il tuo branch è **`feature/frontend-dashboard`**. Sei il proprietario esclusivo di `src/main/resources/static/**` (index.html, app.js, styles.css, immagini). Il backend lo sta modificando un altro agente sul branch `feature/backend-operativita` secondo il CONTRATTO CONDIVISO qui sotto: tu scrivi l'interfaccia contro quel contratto, senza toccare il codice Java.

Lavora in modo autonomo. Prima di modificare leggi tutto `app.js` e `index.html` e segui le convenzioni esistenti (funzione `safe()` per l'escape, gestione CSRF, `formatEuro`, `formatDateTime`, attributi `data-manager-only`/`data-driver-only`, stile Bootstrap). Niente dati fittizi: tutto deve arrivare dalle API.

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

## IL TUO INCARICO (frontend) — requisiti A, B, C, D, E (azioni centrale), G (vista autista), I (visualizzazione km)

### 1. Nuova dashboard operativa come prima schermata della centrale (A)
- `#dashboardPage` diventa la dashboard operativa basata su `GET /api/v1/dashboard` (`categorie`, `distribuzioneStati`, `codaCritica`).
- In alto, con priorità visiva forte (colore, posizione, badge): **coda critica** — verifica polizza, rifiutate da 3 autisti / gestione manuale, richieste di supporto aperte — con azione diretta su ogni riga (apri missione, proponi, conferma/respingi polizza, segna supporto gestito, annulla).
- Riquadri numerici per: critiche, da assegnare, rifiutate da 3 autisti, KO, OK, pending, annullate. Click su un riquadro → elenco missioni filtrato per quegli stati.
- Grafico della distribuzione per stato (istogramma). Puoi usare Chart.js da `cdn.jsdelivr.net` con versione fissata e attributo `integrity` (la CSP lo consente) oppure SVG/CSS nativo; scegli e motiva in `docs/changes/FRONTEND.md`.
- Aggiornamento periodico (es. 30 s) senza ricaricare la pagina, fermandolo quando la sezione non è visibile.

### 2. Anagrafica spostata in sezione secondaria "Costi e analisi" (A)
- I conteggi anagrafici escono dalla prima pagina. Crea una sezione di navigazione "Costi e analisi" con `costi` della dashboard: totali, grafico per autista, grafico per mese, distinzione missioni eseguite / annullate con costo attribuito. Le pagine anagrafiche esistenti (clienti, autisti, mezzi, compagnie, officine) restano raggiungibili dal menu: non eliminarle.
- Sposta qui anche "Guadagni per autista" oggi in `#missionPage`, senza perdere funzionalità.

### 3. Pagina missioni della centrale (B)
- "Luogo" → **"Luogo di fermo"** in tutte le card e nei dettagli (centrale e autista).
- Pulsanti **"Mostra segnalazioni"**, **"Mappa"**, **"Storico proposte"** affiancati su un'unica riga (responsive: su mobile possono andare a capo in modo ordinato).
- Dati anagrafici del cliente compressi: una riga compatta (nome e cognome · telefono · targa · telefono sul posto), dettagli espandibili.
- Filtro stati aggiornato con i nuovi stati (`verifica_polizza`, `gestione_manuale`, `annullata`) e relative etichette/colori (`.status-*` in styles.css).
- Mappa Leaflet: tre marker distinti e riconoscibili per **Deposito** (`tipo: base`), **Luogo di fermo** (`sinistro`), **Officina** (`officina`): icone diverse (es. `L.divIcon` con colore e simbolo), legenda, popup con nome. Mostra il riepilogo km andata / traino / ritorno / totali dalla risposta di `/maps/route`.

### 4. Avvisi e supporto (C)
- Rinomina le segnalazioni/annotazioni dell'autista in **"Avvisi"** (funzionamento invariato, `tipo: AVVISO`).
- Nuovo pulsante per l'autista **"Richiedi supporto alla centrale"** (`tipo: SUPPORTO`) con modale e testo obbligatorio, graficamente distinto dagli avvisi.
- Lato centrale le richieste di supporto aperte sono evidenziate nella coda critica, nella card della missione e nella campanella. La campanella oggi si chiama "Avvisi dagli autisti" ma mostra rifiuti e scadenze: rinominala **"Notifiche centrale"** per evitare confusione con i nuovi "Avvisi", mostra il `tipo` e metti i `SUPPORTO` in testa. Pulsante "Segna come gestito".

### 5. Annullamento (D)
- Pulsante **"Annulla missione"** con conferma e motivo obbligatorio: per l'autista sulla propria missione accettata (avvisa chiaramente che il compenso sarà 0 €), per la centrale su ogni stato non finale (avvisa che, se l'autista aveva accettato, il costo gli viene attribuito).
- Le missioni annullate mostrano chi ha annullato, quando, il motivo e l'importo attribuito; lo storico autista le include con l'importo corretto.

### 6. Verifica polizza (E, lato centrale)
- Per le missioni `verifica_polizza`: targa dichiarata, telefono sul posto, pulsanti **"Conferma polizza"** (scelta di un cliente esistente tramite la ricerca anagrafica oppure creazione nuovo cliente con il modale esistente, poi `POST /missioni/{id}/verifica-polizza` con `CONFERMATA`) e **"Respingi"**.
- Modale cliente: aggiungi il campo **Cognome**.

### 7. Vista autista (G, I)
- Nella proposta e nella missione in corso: nome, cognome, telefono del cliente e telefono della persona sul posto (link `tel:`), tipo di problema.
- Interruttore **"Disponibile / Non disponibile"** (`PATCH /autista/disponibilita`).
- Chiusura missione: campo "Km totali (andata + ritorno)" precompilato con i km stimati totali.

## COMPATIBILITÀ
- Finché il backend non è fuso, alcuni campi nuovi possono mancare: il codice deve degradare senza errori (campi opzionali, sezioni nascoste se assenti), ma senza dati inventati.
- Non toccare file Java, SQL, `chatbot/`. Se ti serve una modifica lato server (es. CSP), scrivila in `docs/changes/FRONTEND.md` → "Richieste di modifica al contratto" e avvisa Marco.

## TEST E VERIFICA (obbligatori)
- `node --check src/main/resources/static/app.js` senza errori; nessun errore in console.
- Test manuale end-to-end: crea in locale un branch usa-e-getta che fonde `origin/feature/backend-operativita` (quando disponibile), avvia backend + PostgreSQL con le migrazioni 01→11, e verifica con gli account di seed responsabile e autista: dashboard e grafici, coda critica, filtri, mappa con 3 marker, avvisi vs supporto, annullamento da entrambi i ruoli, conferma polizza, vista autista con dati cliente, disponibilità, chiusura con km totali. Prova anche larghezza mobile (≈380 px) e tema/contrasto leggibili.
- Non pushare mai il branch usa-e-getta.
- Documenta in `docs/changes/FRONTEND.md`: hash di partenza, modifiche, scelte, checklist dei test con esito, screenshot facoltativi solo dentro `docs/changes/` se ti servono (non altrove).

## CONSEGNA
1. Commit ordinati sul solo `feature/frontend-dashboard`, push.
2. `git diff --name-only origin/main...HEAD` deve contenere solo `src/main/resources/static/**` e `docs/changes/FRONTEND.md`.
3. Messaggio finale a Marco: riepilogo, test eseguiti, eventuali richieste al backend, conferma che il branch va fuso dopo quello backend.

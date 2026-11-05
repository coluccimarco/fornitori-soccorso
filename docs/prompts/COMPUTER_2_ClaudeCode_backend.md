# PROMPT — COMPUTER 2 · Claude Code (Claude Opus 5.5) · BACKEND E DATABASE

Sei un senior backend engineer Java/Spring e lavori in parallelo con altri due agenti AI sulla stessa repository. Il tuo branch è **`feature/backend-operativita`**. Sei il proprietario di backend, database, contratto OpenAPI e test Java. Gli altri due agenti (frontend e bot Telegram) stanno scrivendo codice contro il CONTRATTO CONDIVISO qui sotto, quindi devi rispettarlo alla lettera: è ciò che garantisce che i tre branch si fondano senza conflitti e funzionino insieme.

Lavora in modo autonomo. Prima di modificare un componente leggi il codice esistente (soprattutto `MissionService`, `NearestDriverService`, `MapService`, `PricingService`, `ChatService`, `CentreAlertService`, le entity e i test esistenti) e riusa ciò che c'è: niente duplicazioni, niente dati fittizi.

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

## IL TUO INCARICO (backend) — requisiti A, C, D, E, F, G, H, I lato server

### 1. Migrazione `database/11_operativita_missioni.sql` (unica migrazione nuova, additiva, idempotente come le precedenti, compatibile con i dati esistenti)
- Ricrea il CHECK di `missions.stato` con i 3 nuovi stati (usa `DROP CONSTRAINT IF EXISTS` + `ADD ... NOT VALID` come in 07/09).
- `missions`: `telefono_sul_posto VARCHAR(32)`, `tipo_problema VARCHAR(30)` con CHECK sui 5 codici, `targa_dichiarata VARCHAR(20)`, `annullata_da VARCHAR(10)` CHECK (`autista`,`centrale`), `annullata_il TIMESTAMPTZ`, `motivo_annullamento TEXT`; `customer_id` nullable solo se `stato = 'verifica_polizza'` o se la missione è stata respinta in verifica (vincolo CHECK coerente).
- `mission_notes`: `tipo VARCHAR(10) NOT NULL DEFAULT 'AVVISO'` CHECK (`AVVISO`,`SUPPORTO`), `gestita_il`, `gestita_da` (FK `app_users`).
- `drivers`: `disponibile BOOLEAN NOT NULL DEFAULT TRUE`, `officina_riferimento_id BIGINT REFERENCES workshops(id)` nullable; popola quest'ultima per gli autisti esistenti con l'officina più vicina alla base del loro mezzo (UPDATE nella stessa migrazione, non toccare `02_seed.sql`).
- `customers`: `cognome VARCHAR(100)` nullable.
- Indici utili per dashboard e assegnazione. Verifica che il CHECK su `notifications_outbox.event_type` (se presente) accetti `MISSION_CANCELLED`.
- Verifica come vengono applicate le migrazioni (CI in `.github/workflows/backend-ci.yml`, test `IsolatedPostgresTest`) e assicurati che la 11 venga eseguita anche lì senza modificare il workflow; se non è possibile, segnalalo.

### 2. Stati, 3 esiti negativi e gestione manuale (A)
- Aggiungi gli stati a `MissionService`/`Mission` e alle liste di stati (`ALL_STATES`, ecc.).
- Conta gli esiti negativi (proposte `non_accettata` o `scaduta`) da autisti distinti; al terzo → `gestione_manuale`, evento `MISSION_MANUAL_HANDLING` con motivo `TRE_RIFIUTI`. Costante unica (`MAX_ESITI_NEGATIVI = 3`).
- Consenti la proposta manuale e la `proposta-automatica` anche da `gestione_manuale`.

### 3. Assegnazione automatica (G)
- Punto di partenza autista = `officina_riferimento` (fallback base mezzo). Candidati: `disponibile = true`, nessuna missione in `proposta`/`accettata`, mai proposti per quella missione. Ordine per distanza dal luogo di fermo.
- Avvio automatico: dopo il commit della creazione (chat verificata o centrale), dopo un rifiuto, dopo una scadenza, dopo la conferma polizza. Usa un meccanismo *after commit* (es. `@TransactionalEventListener(phase = AFTER_COMMIT)`) per non proporre missioni non ancora salvate e per non annullare la transazione principale se l'assegnazione fallisce.
- Scheduler (stile `MissionExpiryScheduler`) che ogni 60 s riprova le missioni `nuova`/`non_accettata`/`scaduta` senza proposta aperta (quando prima non c'era nessun autista libero).
- Gestisci la concorrenza come già fa `NearestDriverService` (lock, conflitto → candidato successivo). Mantieni gli endpoint esistenti.
- `PATCH /api/v1/autista/disponibilita` e campo `disponibile` esposto all'autista.
- Dati cliente all'autista: nome, cognome, telefono e `telefonoSulPosto` visibili dalla fase `proposta` (aggiorna `authorizeMissionAccess`/`driverMissionDetail` e i DTO).

### 4. Annullamento e compensi (D, I)
- `POST /missioni/{id}/annulla` con le regole delle sezioni 1 e 5 del contratto. Chiudi la proposta aperta, registra l'evento `MISSION_CANCELLED` e, per missioni da chat, l'outbox `MISSION_CANCELLED`.
- Km: `MapService` calcola il percorso partenza → fermo → [officina] → partenza con OSRM e restituisce `kmAndata/kmTraino/kmRitorno/kmTotali`; `km_stimati` salva i km totali. Se OSRM non è disponibile mantieni il fallback esistente coerente (incluso il ritorno).
- Annullamento da centrale dopo accettazione: costo sui km stimati totali (se mancanti, calcolali). Estendi `adjustCost` alle missioni annullate con costo attribuito.
- Allinea **tutti** i punti che sommano denaro: `dashboard` (totali e per autista), `driverHistory`, stima guadagno autista (`estimatedEarning`). Nessuna incoerenza tra report centrale e storico autista.

### 5. Supporto autista e avvisi (C)
- Note con `tipo`; `SUPPORTO` crea un evento che compare in testa a `/centrale/avvisi` con `tipo: "SUPPORTO"` finché non viene marcata gestita. Endpoint `.../notes/{noteId}/gestita`.

### 6. Dashboard operativa e costi (A)
- Estendi `GET /api/v1/dashboard` con `categorie`, `distribuzioneStati`, `codaCritica`, `costi` esattamente come da contratto, rispettando il parametro `mode` esistente (live / storico di riferimento). Query efficienti (niente `findById` dentro i cicli dove evitabile).

### 7. Chat e Telegram lato server (E, F, H)
- `lookup-targa` con `esito`; grant per `DA_VERIFICARE` legato a sessione + targa; il limitatore resta.
- `chat/missioni`: nuovi campi, validazione del telefono, mappatura `tipoProblema → tipoIntervento` (unica fonte di verità, enum o mappa nel backend), officina più vicina per il traino come oggi, stato `verifica_polizza` quando manca il cliente verificato, idempotenza/impronta anti-duplicato estese ai nuovi campi. Mantieni la compatibilità con i vecchi campi per la durata del merge.
- `verifica-polizza` (CONFERMATA/RESPINTA) per il responsabile.
- Privacy: nome autista sempre `null` in outbox `MISSION_ACCEPTED` e in `chat/stato`.
- Cliente: `cognome` in DTO anagrafica, creazione cliente e risposte.

### 8. Contratto e documentazione
- Aggiorna `api/openapi.yaml` con tutto quanto sopra (schema, enum, errori).
- Crea `docs/changes/BACKEND.md`: hash di partenza, elenco modifiche, migrazione, endpoint, decisioni prese sui punti ambigui, istruzioni di test, sezione "Richieste di modifica al contratto" (anche vuota) e "Impatti sugli altri branch".

## TEST E VERIFICA (obbligatori prima della consegna)
- `./mvnw clean verify` verde. Aggiungi test in **file nuovi** (es. `MissionOperationsTests.java`, `AutoAssignmentTests.java`, `ChatPolicyVerificationTests.java`) usando l'infrastruttura di `IsolatedPostgresTest`.
- Copri almeno: 3 esiti negativi → `gestione_manuale`; nessuna terza proposta allo stesso autista; scelta del più vicino tra i disponibili (esclusi occupati/non disponibili); annullamento autista = 0 €; annullamento centrale = costo attribuito e presente in dashboard/storico; km con ritorno; mappatura dei 5 problemi; chat con polizza non verificabile → `verifica_polizza` → conferma → `nuova` → proposta; nome autista assente in notifica e stato; supporto aperto in `codaCritica` e avvisi; regressione dei test esistenti.
- Avvia l'app con PostgreSQL locale applicando le migrazioni 01→11 su un database con i dati di seed e verifica che la 11 non rompa i dati esistenti.

## CONSEGNA
1. Commit ordinati sul solo `feature/backend-operativita`, push.
2. Controlla che `git diff --name-only origin/main...HEAD` contenga solo file di tua proprietà.
3. Messaggio finale a Marco: riepilogo, test eseguiti con esito, eventuali deviazioni dal contratto (dovrebbero essere zero), ordine di merge consigliato (il tuo branch va fuso per primo).

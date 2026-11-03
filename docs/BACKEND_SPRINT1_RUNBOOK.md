# FORNITORI — Runbook backend e database
**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend Lead<br>
**Indice:** stato · PostgreSQL · Java · login · transazioni · demo · dipendenze.
Branch locale: `feature/backend`.

## 0. Stato del repository

Il repository esistente contiene il backend Spring Sprint 1 in
`src/main/java/it/exprivia/fornitori/{config,controller,dto,entity,enums,repository,service,common}`. I file SQL, OpenAPI
e le evidenze sono nel repository locale. Prima di ulteriori modifiche:

```powershell
git status --short --branch
Get-ChildItem -Force
```

La working tree iniziale aveva la cartella `target/` non tracciata; ora è esclusa da
`.gitignore`. Non sovrascrivere modifiche locali altrui.

## 1. Preparazione PostgreSQL locale
Prerequisiti: PostgreSQL e `psql`; database demo isolato. Impostare le credenziali
solo nell'ambiente locale; non sono versionate.
PostgreSQL 18 locale è attualmente in esecuzione e `localhost:5432` è raggiungibile.
L'autenticazione dell'utente applicativo non è stata verificata: il tentativo senza
password è stato rifiutato dal server; non aggirare l'autenticazione né modificare
la configurazione del servizio.

```powershell
createdb -U postgres fornitori
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/03_verifiche.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
```

`createdb` e `psql` possono non essere nel PATH di Windows. In alternativa usare pgAdmin/Query Tool eseguendo i quattro script in ordine. Il file `04_assert_baseline.sql` deve fare FAIL in caso di discrepanze. La prova di esecuzione deve essere registrata con data, commit e output.

### Conteggi e import attesi
10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti, 6 officine, 30 missioni storiche. Dashboard storica: 16 OK, 6 pending, 4 scadute, 4 non accettate; totale OK = 1509,71 €. Non sono esiti già verificati sul PostgreSQL del team.

**Importantissimo**: il dataset presenta missioni storiche aperte al 28/10/2026 e autista 3 associato a due missioni attive. Le missioni importate sono marcate `is_fixture=TRUE`; i controlli di disponibilità e lo scheduler live devono agire su record `is_fixture=FALSE`, preservando la baseline storico-didattica.

## 2. Avvio Java

L'applicazione legge `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e
`SPRING_DATASOURCE_PASSWORD`; Hibernate usa `ddl-auto=validate`. Java 25.0.5 LTS
è stato installato localmente e la build `clean verify` è passata su tale target.
Non abbassare il target.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

### Login e CSRF
`POST /api/v1/auth/login` accetta JSON ed è implementato; il token CSRF si recupera
da `GET /api/v1/auth/csrf`. Sessione server-side, cookie HttpOnly, ruoli e CSRF sono
attivi. Sei test MVC sono passati con JDK 25; il test context-load è saltato senza
credenziali PostgreSQL.

## 3. Endpoints minimi dello Sprint 1
- `POST /api/v1/auth/login` — 200, cookie sessione; invalidi 401.
- `POST /api/v1/auth/logout` — 204, invalidazione sessione; richiede CSRF.
- `GET /api/v1/auth/me` — email e ruolo sessione.
- `GET /api/v1/autisti` — elenco con disponibilità calcolata dalle missioni LIVE.
- `GET /api/v1/mezzi`, `GET /api/v1/compagnie`, `GET /api/v1/clienti`, `GET /api/v1/officine` — dati importati.
- `POST /api/v1/clienti` — creazione cliente per la centrale.
- `GET /api/v1/auth/csrf` — scambio token CSRF per richieste mutative.

Fonte del contratto: `api/openapi.yaml`. M2 deve poter collegare un login e una consultazione senza copiare array hardcoded.

## 4. Design delle transazioni da implementare in Sprint 2
- Proposta: transazione unica; lock autista, controllo su missioni live attive, lock missione; crea `mission_proposals`, aggiorna `missions` con deadline `now + 10 minutes`.
- Accept: lock dei record pertinenti; rifiutare se `now >= deadline` anche se lo scheduler non è ancora partito.
- Reject/Expire: aggiornare proposta e missione atomici; consentire nuova riproposta con record storico distinto.
- Complete: solo autista assegnato e stato `accettata`; validare km; costo `BigDecimal` e aggiornamento singolo; evento outbox; nessun doppio conteggio.
- Rettifica: solo responsabile; modifica `costo_finale` con motivo e audit, lascia `costo_calcolato` immutato.

Usare un `Clock` iniettato nel codice Java per testare agevolmente il limite dei 10 minuti e la fascia notturna senza aspettare tempo reale.

## 5. Checklist Definition of Done S1
- [x] Repository letto e branch locale `feature/backend` creato
- [ ] SQL, seed e baseline eseguiti su PostgreSQL locale (servizio attivo; credenziali applicative non disponibili)
- [x] Login JSON, errore credenziali, sessione, CSRF e permessi coperti dai test MVC con mock
- [ ] Anagrafiche e baseline verificate su PostgreSQL
- [x] Seed con hash BCrypt; nessuna password in chiaro aggiunta al repository
- [ ] Contratto OpenAPI confermato dagli altri membri
- [ ] PR e integrazione di gruppo (non eseguite)

Le sei prove web passano con Java 25; il test context-load è saltato localmente senza
password DB. La pipeline CI effimera con PostgreSQL ha superato schema, seed,
assertion e verifica applicativa. Questa checklist non dichiara superato il test DB
locale.

## 6. Primo demo script (5–7 minuti)
1. Avvio backend e datasource locale.
2. Mostra dati importati via `psql` o pgAdmin e `04_assert_baseline.sql`.
3. Effettua login responsabile nel browser.
4. Apri elenco clienti e autisti, mostra filtri reali.
5. Effettua login autista in finestra separata.
6. Verifica che l'autista non possa usare un endpoint riservato alla centrale.
7. Presenta le API di missione che saranno implementate nel secondo Sprint senza dichiararle già funzionanti.

## 7. Dipendenze da chiedere agli altri membri
M2 conferma i DTO; M3 richiede lookup targa e endpoint missioni; M4 conferma campi coordinate e km; M5 conferma evento `MISSION_ACCEPTED` e `MISSION_COMPLETED`. Qualunque rottura API richiede aggiornamento congiunto del file OpenAPI.

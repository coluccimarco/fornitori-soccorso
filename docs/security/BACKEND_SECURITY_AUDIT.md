# Audit di sicurezza del backend

**Data:** 04/11/2026 · **Ambito:** backend FORNITORI (base `62e0943`) · **Correzioni:** commit `a5be483`

Verifica applicativa limitata ai casi descritti: non è un penetration test né una certificazione.

## Esito

Nessuna vulnerabilità CRITICAL trovata. Trovate 25 voci: 3 HIGH, 12 MEDIUM, 10 LOW. Ne sono state corrette 16; le altre sono
aperte o accettate per la demo, con il motivo nel [registro](./VULNERABILITY_REGISTER.md).

**Valutazione:** adeguato a una demo con dati sintetici. **Non adeguato a dati reali** finché resta aperta V-01, il riconoscimento
del cliente con la sola targa.

## Metodo

1. Lettura di codice, schema SQL, OpenAPI, configurazione e chatbot.
2. Scrittura di 134 test prima di ogni correzione, per riprodurre i difetti: 21 esiti negativi, di cui 19 dovuti al prodotto e 2 a
   errori dei test stessi.
3. Correzioni, poi ripetizione di tutte le prove. Dettaglio in [SECURITY_TEST_REPORT.md](./SECURITY_TEST_REPORT.md).

Tutte le prove sono su database temporanei `fornitori_e2e_*`. Il database `fornitori` non è stato modificato: da lì viene solo la lettura del proprietario delle tabelle (V-13).

## Autenticazione e sessione

| Controllo | Esito |
|---|---|
| BCrypt, password mai nelle risposte | OK |
| Stessa risposta per password errata e utente inesistente | OK, provato |
| Cambio session ID e rotazione CSRF al login | OK, provato |
| Scadenza sessione 30 minuti, invalidazione al logout | OK; ora il logout rimuove anche il cookie |
| Cookie HttpOnly e SameSite=Lax | OK, provato su HTTP reale |
| Cookie Secure | Solo con profilo `prod`; HTTPS reale **NON VERIFICATO** |
| Limite per indirizzo | 20 login al minuto, provato |
| Limite per account | **Mancava** (V-06), corretto: 10 falliti al minuto |
| Lunghezza delle credenziali | **Mancava** (V-18), corretto: password massimo 128 |

## Autorizzazione

| Controllo | Esito |
|---|---|
| Endpoint della centrale negati ad anonimi e autisti | OK, 15 endpoint provati |
| Endpoint dell'autista negati ad anonimi e centrale | OK, 6 endpoint provati |
| Missione di un altro autista: lettura, note, percorso, accetta, rifiuta, chiudi | OK, 8 operazioni negate |
| Ex destinatario dopo il rifiuto | OK, accesso negato |
| Storico fixture all'autista | OK, negato |
| Token del chatbot sulle API operatore, sessione operatore sulle API chat | OK, negati |
| Note dell'autista prima dell'accettazione | **Permesse** (V-12), corretto |

I permessi stanno sia nella catena di sicurezza sia nei service.

## Dati personali

| Dato | Regola | Esito |
|---|---|---|
| Nome, telefono, targa del cliente | Autista assegnato: nome e telefoni dalla proposta, targa dopo l'accettazione, nessun contatto nello storico (regola cambiata con la migrazione 11) | OK, provato sul testo delle risposte |
| Numero di polizza | Mai all'autista | OK, provato |
| Storico autista | Solo ID cliente | OK, provato |
| Telefono in outbox | Non conservato per le missioni della centrale | OK, provato |
| Nome e compagnia via chatbot | Visibili a chi conosce la targa | **Aperto** (V-01) |
| Indirizzi verso Nominatim | Trasmessi a un servizio esterno | Limite noto, da rivalutare per dati reali |

## Chatbot

| Controllo | Esito |
|---|---|
| Token bearer obbligatorio, confronto a tempo costante | OK, provato |
| Limite targhe ignote | **Aggirabile** (V-03), corretto |
| Limite sulle targhe valide | **Mancava** (V-03), corretto: 10 ricerche al minuto per sessione |
| Una autorizzazione per una sola pratica | **Non applicato** (V-02), corretto |
| Scadenza dell'autorizzazione, 15 minuti | OK, provato |
| Idempotenza con `requestId`, difformità 409 | OK, provato |
| Stato solo per la propria chat, autista solo dopo l'accettazione | OK, provato |

### Soluzione proposta per l'identità del cliente (V-01)

Il PDF del docente chiede solo di verificare che la targa corrisponda a un cliente assicurato, quindi la demo resta conforme e il
dialogo non è stato cambiato. Per dati reali:

1. **Non mostrare più nome e compagnia** dopo la targa: rispondere solo "Veicolo trovato".
2. **Aggiungere un secondo dato**, per esempio le ultime 4 cifre della polizza. Il backend espone
   `POST /api/v1/chat/verifica`, confronta il dato senza rivelarlo, concede 3 tentativi per sessione e solo allora crea l'autorizzazione.
3. **Oppure un codice via SMS** al telefono in anagrafica: più forte, richiede un provider e costi.

Modifica al dialogo: `START → WAIT_VERIFICATION → WAIT_LOCATION → …`. Introdurre tutto dietro una proprietà, spenta per la demo, così
i dialoghi attuali continuano a funzionare e il nuovo percorso si prova a parte. Da concordare col docente.

## Database

| Controllo | Esito |
|---|---|
| FK, UNIQUE, CHECK su stati e coordinate | OK, provati con SQL diretto |
| Stessa proposta o autista occupato con richieste concorrenti | OK, provato con transazioni reali |
| Doppia accettazione concorrente | OK: una sola transizione e una sola notifica |
| SQL injection | OK: query parametrizzate, input provato |
| Hibernate `validate` | OK |
| Ruolo a privilegi minimi | **Funziona**, provato: 154 controlli con soli DML, DDL negato |
| Ruolo reale `fornitori_app` | **È proprietario delle tabelle** (V-13): intervento manuale |
| Vincoli e indici mancanti | Aggiunti in `09_integrity_and_indexes.sql` |

## Notifiche e provider

Claim atomico con `FOR UPDATE SKIP LOCKED`, riserva con scadenza, 5 tentativi: provati, anche con 4 dispatcher concorrenti. **Mancava**
il legame tra esito e riserva (V-04): corretto con il campo `tentativo`.

Provider: timeout 3 e 8 secondi, redirect rifiutati, risposta massimo 1 MiB, URL controllati all'avvio, valori controllati, una richiesta
al secondo verso Nominatim, nessuna chiamata esterna dentro una transazione. Provati con un server locale, salvo il timeout.

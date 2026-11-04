# Disegno del database

**Codice:** FOR-DB-001 · **Versione:** 2.0 · **Data:** 04/11/2026
Fonte schema: gli script in `database/`. Diagramma: [`database_er.mmd`](../diagrams/database_er.mmd).

## Modello

Gli account sono in `app_users`; un `drivers` collega un account a un mezzo. `customers` fa
riferimento a una compagnia assicurativa. Una `missions` appartiene a un cliente e può riferirsi ad
autista e officina. `mission_proposals` conserva i tentativi; note ed eventi sono separati per
l'audit; il pricing è una configurazione singola; `notifications_outbox` conserva invio ed esito.

| Tabella | Scopo e relazioni principali |
|---|---|
| `app_users` | Utenti autenticabili, email univoca, hash BCrypt e ruolo |
| `drivers` | Autista legato a un solo utente e a un solo mezzo |
| `vehicles` | Targa univoca, tipologia, portata e coordinate della base |
| `insurance_companies` | Compagnie assicurative dei clienti |
| `customers` | Cliente, targa normalizzata univoca, compagnia e scadenza polizza |
| `workshops` | Officine e coordinate |
| `missions` | Cliente, autista e officina, stato, località, date, chilometri, costi e flag fixture |
| `mission_proposals` | Tentativi di proposta e scadenza, con storico |
| `mission_notes` | Note collegate a missione e utente |
| `mission_events` | Audit della missione con dettagli JSON |
| `pricing_config` | Parametri tariffari e durata della proposta |
| `notifications_outbox` | Eventi da inviare, stato di invio e tentativi |
| `chat_mission_requests` | Legame requestId, sessione, destinatario e missione; impronta anti-duplicati |
| `chat_lookup_grants` | Autorizzazione sessione-cliente di 15 minuti dopo un lookup positivo |

## Integrità

- PK identity, FK e chiavi uniche per email, targa, `targa_norm`.
- Check su ruolo, stati, tipi di intervento, coordinate (entrambe nulle o entrambe valorizzate),
  costo, chilometri e note.
- `missions.is_fixture` distingue i 30 dati storici dalle operazioni live.
- Indici parziali limitano autista attivo e proposta corrente.
- `NUMERIC` per moneta e distanze, `TIMESTAMPTZ` per gli istanti, `JSONB` per eventi e outbox.

## Stato di invio delle notifiche

`notifications_outbox.stato` vale `PENDING`, `SENDING`, `SENT`, `FAILED` o `SKIPPED`.
`prossimo_tentativo_il` indica la fine della riserva o il momento del nuovo tentativo. Le missioni
della centrale generano righe `SKIPPED` senza telefono. Vedi
[guida notifiche](../team/MEMBRO_5_NOTIFICATIONS.md).

## Transazioni e concorrenza

Transizioni di missione e outbox sono nei service; i repository eseguono lock pessimistici e la
riserva delle notifiche usa `FOR UPDATE SKIP LOCKED`. I vincoli DB sono una protezione ulteriore e
non sostituiscono autorizzazione o validazione.

## Baseline del seed

10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti, 6 officine e 30 missioni, con 16 eseguite,
6 aperte, 4 scadute, 4 rifiutate e €1.509,71. Il file Excel del docente non è nel repository.

## Setup locale

Usare PostgreSQL 18 e un database vuoto, di proprietà dell'utente applicativo, con le credenziali
nell'ambiente. Applicare tutti gli script in ordine:

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/05_chat_requests.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/06_chat_recipient.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/07_backend_hardening.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/08_cleanup_and_delivery.sql
```

Hibernate usa `validate`: senza tutti gli script l'applicazione non parte. Su un database esistente
applicare solo gli script mancanti, dopo un backup. `08_cleanup_and_delivery.sql` rimuove
`chat_sessions` e `drivers.stato_seed`: dopo di esso non rieseguire 01 e 02. Se fallisce la
validazione di `missions_origin_pair` in 07, verificare i dati: non inventare coordinate. Non
modificare le fixture per far passare l'assertion.

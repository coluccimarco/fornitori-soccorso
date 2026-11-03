# Disegno del database

**Codice:** FOR-DB-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Fonte schema: [`database/01_schema.sql`](../../database/01_schema.sql); modifiche chatbot:
[`05_chat_requests.sql`](../../database/05_chat_requests.sql) e
[`06_chat_recipient.sql`](../../database/06_chat_recipient.sql).

## Modello

Gli account sono in `app_users`; un `drivers` collega un account a un mezzo.
`customers` fa riferimento a una compagnia assicurativa. Una `missions` appartiene
a un cliente e può riferirsi ad autista e officina. `mission_proposals` conserva
i tentativi; note ed eventi sono separati per auditabilità; pricing è una
configurazione singola; `notifications_outbox` conserva invio/esito.
`chat_sessions` persiste metadati di canale lato backend ma non rimuove il fatto
che il motore conversazionale Python dichiara stato in RAM.

Diagramma generato da sorgente SQL: [`database_er.mmd`](../diagrams/database_er.mmd).

| Tabella | Scopo e relazioni principali |
|---|---|
| `app_users` | Utenti autenticabili, email univoca, hash password BCrypt e ruolo |
| `drivers` | Autista legato a un solo utente e a un solo mezzo; `stato_seed` è solo il valore importato |
| `vehicles` | Targa univoca, tipologia, portata e coordinate della base operativa |
| `insurance_companies` | Compagnie assicurative referenziate dai clienti |
| `customers` | Cliente, targa normalizzata univoca, compagnia e scadenza polizza |
| `workshops` | Officine e coordinate |
| `missions` | Cliente/autista/officina FK, stato, località, date, chilometri, costi e flag fixture |
| `mission_proposals` | Tentativi di proposta e scadenza, con storico |
| `mission_notes` | Note collegate alla missione e all'utente |
| `mission_events` | Audit della missione con dettagli JSON |
| `pricing_config` | Parametri tariffari e durata della proposta |
| `chat_sessions` | Metadati di canale/chat; non rende persistente lo stato del motore Python in RAM |
| `notifications_outbox` | Eventi da inviare e stato dei tentativi di notifica |

## Integrità e comportamento

- PK identity, FK e chiavi uniche per email, targa, `targa_norm`.
- Check su ruolo, stati, tipi di intervento, coordinate, costo, chilometri e note.
- `missions.is_fixture` distingue i 30 dati storici dalle operazioni live; il
  numero 30 proviene da report preesistenti, non da ricalcolo Excel durante audit.
- Indici parziali limitano autista live attivo e proposta corrente.
- `NUMERIC` per moneta/distanze, `TIMESTAMPTZ` per gli istanti, `JSONB` per dettagli
  evento/outbox.
- Gli script di schema non sono una migrazione versionata; modifiche future vanno
  applicate con un processo di migrazione esplicito, non con Hibernate update.

## Transazioni e concorrenza

Le transizioni di missione e l'outbox sono implementate a livello service; repository
specifici eseguono lock pessimistici. Il vincolo DB è ulteriore protezione, non
sostituisce authorization o validazione applicativa. I test CI PostgreSQL verificano
scenari di concorrenza dichiarati nei report.

## Baseline docente e limiti

Il report storico documenta 10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti,
6 officine e 30 missioni, con 16 missioni eseguite e €1.509,71. Il file Excel non
è incluso nell'audit corrente; questi valori sono citazioni della documentazione
esistente, non una conferma indipendente. L'assert SQL della CI verifica il seed
presente nel repository.

## Setup locale

Usare PostgreSQL 18+ compatibile, `psql` e un database vuoto e isolato con
credenziali autorizzate configurate nell'ambiente. Schema e seed sono script,
non una migrazione versionata; lo schema applicativo usa Hibernate `validate`,
non `ddl-auto=update`.

```powershell
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/01_schema.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/02_seed.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/03_verifiche.sql
psql -v ON_ERROR_STOP=1 -U fornitori_app -d fornitori -f database/04_assert_baseline.sql
```

Non modificare manualmente fixture per far passare l'assertion. Il risultato del
setup è completo solo quando tutti gli script e le verifiche terminano con
successo; consultare [TEST_EVIDENCE.md](../quality/TEST_EVIDENCE.md). Il job CI
usa PostgreSQL effimero e non prova l'accesso al database locale.

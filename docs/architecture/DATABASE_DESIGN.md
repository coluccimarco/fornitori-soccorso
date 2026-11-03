# Disegno del database

**Codice:** FOR-DB-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Fonte schema: [`database/01_schema.sql`](../../database/01_schema.sql); modifiche chatbot: `05_chat_requests.sql`, `06_chat_recipient.sql`.

## Modello

Gli account sono in `app_users`; un `drivers` collega un account a un mezzo.
`customers` fa riferimento a una compagnia assicurativa. Una `missions` appartiene
a un cliente e può riferirsi ad autista e officina. `mission_proposals` conserva
i tentativi; note ed eventi sono separati per auditabilità; pricing è una
configurazione singola; `notifications_outbox` conserva invio/esito.
`chat_sessions` persiste metadati di canale lato backend ma non rimuove il fatto
che il motore conversazionale Python dichiara stato in RAM.

Diagramma generato da sorgente SQL: [`database_er.mmd`](../diagrams/database_er.mmd).

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

# Casi di test FORNITORI

**Versione:** 1.1 · **Data:** 03/11/2026

Gli ID sono locali alla matrice, non ticket originali. Distinguere test implementati,
eseguiti localmente, coperti dalla CI e test manuali non eseguiti.

| ID | Scenario | Atteso | Stato/evidenza |
|---|---|---|---|
| AUTH-01 | API protetta senza sessione | 401 JSON | Codice e CI PASS |
| AUTH-02 | AUTISTA accede a dati riservati alla centrale | 403 | Codice e CI PASS |
| AUTH-03 | Login, sessione, identità, logout e CSRF | Sessione e permessi coerenti | Codice e CI PASS |
| DB-01 | Schema, seed e assertion baseline PostgreSQL | Script idempotenti secondo contratto, assertion coerenti | CI PostgreSQL run `41656606659` PASS |
| MIS-01 | Accept/reject e deadline proposta | Transizioni autorizzate e scadenza corretta | Test d'integrazione PostgreSQL CI PASS |
| MIS-02 | Proposte concorrenti/assegnazioni incompatibili | Nessun doppio assegnamento operativo | Test d'integrazione PostgreSQL CI PASS |
| MIS-03 | Completamento, costi e note | Km effettivi validati, audit conservato | Unit/integration CI PASS |
| MAP-01 | Route traino/riparazione e ownership driver | Tappe e permessi coerenti, km stimati separati | Test PostgreSQL inclusi in CI PASS; local DB BLOCKED |
| MAP-02 | Client Nominatim/OSRM con risposta/errori | Parse, timeout/error path deterministici | Test automatici inclusi in CI; esecuzione locale da registrare |
| UI-01 | Login e navigazione responsabile/autista da browser | UI e API same-origin funzionanti per ruolo | UI presente; browser E2E NOT RUN |
| UI-02 | Registri, creazione missione e proposta da browser | Form, errori e aggiornamento vista corretti | Browser E2E NOT RUN |
| UI-03 | Area autista, note e completamento da browser | Azioni, km e storico coerenti | Browser E2E NOT RUN |
| UI-04 | Rendering mappa nel client | Mostrare geometria/km stimati e fallback | Non integrato; DA FARE solo se requisito approvato |
| CHAT-01 | Dialogo chatbot, backend HTTP e notifiche | Stati e messaggi attesi senza accesso diretto DB | Suite Python locale 64 test PASS; CI non esegue Python |
| CHAT-02 | Verifica titolare del veicolo | Autorizzazione indipendente dalla sola targa | Limite noto; redesign/acceptance DA CONFERMARE |
| NOT-01 | Invio reale Telegram e retry provider | Ricezione e gestione errori remote | Documentato dal modulo, non ripetuto in questa verifica |
| NOT-02 | WhatsApp/webhook firmato | Provider integrato, autenticato e idempotente | NOT RUN; non implementato |

Non marcare un caso PASS per la sola presenza del codice/OpenAPI. Per l'esito
effettivo consultare [TEST_EVIDENCE.md](./TEST_EVIDENCE.md).

# Business Requirements — FORNITORI

**Codice:** FOR-BUR-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Responsabile:** team project work · **Approvazione:** DA CONFERMARE con docente
**Stato:** baseline ricostruita da repository; non è una trascrizione del PDF del corso

## Executive summary

FORNITORI supporta la gestione dimostrativa di richieste di soccorso: il backend
modella soggetti, missioni e assegnazioni; un bot guidato su Telegram crea richieste
e recapita alcuni aggiornamenti. Il repository corrente non include una UI web né
routing cartografico o WhatsApp. Le fonti primarie del corso (PDF ed Excel) non sono
disponibili in questa revisione: i requisiti sotto sono una baseline verificabile
con il repository e richiedono convalida, non requisiti obbligatori attribuiti al docente.

## Contesto e problema

Il project work mira a coordinare centrale operativa, autisti e richiesta cliente
tramite componenti separati e API. La soluzione deve preservare lo storico fornito,
proteggere dati e responsabilità, e rendere osservabili gli esiti delle missioni.
Gli importi e i conteggi del workbook citati nei report storici non sono stati
ricalcolati sul file Excel originale, assente dalla working tree.

## Stakeholder e attori

| Attore | Esigenza / responsabilità | Stato d'interazione nella baseline |
|---|---|---|
| Docente/referente | Definisce specifica, dati e acceptance | Fonte richiesta; documenti non disponibili |
| Responsabile centrale | Gestisce anagrafiche, missioni, assegnazioni e costi | API backend; nessuna UI browser |
| Autista | Riceve proposte, risponde, completa missione | API backend; nessuna UI browser |
| Cliente | Avvia richiesta e consulta stato | Bot Telegram presente; controllo identità da validare |
| Team M1–M5 | Implementa backend, UI, bot, mappe e canali | Owner indicati nei documenti team; nomi da confermare |
| Provider Telegram | Trasporto messaggi | Client Telegram implementato; prova reale dichiarata nel README del modulo |
| Provider WhatsApp/mappe | Canale e dati geospaziali | Non integrato |

## Ambito

**Incluso nella baseline documentata:** autenticazione a sessione e CSRF; ruoli
RESPONSABILE/AUTISTA; anagrafiche; ciclo backend missione/proposta/timeout/
accettazione/rifiuto/completamento; costi e rettifica; note, dashboard e storia
autista; API servizio per chatbot, conversazione Python e canale Telegram;
outbox e dispatcher Telegram per notifiche supportate; PostgreSQL e CI.

**Non incluso o non dimostrato:** interfaccia web; mappa/routing/geocoding;
WhatsApp; verifica indipendente del titolare del veicolo; produzione/monitoraggio/
backup; funzionalità AI/Gemini. La loro obbligatorietà è **DA CONFERMARE**.

## Requisiti business

| ID | Requisito baseline | Priorità fonte | Stato nel repository | Acceptance sintetica |
|---|---|---|---|---|
| BR-001 | Gestire clienti, autisti, veicoli, assicurazioni e officine relazionati | Da convalidare | IMPLEMENTATO | API e vincoli DB restituiscono entità senza credenziali |
| BR-002 | Autenticare gli operatori e separare i privilegi centrali/autista | Da convalidare | IMPLEMENTATO | Sessione, CSRF e casi anonimo/ruolo errato testati |
| BR-003 | Creare e consultare una missione live separata dallo storico fixture | Da convalidare | IMPLEMENTATO | Nuova missione live persiste; le fixture non alterano viste live |
| BR-004 | Proporre la missione a un autista libero e mantenere esito di ogni proposta | Da convalidare | IMPLEMENTATO | Proposta, rifiuto/scadenza e riproposizione persistono senza perdere storico |
| BR-005 | Applicare deadline della proposta e impedire accettazioni scadute o doppie | Seed: 10 min; policy da convalidare | IMPLEMENTATO | Timeout e vincoli concurrency coperti da test backend/CI PostgreSQL |
| BR-006 | Chiudere missione con km effettivi e costo calcolato server-side | Regola da convalidare | IMPLEMENTATO | Completamento valida input e memorizza importi e istanti |
| BR-007 | Consentire rettifica centrale motivata e conservare audit | Da convalidare | IMPLEMENTATO | Solo centrale autorizzata; motivo e costo iniziale/finale persistono |
| BR-008 | Consultare aggregati centrale e guadagni limitati all'autista autenticato | Da convalidare | IMPLEMENTATO | API dashboard e viste autista rispettano i confini di ruolo |
| BR-009 | Creare richiesta guidata cliente tramite chatbot senza accesso diretto al DB | Da convalidare | IMPLEMENTATO | Bot usa backend fake o API chat; test dialoghi separati |
| BR-010 | Inviare in Telegram notifica di presa in carico/completamento per richieste supportate | Da convalidare | PARZIALE | Outbox/backend e dispatcher sono presenti; provider reale non è certificato dalla CI |
| BR-011 | Mostrare mappe/itinerari e chilometraggio stimato | Da confermare | API IMPLEMENTATA; mappa browser non integrata | Test CI; visualizzazione UI e provider live NOT RUN |
| BR-012 | Usare WhatsApp come canale cliente | Da confermare | PIANIFICATO | Endpoint provider non implementato; test non disponibili |
| BR-013 | Fornire UI web centrale/autista | Da confermare | UI PRESENTE NEL CODICE; verifica browser NOT RUN | Verificare flussi, permessi, UX e acceptance in ambiente isolato |

## Requisiti non funzionali e vincoli

| ID | Requisito/qualità | Verifica prevista | Stato |
|---|---|---|---|
| NFR-001 | Non esporre password o hash nei payload API | Test sicurezza e ispezione DTO | Implementato; CI backend verde |
| NFR-002 | Mutazioni session-based protette da CSRF; autorizzazioni server-side | `ApiSecurityTests`, `ChatApiSecurityTests` | Implementato; CI |
| NFR-003 | Persistenza PostgreSQL e tipi numerici/date appropriati | Schema e test PostgreSQL | Implementato; CI con PostgreSQL 18 |
| NFR-004 | Operazioni concorrenti non assegnano il medesimo autista a missioni live incompatibili | Test d'integrazione PostgreSQL | Implementato e provato nel job CI |
| NFR-005 | Bot non bypassa servizi applicativi per interrogare DB | Test e dipendenza `HttpBackend`/contratto API | Implementato per il bot in repo |
| NFR-006 | Segreti non versionati e errori integrazione espliciti | Review e test/secret handling | Segreti configurati da ambiente; production policy da confermare |

## Assunzioni, dipendenze e rischi

Stack effettivo: Java 25, Spring Boot 4.1.2, Maven Wrapper, Spring Data JPA,
Spring Security, PostgreSQL 18, Python standard library e API REST/OpenAPI.
La CI verde non prova la UI o un provider esterno dal vivo.
Dipendenze: specifiche e dataset del docente; credenziali autorizzate per locale;
eventuali provider di mappe/WhatsApp; completamento delle guide consumer.
Rischi: requisiti non disponibili; targa non prova titolarità; conversazioni del bot
sono in RAM; UI non collaudata e mappa visuale non integrata; uso produzione non validato. Vedi
[registro rischi](../management/RISK_REGISTER.md).

## Criteri di successo e acceptance

Ogni BR accettato deve collegarsi a user story, componente, API e caso di test in
[matrice di tracciabilità](./REQUIREMENTS_TRACEABILITY.md). Un PASS richiede
un'esecuzione identificata; gli elementi PIANIFICATI non passano acceptance.
La baseline tecnica corrente passa i job CI latest `41656606659`; l'accettazione
del project work resta DA CONFERMARE con le fonti primarie e il docente.

## Glossario

**Centrale:** operatore responsabile. **Fixture:** dato storico importato, marcato
`is_fixture`. **Live:** missione operativa creata dall'applicazione.
**Outbox:** coda persistente transazionale di notifiche. **Targa normalizzata:**
chiave di ricerca uniforme, che non è di per sé verifica d'identità.
**Verificato:** accompagnato da fonte di prova, commit e risultato.

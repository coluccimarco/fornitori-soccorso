# Business Requirements — FORNITORI

**Codice:** FOR-BUR-001 · **Versione:** 2.1 · **Data:** 05/11/2026
**Responsabile:** team project work · **Approvazione:** DA CONFERMARE con docente
**Stato:** baseline ricostruita dal repository; non è una trascrizione del PDF del corso

## Executive summary

FORNITORI supporta la gestione dimostrativa di richieste di soccorso: il backend modella
soggetti, missioni e assegnazioni; un client web serve centrale e autisti, con mappa; un bot
guidato su Telegram crea richieste e recapita gli aggiornamenti. Le fonti primarie del corso
(PDF ed Excel) non sono nel repository: i requisiti sotto sono una baseline da convalidare.

## Contesto e problema

Il project work coordina centrale operativa, autisti e richiesta del cliente tramite
componenti separati e API. La soluzione preserva lo storico fornito, protegge dati e
responsabilità e rende osservabili gli esiti delle missioni.

## Stakeholder e attori

| Attore | Esigenza / responsabilità | Interazione |
|---|---|---|
| Docente/referente | Definisce specifica, dati e acceptance | Fonte da acquisire |
| Responsabile centrale | Gestisce anagrafiche, missioni, assegnazioni e costi | Client web |
| Autista | Riceve proposte, risponde, completa la missione | Client web, anche da smartphone |
| Cliente | Avvia la richiesta e ne consulta lo stato | Bot Telegram |
| Team | Backend, frontend, chatbot, mappe e notifiche | Vedi [guide del team](../team/README.md) |
| Provider Telegram | Trasporto dei messaggi | Long polling |
| Provider mappe | Geocodifica e percorsi | Nominatim e OSRM pubblici |

## Ambito

**Incluso:** autenticazione a sessione e CSRF; ruoli RESPONSABILE e AUTISTA; anagrafiche;
ciclo missione, proposta automatica e manuale, timeout, accettazione, rifiuto, gestione manuale,
annullamento e completamento; verifica della polizza; costi e rettifica; note e supporto autista,
dashboard e storia autista; mappa e km stimati; conversazione Python e canale Telegram; outbox e
dispatcher; PostgreSQL.

**Fuori ambito:** WhatsApp; webhook in ingresso; gestione utenti da interfaccia; verifica
indipendente del titolare del veicolo (la sola targa è un limite noto della demo);
produzione, monitoraggio e backup; funzioni AI/Gemini.

## Requisiti business

| ID | Requisito baseline | Priorità fonte | Stato nel repository | Acceptance sintetica |
|---|---|---|---|---|
| BR-001 | Gestire clienti, autisti, veicoli, assicurazioni e officine relazionati | Da convalidare | IMPLEMENTATO | API e vincoli DB restituiscono entità senza credenziali |
| BR-002 | Autenticare gli operatori e separare i privilegi centrale/autista | Da convalidare | IMPLEMENTATO | Sessione, CSRF e ruoli applicati dal server |
| BR-003 | Creare e consultare una missione live separata dallo storico fixture | Da convalidare | IMPLEMENTATO | La missione persiste; le fixture non alterano le viste live |
| BR-004 | Proporre la missione a un autista libero e mantenere l'esito di ogni proposta | Da convalidare | IMPLEMENTATO | Proposte e riproposte persistono; storico consultabile dalla centrale |
| BR-005 | Applicare la deadline della proposta e impedire accettazioni scadute o doppie | Seed: 10 min; policy da convalidare | IMPLEMENTATO | Timeout e vincoli di concorrenza su DB |
| BR-006 | Chiudere la missione con km effettivi e costo calcolato dal server | Regola da convalidare | IMPLEMENTATO | La chiusura valida l'input e memorizza importi e istanti |
| BR-007 | Consentire una rettifica centrale motivata e conservare l'audit | Da convalidare | IMPLEMENTATO | Comando web; motivo e costo iniziale/finale persistono |
| BR-008 | Consultare aggregati centrale e guadagni limitati all'autista autenticato | Da convalidare | IMPLEMENTATO | Dashboard e viste autista rispettano i confini di ruolo |
| BR-009 | Creare una richiesta guidata del cliente via chatbot senza accesso diretto al DB | Da convalidare | IMPLEMENTATO | Bot su API chat; la sola targa è un limite noto |
| BR-010 | Inviare su Telegram la notifica di presa in carico, completamento e annullamento | Da convalidare | IMPLEMENTATO | Outbox con riserva persistente; solo per missioni create dal bot |
| BR-011 | Mostrare mappe, itinerari e chilometraggio stimato | Da confermare | IMPLEMENTATO | Mappa Leaflet in centrale e area autista, km proposti alla chiusura |
| BR-012 | Usare WhatsApp come canale cliente | Da confermare | FUORI AMBITO | Non implementato |
| BR-013 | Fornire una UI web per centrale e autista | Da confermare | IMPLEMENTATO | Pagina pubblica, centrale e area autista responsive |
| BR-014 | Far verificare alla centrale le richieste con polizza non verificabile | Da convalidare | IMPLEMENTATO | La pratica si conferma con un cliente con polizza attiva o si respinge a costo 0 |
| BR-015 | Proporre le missioni in automatico e passarle alla centrale dopo 3 esiti negativi | Da convalidare | IMPLEMENTATO | Proposta all'autista libero più vicino; al terzo esito negativo `gestione_manuale` |
| BR-016 | Annullare una missione con motivo e regole economiche chiare | Da convalidare | IMPLEMENTATO | Motivo obbligatorio; costo attribuito solo se annulla la centrale dopo l'accettazione |
| BR-017 | Permettere all'autista di chiedere supporto e indicare la disponibilità | Da convalidare | IMPLEMENTATO | Il supporto resta in coda critica finché non è gestito; chi non è disponibile non riceve proposte |

## Requisiti non funzionali e vincoli

| ID | Requisito/qualità | Verifica prevista | Stato |
|---|---|---|---|
| NFR-001 | Non esporre password o hash nei payload API | Ispezione DTO | Implementato |
| NFR-002 | Mutazioni di sessione protette da CSRF; autorizzazioni server-side | Prova con e senza token | Implementato |
| NFR-003 | Persistenza PostgreSQL e tipi numerici/date appropriati | Schema e Hibernate validate | Implementato |
| NFR-004 | Operazioni concorrenti non assegnano lo stesso autista a missioni live incompatibili | Lock e indice unico | Implementato |
| NFR-005 | Il bot non interroga il DB direttamente | Dipendenza `HttpBackend` | Implementato |
| NFR-006 | Segreti non versionati ed errori di integrazione espliciti | Revisione | Segreti da ambiente; policy di produzione fuori ambito |

## Assunzioni, dipendenze e rischi

Stack: Java 25, Spring Boot 4.1.2, Maven Wrapper, Spring Data JPA, Spring Security,
PostgreSQL 18, Python con sola libreria standard, API REST/OpenAPI. Dipendenze: specifiche e
dataset del docente. Rischi: requisiti non disponibili; la targa non prova la titolarità;
conversazioni in RAM. Vedi [registro rischi](../management/RISK_REGISTER.md).

## Criteri di successo

Ogni BR si collega a storia, componente e API nella
[matrice di tracciabilità](./REQUIREMENTS_TRACEABILITY.md). L'accettazione del project work
resta da confermare con il docente.

## Glossario

**Centrale:** operatore responsabile. **Fixture:** dato storico importato, marcato
`is_fixture`. **Live:** missione operativa creata dall'applicazione. **Outbox:** coda
persistente transazionale di notifiche. **Targa normalizzata:** chiave di ricerca uniforme,
che non è una verifica d'identità.

# Revisione dell'architettura del backend

**Data:** 04/11/2026 · **Ambito:** backend FORNITORI, albero di lavoro locale (base `62e0943`) · **Modifiche:** non committate

## Flusso

```text
Frontend -> filtro dimensione -> Spring Security -> Controller + DTO validati
         -> Service transazionale -> Repository -> PostgreSQL 18 -> DTO -> Frontend
Bot      -> token di servizio -> ChatController -> ChatService -> stessi service e repository
```

## Valutazione

| Aspetto | Esito |
|---|---|
| Separazione a livelli | Buona: i controller delegano, nessuna entity nelle risposte |
| Transazioni | Buone: nei service e brevi; nessuna chiamata esterna dentro una transazione, verificato su `MapService`, `NearestDriverService`, `ChatService` |
| Concorrenza | Buona: lock in ordine missione poi autista, indici parziali come seconda difesa, `SKIP LOCKED` sull'outbox |
| Coerenza API, DTO, schema | Buona: controller e DTO confrontati con OpenAPI da test; Hibernate `validate` su schema |
| Errori | Migliorata: gestore finale unico, nessun dettaglio interno nelle risposte |
| Osservabilità | Migliorata ma minima: accessi negati, login falliti, errori con riferimento; niente metriche |

## Miglioramenti fatti

- Gestione delle eccezioni centralizzata, con codici pubblici in un solo punto.
- Controllo della configurazione all'avvio.
- Limite di dimensione in un filtro unico, prima della sicurezza.
- `RequestRateLimiter` riusato per i limiti sugli esiti negativi.
- Rimosso `ChatLookupLimiter.reset`, che apriva V-03.
- Scheduler più robusto. Evento di scadenza con l'autista già nel dettaglio.

Nessun refactoring estetico. Nessuna dipendenza nuova a runtime; `spring-boot-starter-*-test` solo per i test.

## Debito tecnico

| Voce | Effetto | Proposta |
|---|---|---|
| `MissionService` oltre 690 righe | Più difficile da mantenere | Dividere in creazione, proposta, chiusura e letture, dopo la consegna |
| Stati come stringhe | Errori di battitura solo a runtime | Enum con convertitore JPA |
| Liste non paginate, query per riga | Lento con molti dati | Paginazione e fetch unico |
| Scadenze in un'unica transazione | Un errore annulla il giro | Una transazione per missione |
| Limiti in memoria | Validi per una sola istanza | Tabella PostgreSQL o Redis se servono più istanze |
| Conversazione del bot in RAM | Persa al riavvio | Accettato per la demo |

## Dipendenze

`pom.xml` contiene solo starter usati (JPA, Security, Validation, WebMVC), il driver PostgreSQL e i rispettivi starter di test. `spring-jdbc` arriva
dallo starter JPA ed è usato da `ChatLookupGrantRepository`.

# Architettura FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend Lead<br>
**Indice:** contesto · componenti · backend · confini · dipendenze

## Scopo e stato

Descrive l'architettura condivisa. La parte backend Sprint 1 è **IMPLEMENTATA**;
frontend, missioni, mappe, chatbot e notifiche sono **PIANIFICATI**. Diagramma
dettagliato: [`docs/TAD_FORNITORI.md`](./TAD_FORNITORI.md) e
[`diagrams/architecture.mmd`](./diagrams/architecture.mmd).

## Vista componenti

Browser centrale/autista → API REST Spring → service → repository JPA → PostgreSQL.
Spring Security protegge sessioni e ruoli. Chatbot, routing e provider di notifica
sono adattatori esterni previsti e non sono collegati nel codice corrente.

## Struttura backend implementata

```text
it.exprivia.fornitori
├── FornitoriApplication
├── config       # SecurityFilterChain, sessione e CSRF
├── controller   # endpoint REST, validazione e status HTTP
├── dto          # contratti di input/output; mai entity in response
├── entity       # mapping JPA del database
├── enums        # valori di dominio condivisi, es. ruoli
├── repository   # query e persistenza Spring Data
├── service      # casi d'uso e transazioni
└── common       # gestione centralizzata errori API
```

Il flusso di dipendenza è `controller → service → repository → entity`.
I controller non accedono alla persistenza; i DTO non contengono hash/password.
L'applicazione parte dal package radice perché `FornitoriApplication` deve
scansionare i sottopackage.

## Confini e dipendenze

- Backend: autorità per autenticazione, permessi, assegnazione e costi.
- Frontend: presentazione, input, stati caricamento e gestione errori; non replica
  regole di business.
- Chatbot/notifiche: parlano con API/eventi autorizzati, non accedono direttamente
  alle tabelle.
- Mappe: coordinate e distanze suggerite; il backend decide validità e tariffa.
- PostgreSQL: fonte persistente; `is_fixture=TRUE` marca lo storico didattico.

**Completamento:** ogni componente rispetta questo verso di dipendenza e aggiorna
diagramma/contratti quando i confini cambiano.<br>
**Riferimenti:** [database](./03_DATABASE.md), [API](./04_API_REST.md),
[TAD](./TAD_FORNITORI.md).

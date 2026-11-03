# Sprint 3 — Integrazioni

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Scrum Master<br>
**Indice:** goal · componenti · dipendenze · acceptance · demo · rischi

## Sprint Goal

Integrare chatbot, routing mappe e notifiche senza dipendere direttamente dalle
tabelle o replicare regole backend. Questa sezione è il piano originale, non lo
stato della baseline corrente; la riconciliazione è riportata in fondo.

## Attività pianificate

| Attività | Owner | Dipendenze |
|---|---|---|
| Menu chatbot, session state, verify customer e create request | M3 | API create mission e identity check M1 |
| Adapter callback Telegram/WhatsApp e retry/idempotenza | M5 | provider approval, webhook auth M1 |
| Leaflet map e route per traino/repair | M4 | mission coordinates, provider decision M1 |
| Eventi accepted/completed e outbox dispatcher | M1/M5 | state transition Sprint 2 |
| UI map/status e stati integrazione | M2/M4 | route/status API |
| Contract/integration tests e privacy review | Tutti | sandbox/fake adapters |

Stime da rifinire in planning; non sono ore consuntivate.

## Acceptance criteria

- Chat guided happy/error path, session isolation e verifica cliente prima di leggere
  stato; output non rivela missioni altrui.
- Routing distingue traino e riparazione, attribuisce provider e tratta la distanza
  come stima, non costo o km effettivi.
- Notifiche partono da outbox dopo commit, callback firmati, retry con backoff e
  chiave idempotente; nessun segreto/PII nei log.
- Provider offline non corrompe lo stato missione; fallback e errori osservabili.

## Demo e rischi

Demo in sandbox/fake provider, input targa verificata, route fixture e notifiche
duplicate senza doppio invio. DoD: contract tests condivisi e review security/privacy.
Rischi: API rate limits, disponibilità provider, geocoding e costi/termini d'uso.
Passaggio a Sprint 4 con integrazioni ripetibili in ambiente di test.

**Riferimenti:** guide [chatbot](../team/MEMBRO_3_CHATBOT.md),
[mappe](../team/MEMBRO_4_MAPS.md), [notifiche](../team/MEMBRO_5_NOTIFICATIONS.md).

### Nota di riconciliazione — 03/11/2026

La scheda resta un piano storico, non un verbale. La baseline `main` successiva contiene il bot Python/Telegram, le API chat e il
dispatcher Telegram; il README del modulo documenta una prova reale che non è
parte dei job CI. Il frontend centrale/autista è presente; API mappe, Nominatim e
OSRM sono implementati e inclusi nella CI. La mappa browser non è collegata,
provider live non sono stati verificati e WhatsApp resta non implementato. Vedi
[registro Scrum](./README.md) e [audit](../management/DOCUMENTATION_AUDIT.md).

# Disegno delle integrazioni

**Codice:** FOR-INT-001 · **Versione:** 1.0 · **Data:** 03/11/2026

| Integrazione | Direzione | Contratto/runtime | Stato e limite |
|---|---|---|---|
| Bot Python → backend | HTTPS/API chat; Bearer token | `HttpBackend`, `ChatController`, OpenAPI | Implementato |
| Bot console → fake backend | Locale, dati sintetici | `FakeBackend` | Implementato per sviluppo/test; non dati reali |
| Bot → Telegram | Long polling e API Telegram dal modulo bot | `telegram_channel.py`, `notifications.py` | Implementato; prova reale dichiarata nel README chatbot, non in CI |
| Backend → outbox | Scrittura transazionale | `notifications_outbox`, service mission/chat | Implementato per eventi supportati |
| Backend → WhatsApp | Futuro | Route/webhook progettati in OpenAPI | Non implementato |
| Client → route provider | Futuro | API mappe `designed` | Non implementato |
| Frontend → API operatori | Futuro | Sessione/cookie/CSRF | UI non presente |

Il dispatcher e la coda non equivalgono a garanzia exactly-once del provider.
Consultare limiti, retry, requestId e memoria bot in
[`chatbot/README.md`](../../chatbot/README.md). Non inviare token/PII nei diagrammi,
test o log.

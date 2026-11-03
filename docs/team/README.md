# Guide del team e contratti condivisi

| Membro | Ownership | Moduli presenti | Stato e guida |
|---|---|---|---|
| M1 — Backend/DB | Java/Spring, DB, API, missioni, pricing, auth | `src/main/java`, `database`, `api` | [Guida M1](./MEMBRO_1_BACKEND.md); implementato e CI verificata |
| M2 — Frontend | UI centrale e autista | Nessun package frontend | [Guida M2](./MEMBRO_2_FRONTEND.md); pianificato |
| M3 — Chatbot | Dialogo Python e canale Telegram | `chatbot/fornitori_chatbot`, `chatbot/tests` | [Guida M3](./MEMBRO_3_CHATBOT.md); implementato, limiti identità da valutare |
| M4 — Mappe | Geolocalizzazione e routing | Nessun modulo mappe | [Guida M4](./MEMBRO_4_MAPS.md); pianificato |
| M5 — Notifiche | Adapter provider, outbox e canali | Backend outbox + dispatcher bot Telegram; webhook/WhatsApp assenti | [Guida M5](./MEMBRO_5_NOTIFICATIONS.md) |

## Ownership e confini

- M1 mantiene schema, servizi, sicurezza server-side e il contratto OpenAPI;
  il consumer interessato rivede ogni breaking change.
- M2 non replica autorizzazione, tariffa o assegnazione nel client; la UI ancora
  non esiste nel repository.
- M3 usa `Backend`/`HttpBackend`, non connessioni PostgreSQL; non identificare il
  cliente solo dalla targa come prova sufficiente.
- M4 non trasforma coordinate mancanti in posizioni inventate; routing previsto,
  ma non runtime.
- M5 separa evento transazionale, dispatch e provider; il dispatcher Telegram è
  presente, WhatsApp no.

## Checklist condivisa prima della PR

- Scope e requisito `BR-*`/story collegati; owner e consumer identificati.
- API/schema coordinati e documento OpenAPI aggiornato se il contratto cambia.
- Test pertinenti passati; test non eseguiti esplicitamente marcati BLOCKED/NOT RUN.
- Nessun token, password, hash, PII o dato docente riservato in commit/log.
- `git diff --check`; build/test del modulo e CI controllati.
- Link e stato dei documenti correlati aggiornati; nessuna capability futura
  presentata come implementata.

Le guide individuali sono mantenute nelle pagine collegate; `INTEGRATION_CONTRACTS.md`
descrive le dipendenze tra componenti. Nomi degli studenti e assegnazione formale
sono da confermare.

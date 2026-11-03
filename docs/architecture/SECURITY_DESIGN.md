# Sicurezza e protezione dei dati

**Codice:** FOR-SEC-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Questa descrive controlli sorgente, non è un penetration test né certificazione.

## Controlli presenti

- Password operatori verificate con BCrypt; DTO non devono restituire hash/password.
- Chain API operatore stateful con sessione HTTP, cambio session ID all'autenticazione,
  CSRF su mutazioni e risposte JSON `401/403`.
- Ruoli RESPONSABILE/AUTISTA e controlli di proprietà missione nel service.
- Chain `/api/v1/chat/**` separata, stateless, token Bearer e nessun CSRF di sessione.
- Segreto passato da `FORNITORI_CHAT_TOKEN`; non mantenerlo nei file del repository.
- I bot usano service API; accesso diretto alle tabelle è vietato per architettura.
- Cookie HttpOnly, SameSite Lax; `SESSION_COOKIE_SECURE` configurabile.
- La chain chat usa autenticazione stateless con token di servizio; non usa CSRF
  di sessione. Il client statico non sostituisce le verifiche di ruolo o proprietà
  eseguite dal server.

## Limiti, privacy e minacce

La lookup cliente a partire dalla targa non verifica la titolarità; il bot può
mostrare nome/compagnia e creare richieste. Stato dialogo in RAM. Telegram
trasporta dati fuori dal backend; l'uso è adatto alla demo dichiarata, non implica
autorizzazione production. Webhook e provider WhatsApp non sono implementati.
Logging/retention, DPIA, consenso, gestione incidenti, secret rotation e policy di
produzione non risultano documentati/approvati. Prima di uso reale serve review
privacy e sicurezza specifica.

Per eventuali webhook: verificare firma sul payload grezzo, timestamp/anti-replay
e idempotency key; limitare dimensione e frequenza, minimizzare i log e conservare
i segreti solo in secret manager o ambiente protetto. Retry deve essere idempotente.
Questi controlli sono requisiti futuri, non funzionalità già implementate.

## Verifiche

`ApiSecurityTests`, `ChatApiSecurityTests`, test chatbot e CI Java/PostgreSQL
verificano soltanto i casi codificati. La CI non è un audit indipendente. Consultare
[test](../quality/TEST_PLAN.md) e [rischi](../management/RISK_REGISTER.md).

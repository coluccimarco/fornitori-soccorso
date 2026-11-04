# Sicurezza e protezione dei dati

**Codice:** FOR-SEC-001 · **Versione:** 2.0 · **Data:** 04/11/2026
Descrive i controlli presenti nel codice; non è un penetration test né una certificazione.

## Controlli presenti

- Password verificate con BCrypt; i DTO non restituiscono hash o password.
- Chain operatore con sessione HTTP, cambio session ID e rotazione CSRF al login, CSRF sulle
  mutazioni e sulla `GET` delle mappe, risposte JSON `401/403`.
- Ruoli RESPONSABILE e AUTISTA; proprietà della missione controllata nel service: una vecchia
  proposta non dà accesso dopo una riassegnazione.
- Chain `/api/v1/chat/**` separata e stateless, con token bearer confrontato a tempo costante.
- Cookie HttpOnly, SameSite Lax, sessione di 30 minuti; `Secure` nel profilo `prod`.
- Limiti: 20 login al minuto per IP; 5 targhe non trovate per sessione; 100 lookup al minuto per
  istanza. Il server non si fida di `X-Forwarded-For`.
- Client dei provider senza redirect, con timeout 3/8 s, risposta massima 1 MiB e valori controllati.
- Contatti del cliente visibili all'autista solo dopo l'accettazione.
- Le missioni della centrale non conservano il telefono del cliente in outbox.
- SQL parametrizzato ed escaping HTML dei dati dinamici nel client.
- CSP, Referrer-Policy, Permissions-Policy, corpo massimo 64 KiB, controllo della configurazione all'avvio.
- 10 login falliti al minuto per account, 10 ricerche di targa al minuto per sessione, autorizzazione del lookup monouso.

## Limiti noti della demo

- **La sola targa identifica il cliente:** il bot mostra nome e compagnia e può creare richieste a
  chi la conosce. Il lookup autorizza la pratica per 15 minuti ma non prova la titolarità. Non usare
  con dati reali.
- Stato del dialogo in RAM. Telegram trasporta dati fuori dal backend.
- Consegna delle notifiche "almeno una volta" in caso di crash tra invio ed esito.
- Logging, retention, DPIA, consenso, gestione incidenti, rotazione dei segreti e policy di
  produzione non sono definiti. Prima di un uso reale serve una revisione privacy e sicurezza.

WhatsApp e webhook in ingresso sono fuori ambito. Se venissero introdotti, dovrebbero verificare la
firma sul payload grezzo, timestamp e replay, limitare dimensione e frequenza.

Audit, vulnerabilità e prove in [`docs/security`](../security/BACKEND_SECURITY_AUDIT.md); rischi nel [registro](../management/RISK_REGISTER.md).

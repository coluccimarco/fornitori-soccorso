# Revisione difensiva sicurezza backend

**04/11/2026 — baseline 5a16da8, feature/backend — Minervini Vincenzo.**
Verifica applicativa limitata ai casi descritti, non penetration test completo
né certificazione. Riferimenti: [OWASP ASVS 5.0](https://asvs.owasp.org/),
[OWASP API Security 2023](https://owasp.org/projects/api-security-project),
[Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html),
[session management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
La verifica considera autenticazione, sessione, autorizzazione, input, API e dati;
non pretende copertura di tutti i requisiti ASVS.

| ID / gravità | Prima / componente | Mitigazione e prova | Stato |
|---|---|---|---|
| SEC-01 HIGH | Vecchio destinatario di proposta poteva leggere missione/note e route dopo riassegnazione | Ownership sul driver corrente; PostgresHttpWorkflowTests con due autisti, note e mappe | CORRETTO per casi verificati |
| SEC-02 HIGH | Creazione chat con clienteId senza lookup legato alla sessione | Grant DB 15 minuti, polizza attiva alla creazione; lookup assente/scaduto negato; ChatPostgresIntegrationTests | MITIGATO, non prova identità |
| SEC-03 HIGH | La targa nota consente riconoscimento nome/compagnia senza dimostrare titolarità | Limite globale 100 lookup/minuto + 5 errori/sessione; grant non sostituisce OTP/consenso | APERTO, blocca dati reali |
| SEC-04 MEDIUM | requestId riusato con destinatario/sessione/payload diversi | Fingerprint SHA256 canonico per richieste nuove, verifica binding; mismatch 409 | CORRETTO nuove richieste; legacy senza fingerprint parziale |
| SEC-05 MEDIUM | Login manuale non invalidava CSRF precedente | Rotazione repository CSRF e refresh client; login DB reale verifica vecchio token 403 e nuova session ID | CORRETTO |
| SEC-06 MEDIUM | Tentativi login non limitati, lookup aggirabile cambiando sessione | Limiter per IP remoto 20/minuto e globale chat 100/minuto, finestra Clock e cap10.000; RateLimitTests | MITIGATO singola istanza; proxy/NAT e replica da dimensionare |
| SEC-07 MEDIUM | GET mappe produceva scritture senza CSRF | Match esplicito GET e HEAD implicito route, header obbligatorio; MVC negativo e test proprietà | CORRETTO; GET con side effect resta debito contrattuale; HEAD riprodotto200 pre-fix e negato403 post-fix |
| SEC-08 MEDIUM | Lunghezze, precisione e costo calcolato oltre capacità DB | Bean Validation + guardia costo, 400 prima della chiusura; MVC e PostgresHttpWorkflowTests | CORRETTO |
| SEC-09 MEDIUM | Provider poteva restituire coordinate/distanze invalide, redirect e body non limitati | Destinazione da config, no redirect, 1 MiB, timeout3/8s, range; 11 test provider HTTP locale | MITIGATO; TLS/config produzione e disponibilità live NOT RUN |
| SEC-10 MEDIUM | Token/sessione persa lasciava UI con 403 generico e token vecchio | Probe /auth/me dopo 403 di mutazione, nessun replay; reset token/logout/login | Test browser e login HTTP; nessun retry scrittura automatico |
| SEC-11 MEDIUM | Record LoginRequest poteva apparire nei log DEBUG con password | toString redatto; osservati 11 record LoginRequest[redacted] nei test HTTP eseguiti; stacktrace esclusi dal JSON, DEBUG disabilitato nel runner | CORRETTO DTO login/CSRF con regression test diagnostica; policy log/retention operativa da definire |
| SEC-12 MEDIUM | Doppia consegna outbox con dispatcher concorrenti o crash prima ACK | event_key unica, ACK lock e limite tentativi già presenti; test outbox mantenuti | APERTO lease/retry persistenti; non exactly-once |
| SEC-13 LOW | Parametri non convertibili fuori dal formato ApiError | Handler 400/405/415 uniforme; ApiContractTests | CORRETTO |
| SEC-14 MEDIUM | Sicurezza cookie dipendente dall'ambiente | HttpOnly, Lax, timeout30m; prod impone Secure | Configurato, HTTPS reale NOT RUN |

Nessuna criticità CRITICAL dimostrata. Le gravità sono valutazioni tecniche per
questo sistema, non punteggi CVSS ufficiali. SEC-03 e SEC-12 non sono accettati
implicitamente: rimangono decisioni aperte per responsabile/team. L'accettazione
storica dello stato bot in RAM vale solo per demo, come da R-07; questa revisione
non approva produzione o uso di dati personali reali.

## Altri controlli e limiti

- BCrypt e autenticazione da PostgreSQL reale; ruoli default-deny, chain chat
  stateless a token separata, CSRF operatore attivo. Niente JWT aggiunto.
- Token servizio esatto, confronto constant-time; assente/errato negato dai test.
  Rotazione operativa: aggiornare backend e bot in ambiente coordinato, riavviare
  e verificare 401 col token precedente. Nessuna doppia chiave/grace period implementata.
- SQL parametrizzato; il repository non concatena input utente. Il frontend
  usa escaping HTML per i dati dinamici; quattro test JS includono payload XSS.
- Runtime E2E con ruolo non-superuser; migrazioni eseguite dall'admin del solo
  cluster temporaneo. Le autorizzazioni del database personale non sono certificate.
- Segreti fuori Git; checkpoint DPAPI esterno. Non pubblicare log completi,
  cookie, dump o datasource. Fixture/password CI sono dimostrative, non credenziali operative.
- OSV: 124 versioni Maven risolte, nessun advisory corrispondente alla scansione
  locale. Servizio esterno e copertura temporale limitati: non significa assenza
  universale di vulnerabilità; nuove corrispondenze bloccano il job dedicato.
- Nessun CORS aperto aggiunto; frontend same-origin. HTTPS/reverse proxy, CSP e
  integrità CDN Bootstrap, dimensione massima JSON in ingresso, retention, backup,
  secret manager, controlli anti-abuso distribuiti restano da implementare/verificare.
- Provider live Telegram, Nominatim/OSRM e WhatsApp non testati con dati reali.
  Il limite body riguarda i client mappe Java; non si estende per inferenza a Python.

## Regole di rilascio

Draft PR: le prove del flusso locale non autorizzano automaticamente dati reali.
Prima del rilascio risolvere SEC-03, concordare le garanzie di consegna SEC-12,
collaudare HTTPS/configurazione e raccogliere approvazioni docente/consumer.
Evidenze riproducibili e limiti in [BACKEND_E2E_AUDIT.md](BACKEND_E2E_AUDIT.md)
e [TEST_EVIDENCE.md](TEST_EVIDENCE.md).

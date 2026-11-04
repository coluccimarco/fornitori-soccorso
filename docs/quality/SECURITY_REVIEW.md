# Revisione difensiva della sicurezza

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)
Revisione limitata ai casi descritti, non penetration test né certificazione. Riferimenti:
[OWASP ASVS 5.0](https://asvs.owasp.org/), [OWASP API Security 2023](https://owasp.org/projects/api-security-project).

| ID / gravità | Rischio | Mitigazione | Stato |
|---|---|---|---|
| SEC-01 HIGH | Un vecchio destinatario di proposta poteva leggere missione, note e percorso dopo una riassegnazione | Accesso limitato all'autista corrente | Corretto |
| SEC-02 HIGH | Creazione chat con `clienteId` senza lookup legato alla sessione | Autorizzazione DB di 15 minuti, polizza controllata alla creazione | Mitigato; non prova l'identità |
| SEC-03 HIGH | La targa nota consente di vedere nome e compagnia senza dimostrare la titolarità | Limiti di 100 lookup al minuto e 5 errori per sessione | Limite noto della demo; blocca l'uso con dati reali |
| SEC-04 MEDIUM | `requestId` riusato con destinatario, sessione o dati diversi | Impronta SHA-256 canonica; difformità 409 | Corretto |
| SEC-05 MEDIUM | Il login non invalidava il CSRF precedente | Rotazione del token e aggiornamento del client | Corretto |
| SEC-06 MEDIUM | Tentativi di login non limitati | 20 al minuto per IP, memoria limitata | Mitigato su singola istanza |
| SEC-07 MEDIUM | La GET delle mappe produceva scritture senza CSRF | Header obbligatorio anche per GET e HEAD | Corretto |
| SEC-08 MEDIUM | Lunghezze, precisione e costo calcolato oltre la capacità del DB | Validazione e controllo del costo, 400 prima della chiusura | Corretto |
| SEC-09 MEDIUM | Il provider poteva restituire valori invalidi, redirect e risposte non limitate | Nessun redirect, 1 MiB, timeout 3/8 s, intervalli controllati | Mitigato |
| SEC-10 MEDIUM | Sessione persa con UI su 403 generico | Controllo di `/auth/me` dopo un 403, senza ripetere le scritture | Corretto |
| SEC-11 MEDIUM | La password del login poteva apparire nei log DEBUG | `toString` redatto per login e CSRF | Corretto |
| SEC-12 MEDIUM | Doppia consegna delle notifiche con più dispatcher | Riserva persistente con `FOR UPDATE SKIP LOCKED` e scadenza | Corretto per più dispatcher; resta il caso di crash tra invio ed esito |
| SEC-13 LOW | Parametri non convertibili fuori dal formato d'errore | Risposte 400, 405 e 415 uniformi | Corretto |
| SEC-14 MEDIUM | Sicurezza del cookie dipendente dall'ambiente | HttpOnly, Lax, 30 minuti; `prod` impone Secure | Configurato; HTTPS reale non provato |
| SEC-15 LOW | Telefono del cliente conservato in outbox per le missioni della centrale | Righe `SKIPPED` senza telefono, anche per i dati esistenti | Corretto |

## Altri controlli e limiti

- BCrypt e autenticazione da PostgreSQL; ruoli default-deny; chain chat stateless a token. Nessun JWT.
- Rotazione del token di servizio: aggiornare backend e bot insieme e riavviare; nessun periodo di grazia.
- SQL parametrizzato; il frontend applica l'escaping HTML dei dati dinamici.
- Segreti fuori da Git. Non pubblicare log completi, cookie, dump o datasource.
- Nessun CORS aperto: frontend same-origin. Reverse proxy HTTPS, CSP, integrità CDN di Bootstrap, retention, backup e
  secret manager restano fuori dalla consegna.

## Regole di rilascio

La demo usa solo dati sintetici. Prima di un uso con dati reali: risolvere SEC-03, collaudare HTTPS e configurazione,
raccogliere le approvazioni.

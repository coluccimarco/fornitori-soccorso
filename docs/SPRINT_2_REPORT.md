# Report Sprint 2 — Backend missioni

**Data:** 03/11/2026 · **Branch:** `feature/backend` · **Owner:** Membro 1

## Risultato

Il branch implementa il workflow backend centrale/autista sopra la baseline
Sprint 1. La CI Java 25 e il job PostgreSQL 18 (schema, seed, baseline e test
d'integrazione) sono **PASS**; le 13 operazioni Sprint 2 sono marcate
`implemented` in OpenAPI. La verifica manuale contro il PostgreSQL locale rimane
bloccata perché la sessione non dispone della password autorizzata per
`fornitori_app`. Non sono state cambiate credenziali o dati locali.

## Funzionalità implementate

- Creazione e consultazione missioni live/reference, con validazione di cliente,
  coordinate, officina, canale e chilometri stimati.
- Proposta/riproposizione ad autista libero con timeout configurabile, storico
  immutabile delle risposte, lock pessimisti e scadenza schedulata live-only.
- Accept con controllo `now < deadline`, reject e chiusura protetti dalle
  transizioni di stato; la doppia assegnazione è verificata dal service e vincolata dal DB.
- Calcolo costi `BigDecimal` in Europe/Rome, fascia 20:00–08:00, arrotondamento
  `HALF_UP`; rettifica centrale motivata con audit.
- Note missione e controllo server-side su ruolo/autista e storico proposte.
- Dashboard reference/live, totale e dettaglio per autista; API missioni aperte e
  storico/guadagno dell'autista autenticato.
- Eventi e outbox atomici per accept/completamento. `channel=internal` non
  rappresenta ancora una consegna Telegram/WhatsApp.

## Contratto, test e CI

`api/openapi.yaml` descrive payload e ruoli aggiornati, incluse `kmStimati` e il
riepilogo per autista. Tutte le 13 operazioni Sprint 2 sono promosse a
`implemented` dopo la validazione PostgreSQL CI.

Verifica mirata locale con Temurin 25.0.5:

- `MissionServiceTests`, `PricingServiceTests`, `ApiSecurityTests`:
  **13 test superati, 0 fallimenti, 0 errori**.
- `MissionPostgresIntegrationTests`: **3 test saltati** per variabile
  `SPRING_DATASOURCE_PASSWORD` non configurata in locale. Sono inclusi flusso
  completo, reject/riproposizione/scadenza, rettifica/dashboard/storico e
  concorrenza; gli stessi test sono stati eseguiti con successo in CI.
- Connessione `psql --no-password` a `localhost:5432`: **BLOCKED**,
  `fe_sendauth: no password supplied`.
- `.\mvnw.cmd clean verify`: **BUILD SUCCESS**, 17 test totali, 0 fallimenti,
  0 errori, 4 saltati localmente (3 PostgreSQL e il test di caricamento context).
- OpenAPI YAML: **31 operazioni** parsate con status e ruolo validi.
- `git diff --check` e verifica Java 25/package layout: **PASS**.
- CI remota Java/OpenAPI e PostgreSQL 18: **PASS**, inclusi baseline SQL e test DB.

## Dipendenze e passaggi successivi

1. M2 integra le schermate centrale/autista; M5 concorda il routing outbox
   `internal`; chatbot e mappe restano componenti separati.
2. Confermare col docente il timestamp di accettazione usato per la tariffa.

Nessun merge in `main` è stato eseguito.

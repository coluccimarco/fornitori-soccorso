# Registro decisioni e questioni aperte

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Architecture Owner<br>
**Indice:** fixture/live · tariffa · rounding · autenticazione · Java 25

## D-001 — Storico docente e missioni live

- **Decisione:** importare lo storico con `is_fixture=TRUE`; la disponibilità e
  lo scheduler operativo esaminano esclusivamente righe `is_fixture=FALSE`.
- **Motivo:** non alterare il riepilogo storico né marcare come scadute missioni
  già aperte alla data del 28/10/2026.
- **Conseguenza:** le due assegnazioni aperte dell'autista 3 restano intatte; la
  verifica di disponibilità live non si basa sullo stato seed.
- **Stato:** adottata nel modello e nel seed; flusso live implementato in Sprint 2,
  verifica PostgreSQL CI pendente.

## D-002 — Fascia tariffaria notturna

- **Decisione applicata, da confermare col docente:** usare il timestamp di accettazione della missione;
  fascia notturna 20:00–08:00, estremi inclusi, moltiplicatore 1,25.
- **Fonte:** il foglio Excel precisa l'ora di accettazione; il PDF definisce la
  fascia ma non il timestamp da usare.
- **Stato:** implementata in Sprint 2; conferma di business ancora aperta.

## D-003 — Rounding

- **Decisione:** memorizzare costi con `NUMERIC(10,2)` e importi Java con
  `BigDecimal`; non arrotondare chilometri prima del calcolo.
- **Rounding:** arrotondamento finale `HALF_UP` a due decimali, verificato con i
  casi tariffari diurni/notturni.
- **Stato:** adottata e testata a livello unitario.

## D-004 — Autenticazione

- **Decisione:** sessione server-side, JSON per login, cookie sessione HttpOnly,
  BCrypt e CSRF in sessione.
- **Stato:** implementata; credenziali demo in chiaro non vengono riportate nei
  file versionati.

## D-005 — Versione Java nel POM

- **Decisione:** Java 25 LTS è il target ufficiale; `java.version` e
  `maven.compiler.release` sono entrambi 25. Maven Wrapper resta 3.9.9.
- **Motivo:** requisito condiviso del progetto e verifica riuscita con Temurin
  25.0.5. Spring Boot 4.1.2 è mantenuto senza cambio major.
- **Stato:** target compilato e `clean verify` superato su Java 25 il 03/11/2026.

## D-006 — Canale outbox notifiche

- **Decisione:** scrivere eventi `MISSION_ACCEPTED` e `MISSION_COMPLETED`
  nell'outbox nella stessa transazione di dominio; usare `channel=internal` finché
  M1/M5 non concordano il mapping dei provider.
- **Conseguenza:** l'outbox è un'integrazione interna pronta per un dispatcher;
  non invia messaggi Telegram o WhatsApp.
- **Stato:** applicata nel backend Sprint 2; dispatcher e contratto provider
  restano fuori perimetro.

# Registro decisioni e questioni aperte — storico

> **Documento storico.** Per stato consolidato e decisioni recenti consultare
> [management/DECISION_LOG.md](./management/DECISION_LOG.md). Le righe D-001–D-009
> seguenti sono conservate senza riscrivere il contesto storico e non prevalgono
> sulle verifiche al commit `25c613b`.

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Architecture Owner<br>
**Indice:** fixture/live · tariffa · rounding · autenticazione · Java 25

## D-001 — Storico docente e missioni live

- **Decisione:** importare lo storico con `is_fixture=TRUE`; la disponibilità e
  lo scheduler operativo esaminano esclusivamente righe `is_fixture=FALSE`.
- **Motivo:** non alterare il riepilogo storico né marcare come scadute missioni
  già aperte alla data del 28/10/2026.
- **Conseguenza:** le due assegnazioni aperte dell'autista 3 restano intatte; la
  verifica di disponibilità live non si basa sullo stato seed.
- **Stato:** adottata nel modello e nel seed; flusso live implementato in Sprint 2
  e verificato in PostgreSQL CI.

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

## D-007 — Autenticazione e verifica del chatbot

- **Decisione:** il chatbot si autentica con un token di servizio fisso
  (`FORNITORI_CHAT_TOKEN`); senza token configurato le API chat rifiutano tutto.
  Il cliente si riconosce dalla sola targa con polizza valida, mostrando nome e
  compagnia; lo stato si cerca per chat, equivalente al numero di telefono del foglio.
- **Motivo:** segue il foglio `DialoghiChatbot` del docente. Telegram non comunica il
  numero del cliente.
- **Conseguenza:** chi conosce una targa vede nome e compagnia del cliente e può
  creare una richiesta. È accettabile con dati inventati, non in produzione, dove
  servirebbe una verifica del titolare. Il limite di 5 targhe non trovate per
  sessione resta in memoria e si azzera al riavvio.
- **Stato:** applicata; sostituisce la verifica con la polizza e il requisito di
  challenge della guida M3.

## D-008 — Traino sulle missioni da chatbot

- **Decisione:** il chatbot crea sempre `riparazione_sul_posto`; la centrale imposta
  traino e officina con `PATCH /api/v1/missioni/{id}/intervento` prima della proposta.
- **Motivo:** il cliente in panne non sceglie l'officina convenzionata.
- **Stato:** API applicata; manca il comando nell'interfaccia della centrale (M2).

## D-009 — Notifiche al cliente

- **Decisione:** aggiorna D-006. Le missioni create da chat salvano la chat del
  cliente e i loro eventi usano il canale di origine; il bot legge la coda da
  `GET /api/v1/chat/notifiche`, invia e registra l'esito. Dopo 5 invii falliti la
  notifica passa a `FAILED`.
- **Motivo:** Telegram può scrivere solo a chi ha aperto una chat con il bot, e usa
  long polling, quindi non serve un indirizzo pubblico.
- **Conseguenza:** le missioni create dalla centrale restano `internal` e non inviano
  messaggi: il cliente chiamato al telefono non ha una chat nota. La consegna è
  "almeno una volta": un crash del bot tra invio ed esito può ripetere il messaggio.
- **Stato:** applicata per Telegram; WhatsApp resta da collegare (M5).

## D-010 — Geocodifica e percorso

- **Decisione:** il backend fa da tramite verso i servizi pubblici Nominatim e OSRM.
  L'indirizzo si converte in coordinate alla prima richiesta di percorso, non alla
  creazione, così un servizio lento non blocca chatbot e centrale.
- **Motivo:** nessuna chiave o carta di credito, adatto al perimetro didattico.
- **Conseguenza:** gli indirizzi dei clienti arrivano a servizi terzi. I server pubblici
  hanno limiti d'uso e nessuna garanzia, quindi in produzione ne servirebbero di propri.
  La precisione dipende da OpenStreetMap: un numero civico assente porta alla via.
  Se i servizi non rispondono non si inventano km e la chiusura resta possibile.
- **Stato:** API applicata; la visualizzazione sulla mappa è da fare nella UI (M2/M4).

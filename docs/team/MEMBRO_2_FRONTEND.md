# Membro 2 — Frontend

**Responsabili:** Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio ·
**Versione:** 2.1 · **Data:** 05/11/2026

## Stato

Il frontend HTML/CSS/JavaScript con Bootstrap 5 e Leaflet è servito da Spring Boot da
`src/main/resources/static/`. Si avvia con il backend e si apre da `http://localhost:8080/`.

## Schermate e flussi

- Pagina pubblica informativa e accesso all'area riservata.
- Login e logout con sessione e token CSRF.
- Dashboard operativa: coda critica con azioni dirette e un contatore per motivo (verifica
  polizza, rifiutate da 3 autisti, supporto aperto); 6 riquadri (Critiche, Da assegnare, KO,
  OK, Pending, Annullate) e grafico per stato, che aprono l'elenco filtrato. Si aggiorna ogni
  30 secondi.
- Costi e analisi: totali, costo per autista e per mese, guadagni, conteggi delle anagrafiche.
- Centrale: ricerca e consultazione di clienti, autisti, mezzi, compagnie e officine;
  inserimento clienti. La ricerca si azzera al cambio vista.
- Notifiche centrale: campanella con rifiuti, scadenze e supporto, controllo ogni 15 secondi;
  "letto" è salvato solo nel browser di chi guarda, e le richieste di supporto aperte restano.
- Missioni: creazione, filtri, proposte agli autisti (anche "al più vicino"), countdown,
  avvisi, storico proposte, verifica polizza, annullamento con motivo, correzione del costo
  sulle missioni eseguite o annullate con costo attribuito.
- Correzione dell'intervento e dell'officina per le missioni nuove, rifiutate, scadute o in
  gestione manuale.
- Area autista: disponibilità, proposte con i contatti, accettazione e rifiuto, missione
  attiva, avvisi e richiesta di supporto, chiusura con km totali, annullamento e storico con
  guadagni. Le nuove proposte compaiono da sole: la pagina controlla ogni 15 secondi e
  ridisegna solo se qualcosa è cambiato.
- Il pulsante del chatbot punta a `https://t.me/Fornitori_Soccorso_Bot`, un link assoluto.
- Layout responsive, con area autista ottimizzata per smartphone.

## Dati e contratto

Le richieste usano gli endpoint REST di [`api/openapi.yaml`](../../api/openapi.yaml), con
cookie same-origin e header `X-CSRF-TOKEN` sulle mutazioni e sulla `GET` delle mappe.
Non vengono simulati dati o mappe.

La risposta missione contiene gli ID di cliente e officina. La centrale ricava i dati
dalle anagrafiche; l'autista li riceve da `GET /api/v1/autista/missioni/{id}`, che aggiunge
il guadagno stimato. Nello storico l'autista vede solo l'ID cliente.

La mappa Leaflet usa `/api/v1/maps/route`: nell'area autista si carica da sola, nel pannello
della centrale con "Mostra mappa".

## Errori e caricamento

I messaggi d'errore mostrano il testo `message` del server, per esempio "Targa già
presente"; in mancanza traducono il codice `error`. Liste missioni e storico mostrano un
indicatore di caricamento; ogni lista ha un proprio stato vuoto.

## Avvio

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Le credenziali del database vanno configurate nell'ambiente locale secondo il
[README](../../README.md).

**Riferimenti:** [contratti](./INTEGRATION_CONTRACTS.md),
[API](../architecture/API_SPECIFICATION.md), [manuale utente](../manuals/MANUALE_UTENTE.md).

# Membro 2 — Frontend

**Responsabili:** Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio ·
**Versione:** 2.0 · **Data:** 04/11/2026

## Stato

Il frontend HTML/CSS/JavaScript con Bootstrap 5 e Leaflet è servito da Spring Boot da
`src/main/resources/static/`. Si avvia con il backend e si apre da `http://localhost:8080/`.

## Schermate e flussi

- Pagina pubblica informativa e accesso all'area riservata.
- Login e logout con sessione e token CSRF.
- Centrale: conteggi delle anagrafiche, ricerca e consultazione di clienti, autisti,
  mezzi, compagnie e officine; inserimento clienti. La ricerca si azzera al cambio vista.
- Avvisi: campanella con proposte rifiutate o scadute, controllo ogni 15 secondi; "letto"
  è salvato solo nel browser di chi guarda.
- Missioni: creazione, filtri, proposte agli autisti (anche "al più vicino"), countdown,
  dashboard, segnalazioni, storico proposte, correzione del costo sulle missioni eseguite.
- Correzione dell'intervento e dell'officina per le missioni nuove, rifiutate o scadute.
- Area autista: proposte, accettazione e rifiuto, missione attiva, note, chiusura con
  chilometri e storico con guadagni. Le nuove proposte compaiono da sole: la pagina
  controlla ogni 15 secondi e ridisegna solo se qualcosa è cambiato.
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

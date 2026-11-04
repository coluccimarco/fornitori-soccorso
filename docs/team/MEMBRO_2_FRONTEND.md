# Membro 2 — Frontend

## Stato

Il frontend HTML/CSS/JavaScript con Bootstrap 5 è servito da Spring Boot nella
cartella `src/main/resources/static/`. Si avvia insieme al backend e si apre da
`http://localhost:8080/`.

## Schermate e flussi

- Pagina pubblica informativa e accesso all'area riservata.
- Login/logout con sessione e token CSRF.
- Centrale: conteggi delle anagrafiche, ricerca e consultazione di clienti,
  autisti, mezzi, compagnie e officine; inserimento clienti.
- Avvisi: campanella con proposte rifiutate o scadute, controllo ogni 15 secondi;
  "letto" è salvato solo nel browser di chi guarda.
- Missioni: creazione, filtri, proposte agli autisti (anche "al più vicino"), countdown, dashboard e
  consultazione delle segnalazioni.
- Area autista: proposte, accettazione/rifiuto, missione attiva, note, chiusura
  con chilometri e storico con guadagni.
- Layout responsive, con area autista ottimizzata per smartphone.

Le richieste usano soltanto gli endpoint REST implementati in
[`api/openapi.yaml`](../../api/openapi.yaml), con cookie same-origin e header
`X-CSRF-TOKEN` sulle mutazioni. Non vengono simulati dati o mappe.

## Limiti del contratto missioni

La risposta missione contiene gli ID di cliente e officina. La centrale ricava i dati
dalle anagrafiche autorizzate; l'autista li riceve da `GET /api/v1/autista/missioni/{id}`,
che aggiunge anche il guadagno stimato. Nello storico l'autista vede solo l'ID cliente.

La mappa Leaflet usa `/api/v1/maps/route`: nell'area autista si carica da sola, nel
pannello con "Mostra mappa". La centrale corregge il costo delle missioni eseguite.

## Verifica e avvio

Con JDK 25 e database configurato, eseguire:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Le credenziali del database vanno configurate nell'ambiente locale secondo il
[README](../../README.md); non sono incluse nel frontend né nel repository.

**Acceptance:** mostrare solo dati restituiti dal server, gestire loading/errori e
permessi, mantenere il layout responsive; la verifica browser deve essere
registrata separatamente dalla presenza del codice.
**Riferimenti:** [contratti](./INTEGRATION_CONTRACTS.md),
[API](../architecture/API_SPECIFICATION.md), [manuale utente](../manuals/MANUALE_UTENTE.md).

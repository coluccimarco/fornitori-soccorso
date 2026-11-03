# Membro 4 — Mappe e routing

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 4<br>
**Indice:** obiettivo · stack · route · payload · fallback · test

## Scopo e stato

Visualizzazione percorso e distanza. **IMPLEMENTATA** l'API `GET /api/v1/maps/route` con
geocodifica Nominatim e percorso OSRM ([D-010](../DECISION_LOG.md)). Da fare: mostrare
la mappa con Leaflet nel pannello e nell'area autisti e proporre i km alla chiusura.

## Tecnologie proposte

Leaflet per UI e tiles OpenStreetMap secondo attribution/usage policy. OSRM o altro
routing provider da confermare; rispettare limiti, cache, privacy e termini del
provider. Geocoding non è geolocalizzazione live: distinguere indirizzo e coordinate.

## Route

- Traino: base del mezzo → luogo sinistro → officina.
- Riparazione sul posto: base del mezzo → luogo sinistro.
- Input richiesti: coordinate origine missione, coordinate base mezzo, officina se
  traino. Se origine indirizzo manca coordinate, geocoding asincrono validato.
- Mostrare route stimata e chilometri con unità e provider; km suggeriti non sono i
  km effettivi di chiusura.
- Ritorno provider indisponibile: mostrare errore/indirizzo, consentire retry; non
  inventare km né bloccare una chiusura autorizzata.

## Contratto proposto

Il backend fa da tramite verso i servizi pubblici: i dettagli sono in OpenAPI. I km
suggeriti restano in `kmStimati` della missione; backend valida km confermati e tariffa.

## Test e acceptance

Testare due tipi intervento, coordinate limite, officina mancante, percorso nullo,
rate limit/offline, errore geocoding e separazione stimato/effettivo. Credenziali
private non nel browser; OpenStreetMap attribution visibile. Acceptance: route
renderizzata e fallback chiaro, km e fonti etichettati.

**Dipendenze:** M1 coordinate/missione/endpoint e decisione provider; M2 UI map.
**Risultato Sprint successivo:** spike route deterministico su coordinate fixture e
specifica di fallback. **Riferimenti:** [API](../04_API_REST.md),
[integration contracts](./INTEGRATION_CONTRACTS.md).

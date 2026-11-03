# Membro 4 — Mappe e routing

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 4<br>
**Indice:** obiettivo · stack · route · payload · fallback · test

## Scopo e stato

Preparare visualizzazione percorso e distanza. Backend fornisce coordinate base
mezzi e officine nelle anagrafiche; integrazione Leaflet/OSM/OSRM è **PIANIFICATA**,
non presente.

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

`GET /api/v1/maps/route` è **PIANIFICATO**, non implementato. OpenAPI definisce
input coordinate e route; M4 concorda se backend fa proxy (preferito per chiavi e
coerenza) o browser chiama servizio pubblico. Il client può inviare km suggeriti
come dato non autorevole; backend valida km confermati e tariffa.

## Test e acceptance

Testare due tipi intervento, coordinate limite, officina mancante, percorso nullo,
rate limit/offline, errore geocoding e separazione stimato/effettivo. Credenziali
private non nel browser; OpenStreetMap attribution visibile. Acceptance: route
renderizzata e fallback chiaro, km e fonti etichettati.

**Dipendenze:** M1 coordinate/missione/endpoint e decisione provider; M2 UI map.
**Risultato Sprint successivo:** spike route deterministico su coordinate fixture e
specifica di fallback. **Riferimenti:** [API](../04_API_REST.md),
[integration contracts](./INTEGRATION_CONTRACTS.md).

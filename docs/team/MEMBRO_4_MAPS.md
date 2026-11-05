# Membro 4 — Mappe

**Responsabile:** Marco Colucci · **Versione:** 2.1 · **Data:** 05/11/2026

## Stato

Il servizio è in `MapController`, `MapService`, `NominatimGeocodingClient` e
`OsrmRoutingClient`. La UI mostra la mappa con Leaflet, indica provider e natura stimata
dei km e li propone alla chiusura. La chiusura resta possibile se il provider non risponde.

## Contratto

```text
GET /api/v1/maps/route?missionId=<id>&driverId=<opzionale>
```

- Richiede sessione, header `X-CSRF-TOKEN` e ruolo RESPONSABILE o AUTISTA assegnato alla
  missione. L'autista usa il proprio punto di partenza; il responsabile può indicare un autista.
- Itinerario: officina di riferimento dell'autista o, in mancanza, base del mezzo → luogo di
  fermo → officina se il tipo è traino → rientro al punto di partenza.
- Se mancano le coordinate dell'origine, il backend geocodifica l'indirizzo con Nominatim
  e le salva sulla missione.
- OSRM restituisce geometria e distanza. La risposta riporta `kmAndata`, `kmTraino`, `kmRitorno`,
  `kmTotali`; `km` e `kmStimati` valgono come `kmTotali`. I km stimati si salvano solo per
  l'autista assegnato e non sono i km effettivi inseriti alla chiusura.

| Esito | Quando |
|---|---|
| 200 con percorso | Autista assegnato o indicato |
| 200 con il solo punto del sinistro | Nessun autista assegnato né indicato: `kmStimati` e `geometria` sono null |
| 400 | `missionId` mancante o non numerico |
| 401 / 403 | Sessione assente, CSRF mancante, ruolo errato o missione di un altro autista |
| 404 | Missione o autista inesistente |
| 422 | Indirizzo non trovato |
| 503 | Servizio esterno non disponibile |

Il comportamento del codice è stato scelto come riferimento perché coincide con OpenAPI e
con la UI, che usa il 200 per mostrare il luogo prima della scelta dell'autista.

## Provider e privacy

I default sono Nominatim pubblico (`User-Agent` configurabile) e OSRM pubblico. Il client
Nominatim rispetta il limite di una richiesta al secondo per istanza; la ricerca è limitata
alla Puglia. Il backend usa timeout e restituisce errori espliciti; la disponibilità dei
servizi pubblici non è garantita. La geocodifica trasmette indirizzi a un servizio esterno:
adatto alla demo, da rivalutare per dati reali. Le proprietà sono in
[`MANUALE_TECNICO.md`](../manuals/MANUALE_TECNICO.md).

**Riferimenti:** [API](../architecture/API_SPECIFICATION.md),
[contratti](./INTEGRATION_CONTRACTS.md).

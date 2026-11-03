# Membro 4 — Mappe e routing

**Versione:** 1.1 · **Data:** 03/11/2026 · **Responsabile:** Membro 4

## Stato e responsabilità

Il servizio backend è implementato in `MapController`, `MapService`,
`NominatimGeocodingClient` e `OsrmRoutingClient`. `GET /api/v1/maps/route` restituisce
punti, geometria e km stimati; i test automatici sono inclusi nella CI
`41656606659` su `f763d53`. L'uso reale dei provider pubblici non è stato verificato
in questa revisione. La UI web non richiama l'endpoint e non visualizza una mappa.

## Contratto runtime

```text
GET /api/v1/maps/route?missionId=<id>&driverId=<opzionale>
```

- Richiede sessione autenticata e ruolo RESPONSABILE o AUTISTA assegnato alla
  missione. L'autista usa il proprio mezzo; il responsabile può indicare un driver.
- Itinerario: base mezzo → luogo sinistro; aggiunge officina se il tipo è traino.
- Se mancano coordinate dell'origine, il backend geocodifica l'indirizzo con
  Nominatim e salva coordinate e km stimati associati alla missione.
- OSRM restituisce route geometry e distanza arrotondata a due decimali. I km
  stimati non sono i km effettivi inseriti dall'autista alla chiusura.
- `400`: driver non disponibile/indicato; `422`: indirizzo non trovato;
  `503`: servizio esterno non disponibile; `401/403/404`: sessione, permesso o
  risorsa.

## Provider e privacy

I default sono Nominatim pubblico (`User-Agent` configurabile, limite di una
richiesta al secondo) e OSRM pubblico. Il backend usa timeout e restituisce errori
espliciti; provider e rete non sono garantiti. La geocodifica trasmette indirizzi
a un servizio esterno: valutarne minimizzazione, policy, rate limit e licenza prima
di un uso reale. Le proprietà sono documentate in
[`MANUALE_TECNICO.md`](../manuals/MANUALE_TECNICO.md).

## Integrazione e verifica ancora necessarie

- M2 deve integrare una vista cartografica/route nel browser, se richiesta dal
  requisito approvato; al momento non c'è chiamata JS a `/maps/route`.
- Etichettare chiaramente provider, unità e natura stimata dei km.
- Non bloccare la chiusura missione quando il provider non risponde e non
  inventare distanze.
- Le suite `MapProviderClientsTests` e `MapPostgresIntegrationTests` verificano
  il codice con test automatici; test provider live, UX/browser e acceptance
  docente restano NOT RUN/DA CONFERMARE.

**Riferimenti:** [API](../architecture/API_SPECIFICATION.md),
[contratti](./INTEGRATION_CONTRACTS.md), [piano test](../quality/TEST_PLAN.md).

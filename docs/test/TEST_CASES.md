# Test cases FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — QA Coordinator<br>
**Indice:** backend presenti · DB · missioni future · componenti futuri · esiti

## Scopo

Catalogo iniziale; non confondere casi progettati con test già eseguiti. Stato
IMPLEMENTATO si riferisce ai sei test in `ApiSecurityTests`; altri casi sono
PIANIFICATI/DA VALIDARE.

| ID | Scenario | Atteso | Stato |
|---|---|---|---|
| AUTH-01 | richiesta anagrafica anonima | 401 JSON | Implementato / PASS |
| AUTH-02 | AUTISTA legge clienti centrale | 403 JSON | Implementato / PASS |
| AUTH-03 | RESPONSABILE legge autisti | 200 JSON | Implementato / PASS |
| AUTH-04 | password errata | 401 senza dettagli sensibili | Implementato / PASS |
| AUTH-05 | login corretto, me, logout | sessione creata e invalidata | Implementato / PASS |
| AUTH-06 | GET CSRF | token + header `X-CSRF-TOKEN` | Implementato / PASS |
| DB-01 | applica schema, seed, assertion | baseline attesa | CI configurata; locale blocked |
| DB-02 | FK cliente/compagnia, targa normalizzata unique | vincoli rifiutano dati invalidi | Da validare |
| MIS-01 | accetta prima della deadline e al confine | solo `< deadline` ammessa | Pianificato Sprint 2 |
| MIS-02 | due proposte concorrenti allo stesso autista | una transazione respinta | Pianificato Sprint 2 |
| MIS-03 | scheduler dopo timeout | scade solo missione live | Pianificato Sprint 2 |
| MIS-04 | chiusura e retry duplicato | costo calcolato una volta | Pianificato Sprint 2 |
| MAP-01 | traino vs riparazione sul posto | route con tappe previste | Pianificato Sprint 3 |
| CHAT-01 | richiesta e verifica targa | nessun accesso cross-customer | Pianificato Sprint 3 |
| NOT-01 | callback provider duplicato | un solo evento effettivo | Pianificato Sprint 3 |

## Criterio

Registrare command, runtime, commit, esito e link CI in
[TEST_EVIDENCE.md](../TEST_EVIDENCE.md). Non segnare test futuri PASS in base al
contratto o alla presenza degli script SQL.

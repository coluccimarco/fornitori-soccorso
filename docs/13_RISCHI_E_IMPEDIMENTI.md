# Rischi e impedimenti

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Scrum Master — Membro 1<br>
**Indice:** registro · mitigazioni · escalation · chiusura

## Scopo

Registro vivo dei blocchi. Stato **BLOCCATO** indica una dipendenza che impedisce la
verifica o la delivery; non equivale a funzionalità fallita.

| ID | Rischio | Stato | Impatto | Mitigazione / owner |
|---|---|---|---|---|
| R-01 | DB locale non accessibile senza credenziali; Docker Engine locale HTTP 500 | BLOCCATO locale; CI PostgreSQL configurata | Import/assertion e runtime locale non certificati | M1 ottiene credenziali autorizzate o osserva CI effimera; mai aggirare auth |
| R-02 | Esecuzione CI GitHub non ancora osservata | DA VALIDARE | Quality gate remoto non attestato | M1 controlla workflow dopo push e registra link/risultato |
| R-03 | Fascia notturna: PDF non specifica timestamp | DA CONFERMARE | Costi divergenti | M1 chiede decisione docente prima di pricing Sprint 2 |
| R-04 | File Excel e hash/password di test sono dati sensibili | APERTO | Privacy e leakage | Conservare Excel fuori Git; mai stampare o versionare password |
| R-05 | Missioni fixture includono assignment incoerenti e record passati | MITIGATO | Scheduler altera baseline | Separare con `is_fixture`; testare solo live |
| R-06 | Contratti missione/chat/mappe/notifiche progettati ma non approvati dai consumer | IN CORSO | Integrazione e rework | Review M2–M5 e contratti in OpenAPI prima dello sviluppo |
| R-07 | Hosting, backup, monitoring e provider non scelti | PIANIFICATO | Nessuna distribuzione reale | Definire in Sprint 4 dopo requisiti e disponibilità |

## Escalation e completamento

Registrare evidenza, owner, prossima azione e scadenza nell'issue. Blocchi che
impattano Sprint Goal sono escalati al daily; nessun segreto deve essere allegato.
Chiudere un rischio solo quando la mitigazione è verificata.

**Dipendenze:** accesso sicuro a DB/Excel, feedback docente, esecuzione CI e review.
**Riferimenti:** [evidenze](./TEST_EVIDENCE.md), [decisioni](./DECISION_LOG.md),
[piano Scrum](./08_AGILE_SCRUM.md).

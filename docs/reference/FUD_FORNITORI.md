# FUD — Functional User Design FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Team Lead<br>
**Indice:** flussi · schermate · componenti · errori · acceptance

## Scopo e maturità

Vista funzionale destinata a guidare l'implementazione e la demo. Solo login e
consultazione anagrafiche hanno backend Sprint 1 **IMPLEMENTATO**; schermate e flussi
qui descritti sono **PIANIFICATI** finché non collegati e testati.

## Flusso centrale

Login → dashboard → ricerca/creazione cliente → creazione missione → proposta
autista → timer e risultato → eventuale riproposizione → dettaglio/chiusura →
storico e costi. La centrale può effettuare rettifica con motivazione e permesso.

## Flusso autista

Login → lista missioni assegnate → dettaglio e route → accept/reject entro deadline
→ intervento → inserimento km effettivi → chiusura → riepilogo personale.
L'autista non può vedere dati di altri driver o rettificare tariffe.

## Flusso cliente/chatbot

Start menu → verifica identità/targa → posizione → problema → riepilogo/consenso →
creazione richiesta nuova → conferma; successivo stato disponibile solo dopo
ulteriore verifica. Notifica di presa in carico/completamento tramite provider
approvato. Chatbot AI non è requisito.

## Schermate e stati comuni

Login, dashboard, anagrafiche, mission create/detail, driver mission panel, map,
history, chat interaction, notification status. Tutte le schermate gestiscono
loading, empty, validation error, unauthorized, conflict, unavailable integration.
Nessuna UI mostra un futuro endpoint come dato live.

## Criteri e dipendenze

Responsive e accessibile, dati da API, autorizzazioni server-side, test di flusso e
errori, demo con dati autorizzati. Dipende da OpenAPI e team guides.<br>
**Riferimenti:** [Membro 2](../team/MEMBRO_2_FRONTEND.md),
[Chatbot](../team/MEMBRO_3_CHATBOT.md), [TAD](../TAD_FORNITORI.md).

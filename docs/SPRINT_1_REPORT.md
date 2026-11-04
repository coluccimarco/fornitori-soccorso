# Report Sprint 1 — backend e database

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)

## Risultato

Lo Sprint ha portato il repository da un progetto Spring Boot minimo a una base condivisa: `pom.xml` su Java 25,
schema e seed PostgreSQL, autenticazione a sessione, anagrafiche, contratto OpenAPI e pipeline.

## Contenuti

- **Database:** 13 tabelle iniziali con FK, chiavi univoche, check, indici, `NUMERIC`, `TIMESTAMPTZ`, fixture
  storiche e outbox; seed con hash BCrypt; assertion su conteggi e guadagni per autista.
- **API:** login, logout, profilo, CSRF, lettura di autisti, mezzi, compagnie, clienti (ricerca e inserimento) e
  officine. La disponibilità degli autisti è calcolata solo sulle missioni operative.
- **Configurazione:** datasource da ambiente, Hibernate `validate`, cookie di sessione protetto.

## Decisioni aperte allora

L'Excel usa l'ora di accettazione per la maggiorazione notturna; il PDF non indica quale timestamp usare. Il foglio
missioni ha due missioni attive per l'autista 3: i record restano invariati e separati dall'operatività live.

Questo report descrive lo Sprint; lo stato attuale è nel [registro Scrum](./agile/README.md).

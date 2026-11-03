# Testing e Quality Assurance

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — QA Coordinator<br>
**Indice:** livelli · comando · dataset · casi · evidenze · criteri

## Scopo e stato

Definisce verifiche riproducibili. Backend MVC: **PASS su Java 25** (11 test mirati). PostgreSQL 18 locale è in esecuzione e
la porta `localhost:5432` è raggiungibile; il test context con il DB locale resta
**BLOCKED** perché non sono configurate le credenziali dell'utente applicativo.
Il workflow CI con DB effimero e assertion baseline è **PASS**. Nessun test
UI/chat/mappe è ancora disponibile.
`clean verify` sul branch Sprint 2 ha terminato con BUILD SUCCESS: 17 test totali,
0 fallimenti, 0 errori, 4 saltati (i tre PostgreSQL e il test di caricamento
contestuale). I test mirati risultano 13 totali, 11 eseguiti e 2 saltati per
assenza della password; la nuova persistenza/concorrenza resta in attesa di CI.

## Livelli

1. Unit/service: regole pure, validazione, tariffe e transizioni.
2. Web slice: status, sessione, CSRF, permessi, DTO e errori.
3. Integration PostgreSQL: schema/seed, FK, indici, transazioni e query.
4. Component/E2E: flussi browser, chatbot, routing, provider mock/sandbox.
5. Quality static: Java 25, `clean verify`, OpenAPI status, `git diff --check`.

## Comandi backend

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
git diff --check
```

Il JDK deve essere 25, senza `-Dmaven.compiler.release` alternativo. Per DB locale
applica schema, seed e assertion come in [DATABASE](./03_DATABASE.md).

## Dataset e risultati

Excel di riferimento: 10 utenti, 8 autisti, 8 mezzi, 4 compagnie, 40 clienti,
6 officine, 30 missioni; 16 completate e €1.509,71. Sorgente e password non vanno
versionate. Testi dei casi in [`test/TEST_CASES.md`](./test/TEST_CASES.md);
risultati datati in [TEST_EVIDENCE](./TEST_EVIDENCE.md).

## Criteri

Ogni PR scrive comando/versione/result; PASS solo con exit 0 e assertion attesa,
BLOCKED solo con causa esterna riproducibile, NOT RUN se non eseguito. Nessun
failure va trasformato in skipped senza motivazione approvata. Il passaggio allo
Sprint 4 richiede test integrazione DB e percorsi end-to-end.

**Dipendenze:** JDK 25, Maven Wrapper, DB effimero/local, dati fixture.<br>
**Completamento:** test richiesti verdi, regressioni valutate, evidenze aggiornate.<br>
**Riferimenti:** [CI workflow](../.github/workflows/backend-ci.yml),
[DoD](./11_DEFINITION_OF_DONE.md).

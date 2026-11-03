# FORNITORI — Start here

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Membro 1 — Backend & Database Lead<br>
**Indice:** progetto · stato · setup · team · sviluppo · documentazione

## Scopo

Onboarding rapido per i cinque studenti ITS Developer 4.0 che sviluppano FORNITORI
in parallelo. Leggi questa guida prima di modificare codice o contratti.

## 1. Che cos'è FORNITORI

Sistema per coordinare il soccorso stradale: una centrale gestisce richieste,
autisti, mezzi e officine; il cliente può avviare una richiesta tramite il canale
conversazionale previsto. Il progetto didattico integra un backend Spring, un
frontend web e componenti chatbot, mappe e notifiche.

## 2. Stato reale al 03/11/2026

- **IMPLEMENTATO:** Java 25, backend Spring Boot per sessioni, CSRF, ruoli e
  anagrafiche, script PostgreSQL e contratto OpenAPI.
- **DA VALIDARE:** import e assertion baseline su PostgreSQL del team; la pipeline
  CI include un database PostgreSQL effimero.
- **PIANIFICATO:** missioni, timeout, pricing, dashboard, UI, chatbot, routing e
  notifiche. Non sono endpoint o interfacce operative.
- **BLOCCATO:** nessuna funzione di Sprint 2 può essere dichiarata conclusa finché
  non è implementata e testata.

## 3. Prima configurazione

1. Installa Git, JDK 25 LTS e PostgreSQL 18 (o compatibile).
2. Clona e seleziona il branch coordinato:

   ```powershell
   git clone https://github.com/coluccimarco/fornitori-soccorso.git
   cd fornitori-soccorso
   git switch feature/backend
   ```

3. Configura `JAVA_HOME` a un JDK 25 e verifica `java -version`,
   `javac -version` e `.\mvnw.cmd -version`.
4. Crea un database locale isolato, imposta le variabili datasource illustrate
   nel [README](../README.md), poi applica gli script SQL in ordine.
5. Esegui `.\mvnw.cmd clean verify`. Segui il [runbook backend](./BACKEND_SPRINT1_RUNBOOK.md)
   per DB, login e smoke test.

Non copiare password o token in Git, documenti o log. Le credenziali demo sorgente
restano nei materiali riservati del corso.

## 4. Componenti e responsabili

| Membro | Componente | Primo documento |
|---|---|---|
| 1 | Backend, database, contratti condivisi | [Backend](./team/MEMBRO_1_BACKEND.md) |
| 2 | Frontend | [Frontend](./team/MEMBRO_2_FRONTEND.md) |
| 3 | Chatbot guidato | [Chatbot](./team/MEMBRO_3_CHATBOT.md) |
| 4 | Mappe e routing | [Mappe](./team/MEMBRO_4_MAPS.md) |
| 5 | Ingressi e notifiche | [Notifiche](./team/MEMBRO_5_NOTIFICATIONS.md) |

Il [contratto d'integrazione](./team/INTEGRATION_CONTRACTS.md) definisce confini,
dipendenze e modalità di coordinamento. Un ruolo non autorizza a cambiare da solo
un contratto consumato da un altro componente.

## 5. Come contribuire

- Lavora sul branch del tuo componente; chiedi review prima di integrare in `main`.
- Per payload, ruoli o URL modifica prima una proposta in
  [`api/openapi.yaml`](../api/openapi.yaml) e concordala con i consumatori.
- Aggiungi test e aggiorna la documentazione nella stessa PR.
- Apri issue per impedimenti o ambiguità; usa i template Scrum in `docs/agile/`.
- Non duplicare regole economiche, autorizzazioni o assegnazione nel frontend.

## 6. Documentazione

| Documento | Scopo |
|---|---|
| [01 Guida generale](./01_GUIDA_GENERALE_TEAM.md) | Stack condiviso, ruoli e rituali |
| [02 Architettura](./02_ARCHITETTURA.md) | Layer Spring e componenti del sistema |
| [03 Database](./03_DATABASE.md) | Modello, fixture e script |
| [04 API REST](./04_API_REST.md) | Regole di consumo e OpenAPI |
| [05 Regole di business](./05_BUSINESS_RULES.md) | Stati missione e pricing pianificati |
| [06 Sicurezza](./06_SICUREZZA.md) | Sessioni, CSRF, privacy e segreti |
| [07 Git workflow](./07_GIT_WORKFLOW.md) | Branch, commit, PR e file condivisi |
| [08 Agile Scrum](./08_AGILE_SCRUM.md) | Piano dei quattro Sprint |
| [09 Testing e QA](./09_TESTING_QA.md) | Strategia, casi ed evidenze |
| [10 Deployment](./10_DEPLOYMENT.md) | Avvio locale e deployment futuro |
| [11 Definition of Done](./11_DEFINITION_OF_DONE.md) | Criteri di completamento |
| [12 Decision log](./DECISION_LOG.md) | Decisioni tecniche |
| [13 Rischi](./13_RISCHI_E_IMPEDIMENTI.md) | Blocchi e mitigazioni |

**Criterio di completamento onboarding:** aver configurato JDK 25, eseguito i test,
letto la propria guida e concordato il primo task con il team lead.

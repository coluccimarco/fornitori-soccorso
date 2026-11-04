# Project Charter — FORNITORI

**Codice:** FOR-CH-001 · **Versione:** 2.0 · **Data:** 04/11/2026
**Sponsor/approvatore:** da confermare con il docente

## Sintesi

FORNITORI è un project work del percorso ITS Developer 4.0: un sistema dimostrativo di coordinamento del soccorso
stradale, con backend Java/Spring e PostgreSQL, client web con mappa e chatbot Python su Telegram.

## Obiettivi

- Modellare anagrafiche, missioni e transizioni operative.
- Separare i ruoli centrale e autista e fornire contratti REST verificabili.
- Preservare le fixture e distinguere lo storico didattico dal lavoro live.
- Mostrare un flusso chatbot su Telegram senza accesso diretto alle tabelle.

## Ambito

**Presente:** API di autenticazione, anagrafiche, missioni, chat e mappe; client web per responsabile e autista con
mappa, rettifica costo, correzione officina e storico proposte; persistenza JPA; schema SQL; dispatcher Telegram;
contratti OpenAPI; pipeline.

**Fuori ambito:** WhatsApp; webhook in ingresso; gestione utenti da interfaccia; infrastruttura cloud e produzione.

**Deliverable documentali:** Markdown, DOCX generati, workbook dei registri e diagrammi Mermaid.

## Organizzazione

| Area | Responsabili | Approvazioni necessarie |
|---|---|---|
| Backend | Vincenzo Minervini | Modifiche a schema e API condivisi |
| Frontend | Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio | UX e integrazione API |
| Chatbot | Marco Colucci | Conversazione, canale e privacy |
| Mappe | Marco Colucci | Provider, licenza e fallback |
| Notifiche | Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi | Provider, segreti e tentativi |
| Docente | Requisiti e decisioni didattiche | Fonte primaria non presente nel repository |

## Vincoli e successo

JDK 25; Spring Boot 4.1.2; Maven Wrapper 3.9.9; PostgreSQL 18; OpenAPI come contratto. Nessuna credenziale viene
committata. Il successo del progetto si dichiara dopo l'acceptance del docente.

Gate: convalida delle fonti, freeze dei requisiti, demo, review finale. Dettagli nel
[registro rischi](./RISK_REGISTER.md).

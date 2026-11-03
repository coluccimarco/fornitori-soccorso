# Project Charter — FORNITORI

**Codice:** FOR-CH-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Sponsor/approvatore:** da confermare con il docente · **Project lead:** ruolo M1 nel repository

## Sintesi

FORNITORI è un project work del percorso ITS Developer 4.0 per realizzare un
sistema dimostrativo di coordinamento del soccorso stradale. Il repository integra
un backend Java/Spring e PostgreSQL e un chatbot Python su Telegram; l'interfaccia
web, le mappe e WhatsApp non sono presenti nella baseline verificata.

## Scopo e obiettivi

- Modellare anagrafiche, missioni e relative transizioni operative.
- Separare ruoli centrale/autista e fornire contratti REST verificabili.
- Preservare dati fixture e distinguere storico didattico dal lavoro live.
- Mostrare un flusso chatbot tramite Telegram senza accesso diretto alle tabelle.
- Produrre evidenze ripetibili con Maven, test e PostgreSQL in CI.
- Preparare il project work per integrazione didattica, con ogni funzione marcata
  in modo trasparente come implementata, parziale o pianificata.

Questi obiettivi sono una baseline derivata dal repository e dai documenti storici,
non un'approvazione della specifica del docente.

## Ambito e consegne

**Presente:** API di autenticazione/anagrafiche/missioni/chat, persistenza JPA,
schema SQL, dispatcher Telegram nel chatbot, contratti OpenAPI, test e CI.

**Non presente nella baseline:** frontend web; calcolo/rendering cartografico;
WhatsApp; infrastruttura cloud/produzione. La disponibilità in OpenAPI di un
endpoint `designed` non è una consegna implementata.

**Deliverable documentali:** Markdown canonico, DOCX modificabili generati,
workbook dei registri, diagrammi Mermaid e audit delle fonti.

## Organizzazione

| Ruolo repository | Ambito primario | Approvazioni necessarie |
|---|---|---|
| M1 | Backend, database, contratti e coordinamento tecnico | Modifiche a schema/API condivisi |
| M2 | Frontend (pianificato) | UX e integrazione delle API |
| M3 | Chatbot Python/Telegram | Conversazione, canale e privacy |
| M4 | Mappe/routing (pianificato) | Provider, licenza, fallback e coordinate |
| M5 | Adapter canale/notifiche | Provider esterni, segreti e policy di retry |
| Docente/team | Requisiti e decisioni didattiche | Fonte primaria non disponibile in questa baseline |

I ruoli non provano che tutte le attività siano state assegnate o approvate per
persona; i nomi degli studenti non sono registrati nel repository esaminato.

## Vincoli e criteri di successo

JDK 25; Spring Boot 4.1.2; Maven Wrapper 3.9.9; PostgreSQL 18 o compatibile;
OpenAPI come contratto. Nessuna credenziale va committata. CI latest sulla baseline
`25c613b` ha entrambi i job in success. Il successo complessivo del progetto non
si dichiara finché i requisiti originali non sono disponibili e le parti UI/mappe
che il docente riterrà obbligatorie non sono state valutate.

## Rischi, ipotesi e gate

La priorità dei requisiti, tempi reali, budget, date Sprint e sign-off docente sono
**DA CONFERMARE**. Gate: convalida fonti, freeze dei requisiti, contratti consumer,
integrazione verificabile, review finale. Dettagli: [registro rischi](./RISK_REGISTER.md)
e [audit](./DOCUMENTATION_AUDIT.md).

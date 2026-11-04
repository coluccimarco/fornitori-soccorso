# Membro 1 — Backend

**Responsabile:** Vincenzo Minervini · **Versione:** 2.0 · **Data:** 04/11/2026

## Responsabilità

Spring Boot, PostgreSQL, OpenAPI e integrazione. Java 25, Spring Boot 4.1.2, Maven
Wrapper 3.9.9. Coordina `pom.xml`, schema, seed e contratti.

## Struttura

`controller` valida HTTP e delega; `dto` definisce i payload; `entity` mappa il DB;
`repository` esegue le query; `service` contiene casi d'uso e transazioni; `enums` i
valori stabili; `config` sessione, CSRF e ruoli; `common` normalizza gli errori. Le
entity e le credenziali non vengono mai esposte.

## Funzioni implementate

- Login JSON, CSRF, logout, profilo, sessione HttpOnly di 30 minuti, limite di 20 login
  al minuto per IP, ruoli `RESPONSABILE` e `AUTISTA`.
- Anagrafiche: autisti, mezzi, compagnie, clienti (ricerca e inserimento), officine.
- Missioni: creazione, proposta, accettazione, rifiuto, chiusura, correzione dell'intervento.
- Storico proposte in sola lettura: `GET /api/v1/missioni/{id}/proposte`, per la centrale.
- Scadenza da `pricing_config`, scheduler solo sulle missioni live, controllo anche al
  momento della risposta dell'autista.
- Lock pessimista su missione e autista; indice DB contro le doppie assegnazioni.
- Chiusura con tariffa server-side, `BigDecimal`, `HALF_UP`, fascia notturna 22:00–08:00
  sull'ora di accettazione, Europe/Rome. Rettifica motivata con audit.
- Dashboard reference e live con totale e subtotali per autista.
- Note solo su missioni live: su una fixture la risposta è 409.
- Outbox nella transazione di dominio: le missioni della centrale non hanno un
  destinatario, quindi la riga nasce `SKIPPED` e non conserva il telefono del cliente.

## Database

Applicare gli script di `database/` in ordine su un DB vuoto. Su un DB già inizializzato
applicare solo gli script mancanti. Hibernate resta in `validate`. Per un uso condiviso
tenere separati l'account delle migrazioni e quello applicativo.

## Regole e chiusura del lavoro

Log senza password, token o dati personali; messaggi API generici per gli errori
inattesi. Prima di ogni PR: `./mvnw clean package`, prova manuale del flusso, diff check
e OpenAPI.

**Riferimenti:** [manuale tecnico](../manuals/MANUALE_TECNICO.md),
[regole](../requirements/BUSINESS_RULES.md).

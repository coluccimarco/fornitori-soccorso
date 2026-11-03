# Dizionario dati

**Codice:** FOR-DICT-001 · **Versione:** 1.0 · **Data:** 03/11/2026

| Tabella | Significato | Vincoli/relazioni rilevanti |
|---|---|---|
| `app_users` | Credenziali hash e ruolo applicativo | Email unica; ruolo check; BCrypt non restituito da DTO |
| `vehicles` | Mezzo e coordinate base | Targa unica; portata positiva; lat/lng in range |
| `drivers` | Anagrafica autista | FK univoca a utente e mezzo; `stato_seed` è dato importato, non fonte live |
| `insurance_companies` | Compagnie | Referenziate da clienti |
| `customers` | Cliente, veicolo/targa, polizza e compagnia | `targa_norm` unica; compagnia obbligatoria |
| `workshops` | Officine e coordinate | coordinate validate |
| `pricing_config` | Parametri tariffari e timeout | singolo record; importi/timeout non negativi |
| `missions` | Lavoro, stato, località, date, importi | FK; stato/coordinate/costi check; fixture separate dal live |
| `mission_proposals` | Tentativo per missione/autista e deadline | FK; deadline > proposta; indice una proposta aperta live |
| `mission_notes` | Nota dell'utente su missione | FK; 1–2000 caratteri non blank |
| `mission_events` | Audit evento e dettaglio | FK missione/actor; detail JSONB |
| `chat_sessions` | Identità di sessione esterna e contesto | canale + external id unici; dati JSONB |
| `notifications_outbox` | Messaggi pendenti/inviati/falliti | event key univoca, stato e tentativi |

Per colonne/precisione, script SQL è la fonte normativa; questa tabella riassume
entità, non sostituisce DDL.

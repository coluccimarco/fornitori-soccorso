# Registro rischi e impedimenti

**Codice:** FOR-RSK-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Scale qualitative: Alto / Medio / Basso. Owner indica il ruolo proposto, non un nome.

| ID | Rischio / evidenza | Prob. | Impatto | Mitigazione / trigger | Owner | Stato |
|---|---|---:|---:|---|---|---|
| R-01 | PDF docente e Excel originale non sono disponibili nella working tree | Alta | Alto | Acquisirli prima di approvare BUR/dataset; bloccare claims di conformità | Docente/M1 | BLOCCATO |
| R-02 | Contratto OpenAPI e comportamento runtime possono divergere; gli status non attestano E2E | Media | Alto | Aggiornare contratto e test con ogni modifica; run latest `41656606659` PASS | M1 | MITIGATO / monitorare |
| R-03 | UI presente ma non coperta da browser E2E; alcuni flussi centrali non sono esposti | Alta | Alto | Testare login, accessibilità, browser e percorsi missione; confrontare richieste UX | M2/team | APERTO |
| R-04 | API mappe dipende da Nominatim/OSRM pubblici; mappa UI e WhatsApp non integrati | Alta | Medio/alto | Rispettare rate limit/privacy; pianificare fallback/provider e vista UI se richiesti | M4/M5 | PARZIALE |
| R-05 | Accesso PostgreSQL locale non verificato senza credenziali; Docker Engine locale ha risposto HTTP 500 | Media | Medio | Usare ambiente autorizzato; CI effimera è verde e non sostituisce setup locale | M1 | DA CONFERMARE |
| R-06 | Ricerca cliente via targa non verifica titolarità (limite dichiarato nel README chatbot) | Alta | Alto | Definire verifica identità con docente/privacy prima dell'uso reale | M3/M1 | APERTO |
| R-07 | Stato conversazione in RAM si perde al riavvio (README chatbot) | Alta | Medio | Decidere se persistenza è necessaria; testare recovery | M3 | ACCETTATO per demo |
| R-08 | Dataset storico include assegnazioni incoerenti con regole live | Media | Medio | Mantenere `is_fixture`; non reinterpretare fixture come assegnazioni live | M1 | MITIGATO |
| R-09 | Requisiti approvati e date/verbali Scrum non dimostrati | Media | Medio | Raccogliere verbali e sign-off reali, senza ricostruzioni retroattive | Team | DA CONFERMARE |
| R-10 | DOCX non ancora ispezionati visivamente in Microsoft Word | Media | Medio | Eseguire checklist manuale in Word e aggiornare indice/campi prima consegna | M1 | NOT RUN |

Non rappresenta un registro ufficiale approvato. Le righe di `R-01` e `R-06` vanno
risolte prima di dichiarare idoneità a uso reale.

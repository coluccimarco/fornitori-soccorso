# Registro rischi e impedimenti

**Codice:** FOR-RSK-001 · **Versione:** 1.0 · **Data:** 03/11/2026
Scale qualitative: Alto / Medio / Basso. Owner indica il ruolo proposto, non un nome.

| ID | Rischio / evidenza | Prob. | Impatto | Mitigazione / trigger | Owner | Stato |
|---|---|---:|---:|---|---|---|
| R-01 | PDF docente e Excel originale non sono disponibili nella working tree | Alta | Alto | Acquisirli prima di approvare BUR/dataset; bloccare claims di conformità | Docente/M1 | BLOCCATO |
| R-02 | Guida API introduttiva dichiara ancora chatbot/dashboard come designed, mentre codice e metadata includono funzionalità implementate | Media | Alto | Revisione contract owner di introduzione e test stato per endpoint | M1 | APERTO |
| R-03 | UI browser non presente: demo end-to-end centrale/autista non realizzabile via UI | Alta | Alto | Confermare requisito/priorità e piano di frontend | M2/team | PIANIFICATO |
| R-04 | Routing mappe e WhatsApp non presenti, dipendono da provider e policy esterne | Alta | Medio/alto | Se richiesti, scegliere provider, costi, licenza, privacy, fallback | M4/M5 | PIANIFICATO |
| R-05 | Accesso PostgreSQL locale non verificato senza credenziali disponibili | Media | Medio | Usare ambiente autorizzato; CI effimera è verde e non sostituisce setup locale | M1 | DA CONFERMARE |
| R-06 | Ricerca cliente via targa non verifica titolarità (limite dichiarato nel README chatbot) | Alta | Alto | Definire verifica identità con docente/privacy prima dell'uso reale | M3/M1 | APERTO |
| R-07 | Stato conversazione in RAM si perde al riavvio (README chatbot) | Alta | Medio | Decidere se persistenza è necessaria; testare recovery | M3 | ACCETTATO per demo |
| R-08 | Dataset storico include assegnazioni incoerenti con regole live | Media | Medio | Mantenere `is_fixture`; non reinterpretare fixture come assegnazioni live | M1 | MITIGATO |
| R-09 | Requisiti approvati e date/verbali Scrum non dimostrati | Media | Medio | Raccogliere verbali e sign-off reali, senza ricostruzioni retroattive | Team | DA CONFERMARE |
| R-10 | DOCX non ancora ispezionati visivamente in Microsoft Word | Media | Medio | Eseguire checklist manuale in Word e aggiornare indice/campi prima consegna | M1 | NOT RUN |

Non rappresenta un registro ufficiale approvato. Le righe di `R-01` e `R-06` vanno
risolte prima di dichiarare idoneità a uso reale.

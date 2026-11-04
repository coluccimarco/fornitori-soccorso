# Registro rischi e impedimenti

**Codice:** FOR-RSK-001 · **Versione:** 2.0 · **Data:** 04/11/2026
Scale qualitative: Alto / Medio / Basso.

| ID | Rischio | Prob. | Impatto | Mitigazione | Responsabili | Stato |
|---|---|---:|---:|---|---|---|
| R-01 | PDF del docente ed Excel originale non sono nel repository | Alta | Alto | Acquisirli prima di approvare BUR e dataset | Team | Aperto |
| R-02 | Contratto OpenAPI e comportamento possono divergere | Media | Medio | Aggiornare il contratto insieme al codice | Vincenzo Minervini | Mitigato |
| R-03 | Nessuna suite automatica nel repository: una modifica può introdurre regressioni | Media | Alto | Prova manuale dei flussi prima di ogni merge e della demo | Tutti | Aperto |
| R-04 | Nominatim e OSRM pubblici possono non rispondere | Media | Medio | Errori espliciti; la chiusura missione resta possibile | Marco Colucci | Mitigato |
| R-05 | Setup locale errato: script non applicati | Media | Medio | Seguire 01..08 del README; Hibernate `validate` lo segnala all'avvio | Tutti | Mitigato |
| R-06 | La targa non prova la titolarità | Alta | Alto | Limite noto della demo: solo dati sintetici | Marco Colucci | Accettato per la demo |
| R-07 | Lo stato della conversazione in RAM si perde al riavvio | Alta | Basso | Le missioni restano nel backend; il cliente ricomincia il dialogo | Marco Colucci | Accettato per la demo |
| R-08 | Il dataset storico include assegnazioni incoerenti con le regole live | Media | Medio | `is_fixture` isola lo storico | Vincenzo Minervini | Mitigato |
| R-09 | Requisiti approvati e verbali Scrum non dimostrati | Media | Medio | Raccogliere verbali e sign-off reali | Team | Aperto |
| R-10 | DOCX non ispezionati visivamente in Microsoft Word | Media | Medio | Controllo manuale e aggiornamento dell'indice prima della consegna | Team | Aperto |
| R-11 | Messaggio duplicato se il bot si ferma tra invio ed esito | Bassa | Basso | Riserva persistente; consegna "almeno una volta" | Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi | Accettato per la demo |

`R-01` e `R-06` vanno risolti prima di dichiarare l'idoneità a un uso reale.

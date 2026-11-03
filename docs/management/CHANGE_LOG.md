# Registro modifiche documentali

| Data | ID | Modifica | Fonte / motivazione | Stato |
|---|---|---|---|---|
| 03/11/2026 | CHG-001 | Creata documentazione canonica organizzata, audit, requisiti e TAD; aggiunti indici e manuali | Stato integrato `main` e audit iniziale | Applicata nel branch `documentazione` |
| 03/11/2026 | CHG-002 | Aggiornato stato chatbot/Telegram e notifiche, distinto da UI/mappe/WhatsApp | Codice, docs modulo e CI | Applicata |
| 03/11/2026 | CHG-003 | Aggiunti generatori DOCX/workbook | Requisito di consegna modificabile | Applicata |
| 03/11/2026 | CHG-004 | PDF e Excel originali non validati | Fonti non reperite nella working tree | Aperta |
| 03/11/2026 | CHG-005 | Integrato `origin/main` a `f763d53`; risolti i conflitti README e guida M2 | Merge commit `6dbd5aa` sul branch `documentazione` | Applicata |
| 03/11/2026 | CHG-006 | Allineati manuali, API/mappe/UI, test e audit alla baseline integrata | Codice, CI run `41656606659`, esecuzioni locali | Applicata |
| 03/11/2026 | CHG-007 | Consolidati duplicati, unificati log decisioni e template Scrum, preservati report storici | Audit documentale | Applicata |
| 03/11/2026 | CHG-008 | Rigenerati DOCX, workbook e rendering Mermaid; validati link e struttura documentale | `build_docs.py`, `build_diagrams.ps1`, `validate_docs.py` | PASS |

Aggiornare questo registro quando vengono cambiati contenuti canonici, fonti o
stati; le modifiche sono tracciate in Git e non sostituiscono il changelog del codice.

# Git workflow del team

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Team Lead — Membro 1<br>
**Indice:** branch · routine · commit · PR · file condivisi · incidenti

## Branch

```text
main
feature/backend
feature/frontend
feature/chatbot
feature/maps
feature/notifications
```

`main` contiene solo incrementi integrati e verificati. Non si introduce `develop`
per il ciclo breve di quattro Sprint. Una feature branch parte da `main` aggiornato;
il branch backend esistente è da coordinare prima di pubblicarlo.

## Routine PowerShell

```powershell
git clone https://github.com/coluccimarco/fornitori-soccorso.git
git switch main
git pull --ff-only origin main
git switch -c feature/frontend
# dopo le modifiche
git status --short
.\mvnw.cmd clean verify
git diff --check
git add <file-del-task>
git commit -m "feat(frontend): add dispatch dashboard"
git push -u origin feature/frontend
```

Usa commit Conventional: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`,
`chore:`. Commit piccoli e coerenti. Non includere build output, credenziali,
database locali o input riservati del corso.

## Pull Request e review

Apri PR verso `main` con obiettivo, issue, implementazione, test PASS/FAIL/BLOCKED,
screenshots se UI, impatti API/DB e rollback. Almeno un altro membro rivede;
contratti e schema richiedono review M1 e approvazione consumer. Non fare merge
quando i controlli richiesti falliscono o sono bloccati.

## File condivisi

| File/area | Coordinamento |
|---|---|
| `pom.xml`, schema/seed/migrazioni | Membro 1 |
| `api/openapi.yaml` | Review M1 + consumer interessati |
| Frontend | Membro 2 |
| Chatbot | Membro 3 |
| Mappe | Membro 4 |
| Notifiche/webhook | Membro 5 |

Prima di risolvere conflitti, leggi entrambe le versioni e chiedi al proprietario
se cambia il contratto. Nessun `push --force`; per aggiornare branch usa merge o
rebase concordato solo su working tree pulito. Commit con trailer Copilot quando
generati con assistenza di Copilot.

**Completamento:** test e diff check passano, review approvata, CI verde,
documentazione/contratti allineati. **Riferimenti:** [Definition of Done](./11_DEFINITION_OF_DONE.md),
[issue Scrum](./08_AGILE_SCRUM.md).

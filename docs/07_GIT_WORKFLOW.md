# Git workflow del team

**Versione:** 2.0 · **Data:** 04/11/2026 · **Responsabile:** Vincenzo Minervini (Membro 1)<br>
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
per il ciclo breve di quattro Sprint. Il backend continua sul branch condiviso
`feature/backend`; gli altri componenti usano i branch dedicati.

## Routine PowerShell

```powershell
git clone https://github.com/coluccimarco/fornitori-soccorso.git
git switch main
git pull --ff-only origin main
git switch -c feature/frontend
# dopo le modifiche
git status --short
.\mvnw.cmd clean package
git diff --check
git add <file-del-task>
git commit -m "feat(frontend): add dispatch dashboard"
git push -u origin feature/frontend
```

Usa commit Conventional: `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`. Commit piccoli e coerenti. Non includere build output, credenziali,
database locali o input riservati del corso.

## Pull Request e review

Apri PR verso `main` con obiettivo, issue, implementazione, prove manuali eseguite,
screenshots se UI, impatti API/DB e rollback. Almeno un altro membro rivede;
contratti e schema richiedono la review del Membro 1 e l'approvazione dei consumer. Non fare merge
quando i controlli richiesti falliscono o sono bloccati.

## File condivisi

| File/area | Coordinamento |
|---|---|
| `pom.xml`, schema, seed e migrazioni | Vincenzo Minervini |
| `api/openapi.yaml` | Vincenzo Minervini + consumer interessati |
| Frontend | Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio |
| Chatbot | Marco Colucci |
| Mappe | Marco Colucci |
| Notifiche | Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi |

Prima di risolvere conflitti, leggi entrambe le versioni e chiedi al proprietario
se cambia il contratto. Nessun `push --force`; per aggiornare branch usa merge o
rebase concordato solo su working tree pulito.

**Completamento:** build e diff check passano, review approvata, pipeline verde,
documentazione/contratti allineati. **Riferimenti:** [Definition of Done](./11_DEFINITION_OF_DONE.md),
[registro Scrum](./agile/README.md).

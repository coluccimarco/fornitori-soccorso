# Sorgenti e rendering dei diagrammi

I file `.mmd` sono le fonti modificabili. Il modello ER è derivato dagli script `database/01..08`;
aggiornare entrambi nella stessa PR.

## Render ripetibile

Prerequisiti: Node.js/npm disponibili. Dalla root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build_diagrams.ps1
```

Il comando usa `npx --yes @mermaid-js/mermaid-cli@11.4.2` e genera `.svg` e `.png`
in `docs/diagrams/rendered/`. Il PNG è usato nel DOCX; l'SVG è l'output vettoriale
per la consultazione.

## Catalogo

| Diagramma | File |
|---|---|
| Architettura runtime sintetica | `architecture.mmd` |
| System Context | `system_context.mmd` |
| Container | `container.mmd` |
| Package/componenti | `component_packages.mmd` |
| Modello ER | `database_er.mmd` |
| Stato missione | `mission_states.mmd` |
| Creazione missione | `mission_create_sequence.mmd` |
| Assegnazione | `mission_assignment_sequence.mmd` |
| Timeout/scadenza | `mission_expiry_sequence.mmd` |
| Completamento/outbox | `mission_completion_sequence.mmd` |
| Chatbot/notifiche | `chat_notifications_sequence.mmd` |
| Sequenze storiche composite | `mission_sequence.mmd`, `chatbot_sequence.mmd` |

# Sorgente della presentazione RecuVeicol

Serve solo a rigenerare i PDF in `docs/presentazione/`, per esempio dopo la conferma del docente sulla fascia notturna.
Non fa parte dell'applicazione e non aggiunge dipendenze al progetto.

| File | Contenuto |
|---|---|
| `slides.mjs` | Testo delle slide, diagrammi e note, nell'ordine di presentazione |
| `lib.mjs` | Colori presi dal CSS del sito, stile e componenti dei diagrammi |
| `build.mjs` | Genera i due PDF e controlla note, lunghezza del testo a voce e testo fuori dai riquadri |
| `fonts/` | Inter e JetBrains Mono, con le rispettive licenze OFL |

Le immagini stanno in `../img/`. Screenshot e dialogo sono stati catturati dall'app reale su un database temporaneo, con i soli dati di `database/02_seed.sql`.
La grafica Telegram dei due file `chatbot_*_simulato.png` è simulata; i testi sono quelli prodotti dal bot.

## Rigenerare i PDF

Playwright va installato fuori dal repository, in una cartella temporanea:

```bash
cd "$(mktemp -d)" && npm init -y && npm i playwright && npx playwright install chromium
```

```bash
PLAYWRIGHT_MODULE="$PWD/node_modules/playwright/index.mjs" node <percorso-del-repo>/docs/presentazione/sorgente/build.mjs
```

Il comando termina con errore se trova problemi, elencandoli. I PDF vengono sovrascritti.

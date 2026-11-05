# Presentazione RecuVeicol per Canva

- `RecuVeicol_per_Canva.pptx`: 22 slide con testi modificabili e note del relatore.
- `video/`: 7 mini video MP4, senza audio, da 22 a 38 secondi.

Screenshot e video vengono dall'app reale, su un database temporaneo con i dati dimostrativi di `database/02_seed.sql`. Cursore e didascalie dei video sono sovrapposti in fase di registrazione e non fanno parte dell'app. La grafica Telegram del video 01 è simulata: i testi sono generati dal motore del bot con il suo backend finto, senza usare Telegram.

## Importare in Canva

1. Importare su Canva il file `RecuVeicol_per_Canva.pptx`.
2. Caricare i video della cartella `video/` in **Caricamenti**.
3. In ogni slide della tabella, trascinare il video sulla copertina.

Canva non importa le note del relatore dal PowerPoint: restano nel file `.pptx`.

| Slide | Titolo | Video |
|---|---|---|
| 9 | Il cliente scrive al bot | `01_chatbot.mp4` |
| 11 | Verifica della polizza | `02_verifica_polizza.mp4` |
| 12 | Assegnazione e tre rifiuti | `06_tre_rifiuti.mp4` |
| 13 | L'autista dal telefono | `03_autista_dal_telefono.mp4` |
| 14 | Mappa e chilometri | `07_mappa_percorso.mp4` |
| 15 | Richiesta di supporto | `04_supporto_autista_e_centrale.mp4` |
| 16 | Annullamento e costi | `05_annullamento_e_costi.mp4` |

## Da decidere

Il video 01 non mostra il messaggio di accettazione, perché contiene "Entro 45 minuti", in attesa di conferma: al suo posto c'è la scritta "Quando un autista accetta, il bot avvisa il cliente". Il bot reale invia ancora quel messaggio.

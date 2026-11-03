# Specifica tipografica Word

Fonte contenuto: Markdown sotto `docs/`; i file DOCX sono output generati.
Generatore: `python tools/build_docs.py`; dipendenze in
`tools/requirements-docs.txt`.

| Elemento | Specifica |
|---|---|
| Pagina | A4 verticale, margini 2,2 cm |
| Font | Aptos (fallback sicuro Calibri), corpo 10,5 pt |
| Titolo copertina | FORNITORI navy e titolo 22 pt |
| Heading 1/2/3 | Stili Word reali, 16/13/11 pt, keep-with-next |
| Colori | Blu navy, blu moderato, grigio chiaro, bianco |
| Tabelle | Intestazione navy con testo bianco, bordi sobri, righe non spezzate dove possibile |
| Codice | Consolas 9 pt con sfondo grigio chiaro |
| Header/footer | Titolo breve, codice/versione e campo numero pagina |
| Indice | Campo Word `TOC \o "1-3" \h \z \u`, da aggiornare in Word |
| Figure | PNG generati dai sorgenti Mermaid; proporzioni mantenute e didascalia |

## Pubblicazione

1. Modificare esclusivamente il Markdown autorevole.
2. Se si modifica un `.mmd`, eseguire `tools/build_diagrams.ps1`.
3. Eseguire `python tools/build_docs.py`.
4. Verificare che DOCX e workbook siano stati rigenerati; aprire in Word e aggiornare
   il campo TOC.
5. Selezionare alcune pagine in **Layout di stampa** per controllare titoli,
   tabelle, figure, margini e numerazione. La generazione non è una certificazione
   visuale.

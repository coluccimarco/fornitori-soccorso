# Output Microsoft Word

I documenti Word sono generati dai Markdown autorevoli; non modificarli a mano.
Per rigenerare tutti i DOCX e il workbook: dalla root
`python tools/build_docs.py`. Per controlli strutturali e link:
`python tools/validate_docs.py`. Il campo indice Word è aggiornabile all'apertura:
clic destro sull'indice → **Aggiorna campo → Aggiorna intero indice**.

Il DOCX mantiene stili Word, header/footer, numeri pagina, link, tabelle e figure
PNG se presenti nella cartella rendered. Prima della consegna ufficiale aprirli in Word per un controllo
visivo e per aggiornare l'indice.

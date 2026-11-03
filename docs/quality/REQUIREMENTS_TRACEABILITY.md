# Matrice requisiti, API e test

La matrice leggibile è in [requirements/REQUIREMENTS_TRACEABILITY.md](../requirements/REQUIREMENTS_TRACEABILITY.md).
Il workbook filtrabile `quality/registri.xlsx` è generato con
`python tools/build_registers.py` dai registri documentali e dalla baseline API/test.
Aggiornare i Markdown sorgente prima di rigenerare il workbook.

## Flusso di tracciabilità

`BR requisito → ST story di tracciabilità → componente → endpoint/schema → test → run/evidenza`

Gli ID ST sono introdotti da questo archivio e non sono ticket preesistenti.
I test per una funzione pianificata e mai implementata sono **NOT RUN**, non PASS.

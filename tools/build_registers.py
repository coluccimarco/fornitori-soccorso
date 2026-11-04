from __future__ import annotations

from pathlib import Path

import yaml
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.table import Table, TableStyleInfo


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "quality" / "registri.xlsx"


def markdown_table(path: Path) -> list[list[str]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    for index, line in enumerate(lines[:-1]):
        if not line.strip().startswith("|"):
            continue
        if not lines[index + 1].strip().startswith("|"):
            continue
        rows = []
        for candidate in lines[index :]:
            if not candidate.strip().startswith("|"):
                break
            cells = [cell.strip() for cell in candidate.strip().strip("|").split("|")]
            if all(set(cell) <= set("-: ") for cell in cells):
                continue
            rows.append(cells)
        if len(rows) > 1:
            return rows
    return []


def add_markdown_sheet(workbook: Workbook, title: str, path: Path) -> None:
    rows = markdown_table(path)
    if not rows:
        raise ValueError(f"No Markdown table found in {path}")
    sheet = workbook.create_sheet(title)
    for row in rows:
        sheet.append(row)


def style_sheet(sheet) -> None:
    sheet.freeze_panes = "A2"
    sheet.auto_filter.ref = sheet.dimensions
    for cell in sheet[1]:
        cell.fill = PatternFill("solid", fgColor="17365D")
        cell.font = Font(name="Aptos", size=10, bold=True, color="FFFFFF")
        cell.alignment = Alignment(vertical="center", wrap_text=True)
    sheet.row_dimensions[1].height = 30
    for row in sheet.iter_rows(min_row=2):
        for cell in row:
            cell.font = Font(name="Aptos", size=10, color="1F1F1F")
            cell.alignment = Alignment(vertical="top", wrap_text=True)
    for column in sheet.columns:
        letter = get_column_letter(column[0].column)
        width = min(max(max(len(str(cell.value or "")) for cell in column) + 2, 12), 52)
        sheet.column_dimensions[letter].width = width
    if sheet.max_row > 1 and sheet.max_column > 0:
        name = "T" + "".join(ch for ch in sheet.title if ch.isalnum())[:20]
        table = Table(displayName=name, ref=sheet.dimensions)
        table.tableStyleInfo = TableStyleInfo(
            name="TableStyleMedium2",
            showFirstColumn=False,
            showLastColumn=False,
            showRowStripes=True,
            showColumnStripes=False,
        )
        sheet.add_table(table)


def build() -> Path:
    workbook = Workbook()
    workbook.remove(workbook.active)
    docs = ROOT / "docs"
    # BUR begins with a stakeholder table: select requirement IDs, never the first table.
    requirements = workbook.create_sheet("Requisiti")
    requirements.append(["ID", "Requisito", "Priorita / verifica", "Stato", "Acceptance"])
    for line in (docs / "requirements" / "BUR_FORNITORI.md").read_text(encoding="utf-8").splitlines():
        if line.startswith("| BR-") or line.startswith("| NFR-"):
            cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
            requirements.append(cells + [""] * (5 - len(cells)))
    add_markdown_sheet(workbook, "Tracciabilita", docs / "requirements" / "REQUIREMENTS_TRACEABILITY.md")
    add_markdown_sheet(workbook, "Backlog", docs / "agile" / "PRODUCT_BACKLOG.md")
    add_markdown_sheet(workbook, "Rischi", docs / "management" / "RISK_REGISTER.md")
    add_markdown_sheet(workbook, "RACI", docs / "management" / "RACI_MATRIX.md")
    add_markdown_sheet(workbook, "Modifiche", docs / "management" / "CHANGE_LOG.md")

    api_sheet = workbook.create_sheet("API")
    api_sheet.append(["Metodo", "Percorso", "Stato", "Ruolo richiesto", "Sintesi"])
    spec = yaml.safe_load((ROOT / "api" / "openapi.yaml").read_text(encoding="utf-8"))
    for path, path_item in spec["paths"].items():
        for method, operation in path_item.items():
            if method.lower() not in {"get", "post", "put", "patch", "delete"}:
                continue
            api_sheet.append(
                [
                    method.upper(),
                    path,
                    operation.get("x-implementation-status", "MISSING"),
                    str(operation.get("x-required-role", "MISSING")),
                    operation.get("summary", ""),
                ]
            )

    tests_sheet = workbook.create_sheet("Test presenti")
    tests_sheet.append(["Percorso", "Linguaggio", "Suite / scenario", "Esito / limite"])
    for folder, language in ((ROOT / "src" / "test" / "java", "Java"), (ROOT / "chatbot" / "tests", "Python")):
        for test_file in sorted(folder.rglob("*Tests.java")) + sorted(folder.rglob("test_*.py")):
            tests_sheet.append(
                [
                    test_file.relative_to(ROOT).as_posix(),
                    language,
                    test_file.stem,
                    "Inventario sorgente, non esito: vedere docs/quality/TEST_EVIDENCE.md e report del run.",
                ]
            )

    for sheet in workbook.worksheets:
        style_sheet(sheet)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    workbook.save(OUTPUT)
    return OUTPUT


if __name__ == "__main__":
    print(build())

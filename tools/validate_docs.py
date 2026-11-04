from __future__ import annotations

import re
import sys
import zipfile
from pathlib import Path
from urllib.parse import unquote, urlsplit

import yaml
from docx import Document
from openpyxl import load_workbook


ROOT = Path(__file__).resolve().parents[1]
LINK = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
EXPECTED_DOCX = (
    "00_INDICE_DOCUMENTAZIONE.docx",
    "BUR_FORNITORI.docx",
    "FUD_FORNITORI.docx",
    "TAD_FORNITORI.docx",
    "PROJECT_MANAGEMENT_PLAN.docx",
    "GUIDA_TEAM.docx",
    "MANUALE_UTENTE.docx",
    "MANUALE_TECNICO.docx",
)


def markdown_files() -> list[Path]:
    return [ROOT / "README.md", *sorted((ROOT / "docs").rglob("*.md"))]


def find_broken_links() -> list[str]:
    errors = []
    for source in markdown_files():
        in_code = False
        for line_number, line in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
            if line.strip().startswith("```"):
                in_code = not in_code
                continue
            if in_code:
                continue
            for match in LINK.finditer(line):
                target = match.group(1).strip().strip("<>")
                parsed = urlsplit(target)
                if parsed.scheme or not parsed.path:
                    continue
                local_target = (source.parent / unquote(parsed.path)).resolve()
                if not local_target.exists():
                    errors.append(
                        f"{source.relative_to(ROOT).as_posix()}:{line_number}: {target}"
                    )
    return errors


def validate_api() -> tuple[int, int, int]:
    spec = yaml.safe_load((ROOT / "api" / "openapi.yaml").read_text(encoding="utf-8"))
    operations = [
        operation
        for path_item in spec["paths"].values()
        for method, operation in path_item.items()
        if method.lower() in {"get", "post", "put", "patch", "delete"}
    ]
    implemented = sum(item.get("x-implementation-status") == "implemented" for item in operations)
    designed = sum(item.get("x-implementation-status") == "designed" for item in operations)
    invalid = [
        item
        for item in operations
        if item.get("x-implementation-status") not in {"implemented", "designed"}
        or not item.get("x-required-role")
    ]
    if invalid:
        raise ValueError(f"{len(invalid)} OpenAPI operations lack status/role metadata")
    return len(operations), implemented, designed


def validate_docx() -> tuple[int, int, int]:
    folder = ROOT / "docs" / "word"
    pictures = 0
    tables = 0
    for filename in EXPECTED_DOCX:
        path = folder / filename
        if not path.is_file() or path.stat().st_size == 0:
            raise FileNotFoundError(f"Missing/empty DOCX: {path}")
        with zipfile.ZipFile(path) as archive:
            if "word/document.xml" not in archive.namelist():
                raise ValueError(f"Invalid DOCX package: {filename}")
            xml = archive.read("word/document.xml").decode("utf-8")
            package_text = "\n".join(
                archive.read(name).decode("utf-8", errors="ignore")
                for name in archive.namelist()
                if name.startswith("word/")
            )
            if "TOC" not in xml or "PAGE" not in package_text:
                raise ValueError(f"Missing TOC/page field in {filename}")
        document = Document(path)
        if not any(paragraph.style.name.startswith("Heading") for paragraph in document.paragraphs):
            raise ValueError(f"No Word heading styles found in {filename}")
        pictures += len(document.inline_shapes)
        tables += len(document.tables)
    return len(EXPECTED_DOCX), tables, pictures


def validate_workbook() -> list[str]:
    path = ROOT / "docs" / "quality" / "registri.xlsx"
    workbook = load_workbook(path, read_only=True, data_only=True)
    sheets = workbook.sheetnames
    required = {"Requisiti", "Tracciabilita", "Backlog", "API", "Rischi", "RACI", "Modifiche"}
    missing = required.difference(sheets)
    if missing:
        raise ValueError(f"Workbook missing sheets: {sorted(missing)}")
    return sheets


def validate_diagrams() -> tuple[int, int]:
    source_dir = ROOT / "docs" / "diagrams"
    sources = list(source_dir.glob("*.mmd"))
    rendered = source_dir / "rendered"
    if not sources:
        raise ValueError("No Mermaid sources found")
    for source in sources:
        for extension in ("svg", "png"):
            target = rendered / f"{source.stem}.{extension}"
            if not target.is_file() or target.stat().st_size == 0:
                raise FileNotFoundError(f"Missing diagram output: {target}")
    return len(sources), len(list(rendered.glob("*.svg"))) + len(list(rendered.glob("*.png")))


def main() -> int:
    errors = find_broken_links()
    if errors:
        print("Broken relative Markdown links:")
        print("\n".join(errors))
        return 1
    total, implemented, designed = validate_api()
    docx_count, table_count, picture_count = validate_docx()
    sheets = validate_workbook()
    diagram_sources, diagram_outputs = validate_diagrams()
    print(f"Markdown links: PASS ({len(markdown_files())} files checked)")
    print(f"OpenAPI: PASS ({total} operations: {implemented} implemented, {designed} designed)")
    print(f"DOCX: PASS ({docx_count} valid files, {table_count} tables, {picture_count} pictures)")
    print(f"Workbook: PASS ({len(sheets)} sheets: {', '.join(sheets)})")
    print(f"Mermaid: PASS ({diagram_sources} sources, {diagram_outputs} SVG/PNG outputs)")
    print("Visual Word rendering/page count: NOT RUN (not verified with a reliable renderer)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

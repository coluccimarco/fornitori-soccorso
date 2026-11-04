from __future__ import annotations

import re
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_BREAK, WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from docx.table import _Cell
from docx.text.paragraph import Paragraph

from build_registers import build as build_registers


ROOT = Path(__file__).resolve().parents[1]
BLUE = "17365D"
ACCENT = "2F75B5"
GRAY = "EAF0F6"
TEXT = "202124"

DOCUMENTS = [
    ("00_INDICE_DOCUMENTAZIONE.docx", "00_INDICE_GENERALE.md", "FOR-IDX-001", "Indice generale documentazione", "Bozza"),
    ("BUR_FORNITORI.docx", "requirements/BUR_FORNITORI.md", "FOR-BUR-001", "Business Requirements", "Da confermare con il docente"),
    ("FUD_FORNITORI.docx", "requirements/FUD_FORNITORI.md", "FOR-FUD-001", "Functional Design", "Baseline funzionale"),
    ("TAD_FORNITORI.docx", "architecture/TAD_FORNITORI.md", "FOR-TAD-001", "Technical Architecture", "Baseline tecnica"),
    ("PROJECT_MANAGEMENT_PLAN.docx", "management/PROJECT_MANAGEMENT_PLAN.md", "FOR-PMP-001", "Piano di gestione del progetto", "Baseline, approvazione da confermare"),
    ("GUIDA_TEAM.docx", "team/README.md", "FOR-TEAM-001", "Guida del team", "Baseline ruoli"),
    ("MANUALE_UTENTE.docx", "manuals/MANUALE_UTENTE.md", "FOR-USER-001", "Manuale utente", "Limitato alle funzioni disponibili"),
    ("MANUALE_TECNICO.docx", "manuals/MANUALE_TECNICO.md", "FOR-TECH-001", "Manuale tecnico", "Baseline tecnica"),
]

INLINE = re.compile(r"(\[[^\]]+\]\([^)]+\)|\*\*.+?\*\*|`[^`]+`|\*[^*]+\*)")
TABLE_SEPARATOR = re.compile(r"^\|[\s:|-]+\|$")


def set_cell_shading(cell: _Cell, fill: str) -> None:
    properties = cell._tc.get_or_add_tcPr()
    shading = OxmlElement("w:shd")
    shading.set(qn("w:fill"), fill)
    properties.append(shading)


def add_hyperlink(paragraph: Paragraph, label: str, target: str) -> None:
    relationship = paragraph.part.relate_to(
        target,
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink",
        is_external=True,
    )
    hyperlink = OxmlElement("w:hyperlink")
    hyperlink.set(qn("r:id"), relationship)
    run = OxmlElement("w:r")
    properties = OxmlElement("w:rPr")
    color = OxmlElement("w:color")
    color.set(qn("w:val"), ACCENT)
    properties.append(color)
    underline = OxmlElement("w:u")
    underline.set(qn("w:val"), "single")
    properties.append(underline)
    run.append(properties)
    text = OxmlElement("w:t")
    text.text = label
    run.append(text)
    hyperlink.append(run)
    paragraph._p.append(hyperlink)


def add_inline(paragraph: Paragraph, text: str) -> None:
    cursor = 0
    for match in INLINE.finditer(text):
        if match.start() > cursor:
            paragraph.add_run(text[cursor : match.start()])
        token = match.group(0)
        if token.startswith("["):
            parsed = re.fullmatch(r"\[([^\]]+)\]\(([^)]+)\)", token)
            if parsed:
                add_hyperlink(paragraph, parsed.group(1), parsed.group(2))
        elif token.startswith("**"):
            paragraph.add_run(token[2:-2]).bold = True
        elif token.startswith("`"):
            run = paragraph.add_run(token[1:-1])
            run.font.name = "Consolas"
            run.font.size = Pt(9)
        else:
            paragraph.add_run(token[1:-1]).italic = True
        cursor = match.end()
    if cursor < len(text):
        paragraph.add_run(text[cursor:])


def field(paragraph: Paragraph, instruction: str, fallback: str = "") -> None:
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instruction_text = OxmlElement("w:instrText")
    instruction_text.set(qn("xml:space"), "preserve")
    instruction_text.text = instruction
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    content = OxmlElement("w:t")
    content.text = fallback
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend((begin, instruction_text, separate, content, end))


def set_keep_row(row) -> None:
    row_properties = row._tr.get_or_add_trPr()
    row_properties.append(OxmlElement("w:cantSplit"))


def add_markdown_table(document: Document, lines: list[str], source_path: Path) -> None:
    rows = []
    for line in lines:
        if TABLE_SEPARATOR.match(line.replace(" ", "")):
            continue
        rows.append([cell.strip() for cell in line.strip().strip("|").split("|")])
    if not rows:
        return
    columns = max(len(row) for row in rows)
    table = document.add_table(rows=0, cols=columns)
    table.style = "Table Grid"
    table.autofit = True
    for row_index, values in enumerate(rows):
        cells = table.add_row().cells
        set_keep_row(table.rows[-1])
        for column, cell in enumerate(cells):
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP
            value = values[column] if column < len(values) else ""
            paragraph = cell.paragraphs[0]
            add_inline(paragraph, value)
            for run in paragraph.runs:
                run.font.size = Pt(8.5)
            if row_index == 0:
                set_cell_shading(cell, BLUE)
                for run in paragraph.runs:
                    run.font.color.rgb = RGBColor(255, 255, 255)
                    run.bold = True
    document.add_paragraph()


def add_cover(
    document: Document,
    title: str,
    code: str,
    status: str,
    source: str,
    responsible: str,
) -> None:
    paragraph = document.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.space_before = Pt(92)
    run = paragraph.add_run("FORNITORI")
    run.bold = True
    run.font.name = "Aptos Display"
    run.font.size = Pt(22)
    run.font.color.rgb = RGBColor.from_string(BLUE)

    paragraph = document.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.space_before = Pt(24)
    run = paragraph.add_run(title)
    run.bold = True
    run.font.name = "Aptos Display"
    run.font.size = Pt(20)
    run.font.color.rgb = RGBColor.from_string(TEXT)

    paragraph = document.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.space_before = Pt(10)
    paragraph.add_run("Project Work — ITS Developer 4.0").font.size = Pt(12)

    for label, value in (
        ("Codice", code),
        ("Versione", "1.0"),
        ("Data", "03/11/2026"),
        ("Responsabile", responsible),
        ("Stato", status),
        ("Repository", "https://github.com/coluccimarco/fornitori-soccorso"),
        ("Fonte Markdown", source),
    ):
        paragraph = document.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        paragraph.paragraph_format.space_after = Pt(3)
        run = paragraph.add_run(f"{label}: ")
        run.bold = True
        paragraph.add_run(value)
    document.add_page_break()


def set_document_defaults(document: Document, title: str, code: str) -> None:
    section = document.sections[0]
    section.page_width = Inches(8.27)
    section.page_height = Inches(11.69)
    section.top_margin = Inches(0.87)
    section.bottom_margin = Inches(0.87)
    section.left_margin = Inches(0.87)
    section.right_margin = Inches(0.87)

    styles = document.styles
    normal = styles["Normal"]
    normal.font.name = "Aptos"
    normal.font.size = Pt(10.5)
    normal.font.color.rgb = RGBColor.from_string(TEXT)
    normal.paragraph_format.space_after = Pt(6)
    for style_name, size, color in (
        ("Title", 22, BLUE),
        ("Heading 1", 16, BLUE),
        ("Heading 2", 13, ACCENT),
        ("Heading 3", 11, BLUE),
    ):
        style = styles[style_name]
        style.font.name = "Aptos Display"
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(color)
        style.paragraph_format.keep_with_next = True
        style.paragraph_format.space_before = Pt(12)
        style.paragraph_format.space_after = Pt(5)

    code_style = styles.add_style("CodeBlock", 1)
    code_style.font.name = "Consolas"
    code_style.font.size = Pt(9)
    code_style.paragraph_format.left_indent = Inches(0.15)
    code_style.paragraph_format.space_after = Pt(1)

    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    header.add_run(f"FORNITORI  |  {title}").font.size = Pt(8)
    header.runs[0].font.color.rgb = RGBColor.from_string(BLUE)
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run(f"{code}  •  v1.0     |     Pagina ")
    field(footer, "PAGE")

    settings = document.settings.element
    update_fields = OxmlElement("w:updateFields")
    update_fields.set(qn("w:val"), "true")
    settings.append(update_fields)


def add_contents_page(document: Document) -> None:
    document.add_heading("Indice", level=1)
    paragraph = document.add_paragraph()
    field(paragraph, 'TOC \\o "1-3" \\h \\z \\u', "Aprire in Word e aggiornare l'intero indice.")
    note = document.add_paragraph(
        "Se Word non aggiorna automaticamente il campo: clic destro sull'indice, "
        "Aggiorna campo → Aggiorna intero indice."
    )
    note.paragraph_format.space_after = Pt(6)
    document.add_page_break()


def markdown_to_docx(source_path: Path, output_path: Path, code: str, status: str) -> None:
    text = source_path.read_text(encoding="utf-8")
    lines = text.splitlines()
    title_match = next((re.match(r"^#\s+(.+)$", line) for line in lines if line.startswith("# ")), None)
    title = title_match.group(1) if title_match else source_path.stem.replace("_", " ")
    responsible_match = re.search(r"\*\*Responsabile:\*\*\s*([^·\r\n]+)", text)
    responsible = responsible_match.group(1).strip(" ;") if responsible_match else "Da confermare"
    document = Document()
    set_document_defaults(document, title, code)
    add_cover(
        document,
        title,
        code,
        status,
        source_path.relative_to(ROOT).as_posix(),
        responsible,
    )
    add_contents_page(document)

    index = 0
    in_code = False
    heading_numbers = [0, 0, 0]
    figure_number = 0
    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        if stripped.startswith("# "):
            index += 1
            continue
        if stripped.startswith("```"):
            in_code = not in_code
            index += 1
            continue
        if in_code:
            paragraph = document.add_paragraph(style="CodeBlock")
            paragraph.paragraph_format.keep_together = True
            paragraph.add_run(line)
            ppr = paragraph._p.get_or_add_pPr()
            shading = OxmlElement("w:shd")
            shading.set(qn("w:fill"), GRAY)
            ppr.append(shading)
            index += 1
            continue
        if stripped.startswith("|") and index + 1 < len(lines) and "|" in lines[index + 1]:
            table_lines = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index])
                index += 1
            add_markdown_table(document, table_lines, source_path)
            continue
        image_match = re.fullmatch(r"!\[([^\]]*)\]\(([^)]+)\)", stripped)
        if image_match:
            image_path = (source_path.parent / image_match.group(2)).resolve()
            if not image_path.is_file():
                raise FileNotFoundError(f"Image referenced by {source_path}: {image_path}")
            paragraph = document.add_paragraph()
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            paragraph.add_run().add_picture(str(image_path), width=Inches(6.0))
            figure_number += 1
            caption = document.add_paragraph(
                f"Figura {figure_number}. {image_match.group(1)}",
                style="Caption",
            )
            caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
            index += 1
            continue
        heading = re.match(r"^(#{2,6})\s+(.*)$", stripped)
        if heading:
            level = min(len(heading.group(1)) - 1, 3)
            heading_numbers[level - 1] += 1
            for deeper in range(level, len(heading_numbers)):
                heading_numbers[deeper] = 0
            prefix = ".".join(str(number) for number in heading_numbers[:level])
            document.add_heading(f"{prefix} {heading.group(2)}", level=level)
            index += 1
            continue
        if not stripped or stripped in {"---", "***"}:
            index += 1
            continue
        if stripped.startswith(">"):
            paragraph = document.add_paragraph(style="Quote")
            add_inline(paragraph, stripped.lstrip("> ").strip())
            index += 1
            continue
        bullet = re.match(r"^[-*]\s+(.*)$", stripped)
        if bullet:
            paragraph = document.add_paragraph(style="List Bullet")
            add_inline(paragraph, bullet.group(1))
            index += 1
            continue
        number = re.match(r"^\d+\.\s+(.*)$", stripped)
        if number:
            paragraph = document.add_paragraph(style="List Number")
            add_inline(paragraph, number.group(1))
            index += 1
            continue
        paragraph = document.add_paragraph()
        add_inline(paragraph, stripped)
        index += 1

    output_path.parent.mkdir(parents=True, exist_ok=True)
    document.save(output_path)


def main() -> None:
    output_dir = ROOT / "docs" / "word"
    for filename, source, code, title, status in DOCUMENTS:
        markdown_to_docx(ROOT / "docs" / source, output_dir / filename, code, status)
        print(f"Generated {filename}")
    register = build_registers()
    print(f"Generated {register.relative_to(ROOT)}")


if __name__ == "__main__":
    main()

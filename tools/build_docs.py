from __future__ import annotations

import re
from copy import deepcopy
from pathlib import Path
from urllib.parse import urlsplit

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_BREAK, WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor
from docx.table import _Cell
from docx.text.paragraph import Paragraph

from build_registers import build as build_registers


ROOT = Path(__file__).resolve().parents[1]
BLUE = "17365D"
ACCENT = "2F75B5"
GRAY = "EAF0F6"
TEXT = "202124"
REPOSITORY = "https://github.com/coluccimarco/fornitori-soccorso"
USABLE_WIDTH_CM = 16.5
CHAR_CM = 0.16
CELL_PADDING_CM = 0.45

DOCUMENTS = [
    ("00_INDICE_DOCUMENTAZIONE.docx", "00_INDICE_GENERALE.md", "FOR-IDX-001", "Indice generale documentazione", "Baseline documentale"),
    ("BUR_FORNITORI.docx", "requirements/BUR_FORNITORI.md", "FOR-BUR-001", "Business Requirements", "Da confermare con il docente"),
    ("FUD_FORNITORI.docx", "requirements/FUD_FORNITORI.md", "FOR-FUD-001", "Functional Design", "Baseline funzionale"),
    ("TAD_FORNITORI.docx", "architecture/TAD_FORNITORI.md", "FOR-TAD-001", "Technical Architecture", "Baseline tecnica"),
    ("PROJECT_MANAGEMENT_PLAN.docx", "management/PROJECT_MANAGEMENT_PLAN.md", "FOR-PMP-001", "Piano di gestione del progetto", "Baseline, approvazione da confermare"),
    ("GUIDA_TEAM.docx", "team/README.md", "FOR-TEAM-001", "Guida del team", "Baseline ruoli"),
    ("MANUALE_UTENTE.docx", "manuals/MANUALE_UTENTE.md", "FOR-USER-001", "Manuale utente", "Baseline"),
    ("MANUALE_TECNICO.docx", "manuals/MANUALE_TECNICO.md", "FOR-TECH-001", "Manuale tecnico", "Baseline tecnica"),
]

INLINE = re.compile(r"(\[[^\]]+\]\([^)]+\)|\*\*.+?\*\*|`[^`]+`|\*[^*]+\*)")
TABLE_SEPARATOR = re.compile(r"^\|[\s:|-]+\|$")


def shading_element(fill: str):
    shading = OxmlElement("w:shd")
    shading.set(qn("w:val"), "clear")
    shading.set(qn("w:color"), "auto")
    shading.set(qn("w:fill"), fill)
    return shading


def set_cell_shading(cell: _Cell, fill: str) -> None:
    properties = cell._tc.get_or_add_tcPr()
    alignment = properties.find(qn("w:vAlign"))
    if alignment is not None:
        alignment.addprevious(shading_element(fill))
    else:
        properties.append(shading_element(fill))


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


def link_target(target: str, source_path: Path) -> str:
    """Un link relativo non funziona dal DOCX, che sta in un'altra cartella: punta al file su GitHub."""
    if urlsplit(target).scheme:
        return target
    path, _, anchor = target.partition("#")
    resolved = (source_path.parent / path).resolve().relative_to(ROOT).as_posix()
    return f"{REPOSITORY}/blob/main/{resolved}" + (f"#{anchor}" if anchor else "")


def add_inline(paragraph: Paragraph, text: str, source_path: Path) -> None:
    cursor = 0
    for match in INLINE.finditer(text):
        if match.start() > cursor:
            paragraph.add_run(text[cursor : match.start()])
        token = match.group(0)
        if token.startswith("["):
            parsed = re.fullmatch(r"\[([^\]]+)\]\(([^)]+)\)", token)
            if parsed:
                add_hyperlink(paragraph, parsed.group(1).strip("`"), link_target(parsed.group(2), source_path))
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


def breakable_code(value: str) -> str:
    """Nomi di file e percorsi nelle celle possono andare a capo dopo / _ . -, invece di uscire dalla tabella."""
    return re.sub(r"`[^`]+`", lambda code: re.sub(r"([/_.\-])", "\\1\u200b", code.group(0)), value)


def column_widths(rows: list[list[str]], columns: int) -> list[float]:
    """Larghezze in cm: ogni colonna contiene la parola più lunga, il resto va a chi ha più testo."""
    plain = [[re.sub(r"[`*]", "", row[column]) if column < len(row) else "" for row in rows] for column in range(columns)]
    minimum = [
        min(CHAR_CM * max(len(word) for cell in cells for word in re.split(r"[\s\u200b]+", cell)) + CELL_PADDING_CM, 5.0)
        for cells in plain
    ]
    scale = min(1.0, USABLE_WIDTH_CM / sum(minimum))
    minimum = [width * scale for width in minimum]
    wanted = [max(len(cell) for cell in cells) ** 0.5 for cells in plain]
    spare = USABLE_WIDTH_CM - sum(minimum)
    return [width + spare * share / sum(wanted) for width, share in zip(minimum, wanted)]


def add_markdown_table(document: Document, lines: list[str], source_path: Path) -> None:
    rows = []
    for line in lines:
        if TABLE_SEPARATOR.match(line.replace(" ", "")):
            continue
        rows.append([breakable_code(cell.strip()) for cell in line.strip().strip("|").split("|")])
    if not rows:
        return
    columns = max(len(row) for row in rows)
    widths = column_widths(rows, columns)
    table = document.add_table(rows=0, cols=columns)
    table.style = "Table Grid"
    table.autofit = False
    for column, width in enumerate(widths):
        table.columns[column].width = Cm(width)
    for row_index, values in enumerate(rows):
        cells = table.add_row().cells
        set_keep_row(table.rows[-1])
        if row_index == 0:
            table.rows[-1]._tr.get_or_add_trPr().append(OxmlElement("w:tblHeader"))
        for column, cell in enumerate(cells):
            cell.width = Cm(widths[column])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP
            value = values[column] if column < len(values) else ""
            paragraph = cell.paragraphs[0]
            add_inline(paragraph, value, source_path)
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
    version: str,
    date: str,
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
        ("Versione", version),
        ("Data", date),
        ("Responsabile", responsible),
        ("Stato", status),
        ("Repository", REPOSITORY),
        ("Fonte Markdown", source),
    ):
        paragraph = document.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        paragraph.paragraph_format.space_after = Pt(3)
        run = paragraph.add_run(f"{label}: ")
        run.bold = True
        paragraph.add_run(value)
    document.add_page_break()


def set_document_defaults(document: Document, title: str, code: str, version: str) -> None:
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
        ("TOC Heading", 16, BLUE),
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
    footer.add_run(f"{code}  •  v{version}     |     Pagina ")
    field(footer, "PAGE")

    settings = document.settings.element
    update_fields = OxmlElement("w:updateFields")
    update_fields.set(qn("w:val"), "true")
    # Lo schema fissa l'ordine degli elementi: updateFields va prima di questi.
    following = ("hdrShapeDefaults", "footnotePr", "endnotePr", "compat", "docVars", "rsids")
    anchor = next((child for child in settings if child.tag in {qn(f"w:{name}") for name in following}), None)
    if anchor is not None:
        anchor.addprevious(update_fields)
    else:
        settings.append(update_fields)
    zoom = settings.find(qn("w:zoom"))
    if zoom is not None:
        zoom.set(qn("w:percent"), "100")


def add_field_char(paragraph: Paragraph, kind: str) -> None:
    char = OxmlElement("w:fldChar")
    char.set(qn("w:fldCharType"), kind)
    paragraph.add_run()._r.append(char)


def add_contents_page(document: Document, headings: list[tuple[int, str]]) -> None:
    """L'indice nasce già con i titoli: Word aggiunge i numeri di pagina quando aggiorna il campo."""
    title = document.add_paragraph("Indice", style="TOC Heading")
    title.paragraph_format.space_after = Pt(8)
    entries = []
    for level, label in headings:
        entry = document.add_paragraph()
        entry.paragraph_format.left_indent = Inches(0.2 * (level - 1))
        entry.paragraph_format.space_after = Pt(3)
        entries.append((entry, label))
    first = entries[0][0]
    add_field_char(first, "begin")
    instruction = OxmlElement("w:instrText")
    instruction.set(qn("xml:space"), "preserve")
    instruction.text = 'TOC \\o "1-3" \\h \\z \\u'
    first.add_run()._r.append(instruction)
    add_field_char(first, "separate")
    for entry, label in entries:
        entry.add_run(label)
    add_field_char(entries[-1][0], "end")
    document.add_page_break()


def document_metadata(text: str, source_path: Path) -> tuple[str, str, str]:
    """Copertina e piè di pagina riportano i dati del documento: se mancano, meglio fermarsi che inventarli."""
    values = []
    for pattern in (r"\*\*Versione:\*\*\s*([^·\r\n<]+)", r"\*\*Data:\*\*\s*([^·\r\n<]+)", r"\*\*Responsabil[ei]:\*\*\s*([^·\r\n<]+)"):
        match = re.search(pattern, text)
        if not match:
            raise ValueError(f"{source_path}: metadato mancante, atteso {pattern}")
        values.append(match.group(1).strip(" ;"))
    return values[0], values[1], values[2]


def numbered_headings(lines: list[str]) -> dict[int, tuple[int, str]]:
    """Titoli per riga. I titoli che hanno già un numero o un codice (UC-001) non vengono rinumerati."""
    use_numbers = not any(re.match(r"^#{2,6}\s+UC-\d+", line.strip()) for line in lines)
    numbers = [0, 0, 0]
    headings = {}
    in_code = False
    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("```"):
            in_code = not in_code
            continue
        heading = re.match(r"^(#{2,6})\s+(.*)$", stripped)
        if in_code or not heading:
            continue
        level = min(len(heading.group(1)) - 1, 3)
        numbers[level - 1] += 1
        numbers[level:] = [0] * (len(numbers) - level)
        label = re.sub(r"^\d+(\.\d+)*\.?\s+", "", heading.group(2))
        prefix = ".".join(str(number) for number in numbers[:level])
        headings[index] = (level, f"{prefix} {label}" if use_numbers else label)
    return headings


def starts_block(stripped: str) -> bool:
    return (
        not stripped
        or stripped in {"---", "***"}
        or stripped.startswith(("#", "```", "|", ">", "!["))
        or re.match(r"^([-*]|\d+\.)\s+", stripped) is not None
    )


def read_wrapped(lines: list[str], index: int, first: str, is_list: bool) -> tuple[str, int]:
    """Riunisce le righe a capo del sorgente. Una riga che apre con **Etichetta:** è un paragrafo a sé."""
    parts = [first]
    index += 1
    while index < len(lines):
        stripped = lines[index].strip()
        if starts_block(stripped) or (not is_list and stripped.startswith("**")):
            break
        parts.append(stripped)
        index += 1
    return " ".join(part.removesuffix("<br>").strip() for part in parts), index


def new_numbered_list(document: Document) -> int:
    """Lo stile List Number è unico nel documento: ogni elenco ha bisogno di una numerazione che riparte da 1."""
    numbering = document.part.numbering_part.element
    style_num_id = document.styles["List Number"].element.pPr.numPr.numId.val
    abstract_id = next(num.abstractNumId.val for num in numbering.num_lst if num.numId == style_num_id)
    abstracts = numbering.findall(qn("w:abstractNum"))
    template = next(item for item in abstracts if item.get(qn("w:abstractNumId")) == str(abstract_id))
    # Una definizione propria per ogni elenco, senza legame con lo stile: resta valida anche fuori da Word.
    clone = deepcopy(template)
    new_id = max(int(item.get(qn("w:abstractNumId"))) for item in abstracts) + 1
    clone.set(qn("w:abstractNumId"), str(new_id))
    clone.find(qn("w:nsid")).set(qn("w:val"), f"{new_id:08X}")
    for link in clone.findall(f"{qn('w:lvl')}/{qn('w:pStyle')}"):
        link.getparent().remove(link)
    abstracts[-1].addnext(clone)
    number = numbering.add_num(new_id)
    number.add_lvlOverride(ilvl=0).add_startOverride(1)
    return number.numId


def markdown_to_docx(source_path: Path, output_path: Path, code: str, status: str) -> None:
    text = source_path.read_text(encoding="utf-8")
    lines = text.splitlines()
    title_match = next((re.match(r"^#\s+(.+)$", line) for line in lines if line.startswith("# ")), None)
    title = title_match.group(1) if title_match else source_path.stem.replace("_", " ")
    version, date, responsible = document_metadata(text, source_path)
    headings = numbered_headings(lines)
    document = Document()
    set_document_defaults(document, title, code, version)
    add_cover(
        document,
        title,
        code,
        status,
        source_path.relative_to(ROOT).as_posix(),
        responsible,
        version,
        date,
    )
    add_contents_page(document, list(headings.values()))

    index = 0
    in_code = False
    figure_number = 0
    list_num_id = None
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
            ppr.append(shading_element(GRAY))
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
        if index in headings:
            level, label = headings[index]
            document.add_heading(label, level=level)
            list_num_id = None
            index += 1
            continue
        if not stripped or stripped in {"---", "***"}:
            index += 1
            continue
        if stripped.startswith(">"):
            paragraph = document.add_paragraph(style="Quote")
            content, index = read_wrapped(lines, index, stripped.lstrip("> ").strip(), is_list=False)
            add_inline(paragraph, content, source_path)
            continue
        bullet = re.match(r"^[-*]\s+(.*)$", stripped)
        if bullet:
            paragraph = document.add_paragraph(style="List Bullet")
            content, index = read_wrapped(lines, index, bullet.group(1), is_list=True)
            add_inline(paragraph, content, source_path)
            continue
        number = re.match(r"^\d+\.\s+(.*)$", stripped)
        if number:
            if list_num_id is None:
                list_num_id = new_numbered_list(document)
            paragraph = document.add_paragraph(style="List Number")
            numbering = paragraph._p.get_or_add_pPr().get_or_add_numPr()
            numbering.get_or_add_ilvl().val = 0
            numbering.get_or_add_numId().val = list_num_id
            content, index = read_wrapped(lines, index, number.group(1), is_list=True)
            add_inline(paragraph, content, source_path)
            continue
        list_num_id = None
        paragraph = document.add_paragraph()
        content, index = read_wrapped(lines, index, stripped, is_list=False)
        add_inline(paragraph, content, source_path)

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

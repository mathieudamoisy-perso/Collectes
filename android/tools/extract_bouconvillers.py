"""Extract CCVT Bouconvillers 2026 collection dates from rendered PDF pixels."""
from __future__ import annotations

import calendar
from dataclasses import dataclass
from datetime import date
from pathlib import Path

import fitz
from PIL import Image

PDF = Path(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
YEAR = 2026
SCALE = 4

# Month block top-left corners on rendered page (measured from page.png at scale=3, scaled to SCALE)
# Format: month -> (x0, y0, cell_w, cell_h, header_h, weekday_h)
# Derived from PDF coordinates * SCALE/3 * (1259/419) etc - calibrate from page
MONTH_LAYOUT = [
    # row 1
    (1, 28, 318, 44, 14, 18),
    (2, 210, 318, 44, 14, 18),
    (3, 392, 318, 44, 14, 18),
    # row 2
    (4, 28, 428, 44, 14, 18),
    (5, 210, 428, 44, 14, 18),
    (6, 392, 428, 44, 14, 18),
    # row 3
    (7, 28, 538, 44, 14, 18),
    (8, 210, 538, 44, 14, 18),
    (9, 392, 538, 44, 14, 18),
    # row 4
    (10, 28, 648, 44, 14, 18),
    (11, 210, 648, 44, 14, 18),
    (12, 392, 648, 44, 14, 18),
]


@dataclass
class CellMark:
    jaune: bool = False
    gris: bool = False
    verre: bool = False
    encombrants: bool = False


def classify_pixel(r: int, g: int, b: int) -> str | None:
    # Legend colors (tolerant thresholds)
    if r > 210 and g > 150 and b < 80:  # yellow / orange
        return "jaune"
    if r < 90 and g < 90 and b < 90:  # dark grey bar
        return "gris"
    if r < 120 and g > 150 and b < 120:  # green
        return "verre"
    if r > 180 and g < 120 and b > 120:  # pink/magenta outline fill
        return "encombrants"
    if r > 160 and g < 100 and b > 100 and g + b > r:  # pink
        return "encombrants"
    return None


def sample_cell(img: Image.Image, x0: int, y0: int, w: int, h: int) -> CellMark:
    mark = CellMark()
    # sample lower half of cell where bars appear
    sx0 = x0 + w // 8
    sx1 = x0 + w - w // 8
    sy0 = y0 + h // 2
    sy1 = y0 + h - 2
    counts: dict[str, int] = {}
    for y in range(sy0, sy1, 2):
        for x in range(sx0, sx1, 2):
            r, g, b = img.getpixel((x, y))[:3]
            kind = classify_pixel(r, g, b)
            if kind:
                counts[kind] = counts.get(kind, 0) + 1
    if counts.get("jaune", 0) >= 3:
        mark.jaune = True
    if counts.get("gris", 0) >= 3:
        mark.gris = True
    if counts.get("verre", 0) >= 3:
        mark.verre = True
    if counts.get("encombrants", 0) >= 8:
        mark.encombrants = True
    return mark


def calibrate_layout(page_w: int, page_h: int) -> list[tuple[int, int, int, int, int, int, int]]:
    """Return (month, x0, y0, block_w, block_h, weekday_y_offset, row_h)."""
    # Auto-calibrate from page dimensions (A4-like portrait)
    col_w = page_w / 3.05
    row_h = (page_h - 320) / 4.2
    layouts = []
    for row in range(4):
        for col in range(3):
            month = row * 3 + col + 1
            x0 = int(18 + col * col_w)
            y0 = int(300 + row * row_h)
            layouts.append((month, x0, y0, int(col_w - 8), int(row_h - 10), 52, int((row_h - 70) / 6)))
    return layouts


def extract(pdf_path: Path) -> dict[date, set[str]]:
    doc = fitz.open(pdf_path)
    page = doc[0]
    pix = page.get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
    img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

    layouts = calibrate_layout(pix.width, pix.height)
    results: dict[date, set[str]] = {}

    for month, x0, y0, block_w, block_h, weekday_off, row_h in layouts:
        cell_w = block_w // 7
        weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)
        grid_y = y0 + weekday_off
        for wi, week in enumerate(weeks):
            for di, dom in enumerate(week):
                if dom == 0:
                    continue
                cx = x0 + di * cell_w
                cy = grid_y + wi * row_h
                mark = sample_cell(img, cx, cy, cell_w, row_h)
                d = date(YEAR, month, dom)
                types: set[str] = set()
                if mark.jaune:
                    types.add("emballages")
                if mark.gris:
                    types.add("ordures")
                if mark.verre:
                    types.add("verre")
                if mark.encombrants:
                    types.add("encombrants")
                if types:
                    results[d] = types
    return results


def main() -> None:
    schedule = extract(PDF)
    checks = [
        date(2026, 9, 9),
        date(2026, 10, 7),
        date(2026, 10, 8),
    ]
    print("=== Verification ===")
    for d in checks:
        print(d.isoformat(), "->", sorted(schedule.get(d, set())))

    print("\n=== September 2026 ===")
    for d in sorted(schedule):
        if d.month == 9:
            print(d.isoformat(), sorted(schedule[d]))

    print("\n=== October 2026 ===")
    for d in sorted(schedule):
        if d.month == 10:
            print(d.isoformat(), sorted(schedule[d]))

    print("\nTotal collection days:", len(schedule))


if __name__ == "__main__":
    main()

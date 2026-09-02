"""Accurate CCVT calendar extraction via per-cell visual detection."""
from __future__ import annotations

import calendar
from datetime import date
from pathlib import Path

import fitz
from PIL import Image

PDF = Path(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
YEAR = 2026
SCALE = 4

MONTHS = [
    (1, 12, 132, 246, 330),
    (2, 148, 268, 246, 330),
    (3, 284, 404, 246, 330),
    (4, 12, 132, 329, 413),
    (5, 148, 268, 329, 413),
    (6, 284, 404, 329, 413),
    (7, 12, 132, 406, 490),
    (8, 148, 268, 406, 490),
    (9, 284, 404, 406, 490),
    (10, 12, 132, 493, 577),
    (11, 148, 268, 493, 577),
    (12, 284, 404, 493, 577),
]


def cell_marks(img: Image.Image, x0: int, y0: int, x1: int, y1: int) -> set[str]:
    marks: set[str] = set()
    w, h = x1 - x0, y1 - y0
    if w < 8 or h < 8:
        return marks

    mid_x = (x0 + x1) // 2
    bar_top = y0 + h // 3
    bar_bottom = y1 - h // 8

    yellow = gray = green = pink = 0
    samples = 0
    for y in range(bar_top, bar_bottom, 2):
        for dx in (-w // 8, 0, w // 8):
            x = mid_x + dx
            if x0 + 2 <= x < x1 - 2:
                r, g, b = img.getpixel((x, y))[:3]
                samples += 1
                if r > 210 and g > 150 and b < 100:
                    yellow += 1
                if r < 80 and g < 80 and b < 80:
                    gray += 1
                if r < 120 and g > 150 and b < 120:
                    green += 1

    # Border scan for encombrants / verre outline
    border_green = border_pink = 0
    for x in range(x0 + 2, x1 - 2, 2):
        for y in (y0 + 3, y1 - 4):
            r, g, b = img.getpixel((x, y))[:3]
            if r < 120 and g > 150 and b < 120:
                border_green += 1
            if r > 170 and g < 120 and b > 110:
                border_pink += 1
    for y in range(y0 + 3, y1 - 3, 2):
        for x in (x0 + 3, x1 - 4):
            r, g, b = img.getpixel((x, y))[:3]
            if r < 120 and g > 150 and b < 120:
                border_green += 1
            if r > 170 and g < 120 and b > 110:
                border_pink += 1

    if yellow >= 3:
        marks.add("emballages")
    if gray >= 3:
        marks.add("ordures")
    if green >= 3 or border_green >= 6:
        marks.add("verre")
    if border_pink >= 6:
        marks.add("encombrants")

    # CCVT pairs jaune+gris on same day
    if "emballages" in marks and "ordures" not in marks and gray >= 1:
        marks.add("ordures")
    return marks


def extract() -> dict[date, list[str]]:
    doc = fitz.open(PDF)
    page = doc[0]
    pix = page.get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
    img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

    schedule: dict[date, set[str]] = {}
    for month, x_min, x_max, y_start, y_end in MONTHS:
        sx0 = int(x_min * SCALE)
        sy0 = int(y_start * SCALE)
        block_w = int((x_max - x_min) * SCALE)
        block_h = int((y_end - y_start) * SCALE)
        col_w = block_w // 7
        row_h = block_h // 6
        weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)

        for row, week in enumerate(weeks):
            for col, dom in enumerate(week):
                if dom == 0:
                    continue
                x0 = sx0 + col * col_w
                y0 = sy0 + row * row_h
                x1 = x0 + col_w
                y1 = y0 + row_h
                marks = cell_marks(img, x0, y0, x1, y1)
                if marks:
                    schedule[date(YEAR, month, dom)] = marks

    return {d: sorted(v) for d, v in sorted(schedule.items())}


def main() -> None:
    schedule = extract()
    checks = [
        date(2026, 9, 9),
        date(2026, 10, 7),
        date(2026, 10, 8),
        date(2026, 9, 2),
        date(2026, 10, 14),
    ]
    print("=== Verification ===")
    for d in checks:
        print(d.isoformat(), "->", schedule.get(d, []))

    print("\n=== September ===")
    for d, types in schedule.items():
        if d.month == 9:
            print(d.isoformat(), types)

    print("\n=== October ===")
    for d, types in schedule.items():
        if d.month == 10:
            print(d.isoformat(), types)

    print("\nTotal:", len(schedule))


if __name__ == "__main__":
    main()

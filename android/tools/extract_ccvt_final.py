"""Final CCVT schedule extraction for Kotlin implementation reference."""
from __future__ import annotations

import calendar
import json
from datetime import date
from pathlib import Path

import fitz
from PIL import Image

PDF = Path(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
OUT = Path(__file__).with_name("bouconvillers_2026.json")
YEAR = 2026
SCALE = 6

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

YELLOW_RGB = (255, 240, 0)
MAGENTA_STROKE = (0.925, 0.0, 0.545)
GREEN_VERRE = (80, 188, 145)


def dom_at(month: int, x_min: float, x_max: float, y_start: float, y_end: float, cx: float, cy: float):
    col_w = (x_max - x_min) / 7
    row_h = (y_end - y_start) / 6
    col = max(0, min(6, int((cx - x_min) / col_w)))
    row = max(0, min(5, int((cy - y_start) / row_h)))
    weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)
    if row >= len(weeks):
        return None
    dom = weeks[row][col]
    return dom or None


def yellow_pixels(img, x0, y0, x1, y1) -> int:
    count = 0
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b = img.getpixel((x, y))[:3]
            if r >= 240 and g >= 200 and b <= 60:
                count += 1
    return count


def green_pixels(img, x0, y0, x1, y1) -> int:
    count = 0
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b = img.getpixel((x, y))[:3]
            if g >= 170 and r <= 120 and b <= 140:
                count += 1
    return count


def extract() -> dict[date, set[str]]:
    doc = fitz.open(PDF)
    page = doc[0]
    pix = page.get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
    img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)
    schedule: dict[date, set[str]] = {}

    def add(d: date, kind: str) -> None:
        schedule.setdefault(d, set()).add(kind)

    # Raster: jaune/gris + verre
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
                d = date(YEAR, month, dom)
                if yellow_pixels(img, x0, y0, x1, y1) >= 1500:
                    add(d, "emballages")
                    add(d, "ordures")
                elif green_pixels(img, x0, y0, x1, y1) >= 2500:
                    add(d, "verre")

    # Vectors: encombrants outlines
    for drawing in page.get_drawings():
        rect = drawing["rect"]
        cx = (rect.x0 + rect.x1) / 2
        cy = (rect.y0 + rect.y1) / 2
        stroke = drawing.get("color")
        fill = drawing.get("fill")
        is_magenta = stroke and all(abs(stroke[i] - MAGENTA_STROKE[i]) <= 0.05 for i in range(3))
        is_black_box = (
            stroke == (0.0, 0.0, 0.0)
            and fill is None
            and 8 <= rect.width <= 12
            and 8 <= rect.height <= 12
        )
        if not is_magenta and not is_black_box:
            continue
        for month, x_min, x_max, y_start, y_end in MONTHS:
            if not (x_min - 5 <= cx <= x_max + 5 and y_start - 5 <= cy <= y_end + 5):
                continue
            dom = dom_at(month, x_min, x_max, y_start, y_end, cx, cy)
            if dom:
                add(date(YEAR, month, dom), "encombrants")
            break

    return schedule


def main() -> None:
    schedule = extract()
    checks = [date(2026, 9, 9), date(2026, 10, 7), date(2026, 10, 8), date(2026, 10, 1)]
    print("=== Verification ===")
    for d in checks:
        print(d.isoformat(), "->", sorted(schedule.get(d, set())))

    print("\n=== All dates ===")
    for d in sorted(schedule):
        print(d.isoformat(), sorted(schedule[d]))

    OUT.write_text(
        json.dumps({d.isoformat(): sorted(v) for d, v in sorted(schedule.items())}, indent=2),
        encoding="utf-8",
    )
    print(f"\nWrote {OUT} ({len(schedule)} days)")


if __name__ == "__main__":
    main()

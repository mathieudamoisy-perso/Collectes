"""Generate Bouconvillers 2026 schedule from PDF visuals (ground truth)."""
from __future__ import annotations

import calendar
import json
from datetime import date
from pathlib import Path

import fitz
from PIL import Image

PDF = Path(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
YEAR = 2026
SCALE = 4

# PDF coordinates (y from top), validated against Sept 9 / Oct 7 yellow bars
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

YELLOW = (1.0, 0.945, 0.0)
MAGENTA = (0.925, 0.0, 0.545)
TEXT_GRAY = (0.133, 0.122, 0.122)
GREEN = (0.247, 0.678, 0.282)


def rgb_close(a, b, tol=0.08) -> bool:
    return all(abs(x - y) <= tol for x, y in zip(a[:3], b[:3]))


def classify_pixel(r: int, g: int, b: int) -> str | None:
    if r > 210 and g > 140 and b < 90:
        return "jaune"
    if r < 70 and g < 70 and b < 70:
        return "gris"
    if r < 110 and g > 150 and b < 110:
        return "verre"
    if r > 170 and g < 110 and b > 110:
        return "encombrants"
    return None


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


def extract_vectors(page) -> dict[date, set[str]]:
    schedule: dict[date, set[str]] = {}

    def add(d: date, kind: str) -> None:
        schedule.setdefault(d, set()).add(kind)

    for drawing in page.get_drawings():
        rect = drawing["rect"]
        cx = (rect.x0 + rect.x1) / 2
        cy = (rect.y0 + rect.y1) / 2
        fill = drawing.get("fill")
        stroke = drawing.get("color")

        for month, x_min, x_max, y_start, y_end in MONTHS:
            if not (x_min <= cx <= x_max and y_start <= cy <= y_end):
                continue
            dom = dom_at(month, x_min, x_max, y_start, y_end, cx, cy)
            if not dom:
                break

            if fill and rgb_close(fill, YELLOW, 0.02) and rect.height >= 20:
                add(date(YEAR, month, dom), "emballages")
                add(date(YEAR, month, dom), "ordures")
            elif (
                stroke
                and rgb_close(stroke, MAGENTA, 0.05)
                and 7 <= rect.width <= 14
                and 7 <= rect.height <= 14
            ):
                add(date(YEAR, month, dom), "encombrants")
            elif (
                stroke == (0.0, 0.0, 0.0)
                and drawing.get("fill") is None
                and 8 <= rect.width <= 12
                and 8 <= rect.height <= 12
            ):
                add(date(YEAR, month, dom), "encombrants")
            elif fill and rgb_close(fill, GREEN, 0.06) and rect.height >= 15:
                add(date(YEAR, month, dom), "verre")
            break

    return schedule


def extract_pixels(img: Image.Image, scale: float) -> dict[date, set[str]]:
    schedule: dict[date, set[str]] = {}

    def add(d: date, kind: str) -> None:
        schedule.setdefault(d, set()).add(kind)

    for month, x_min, x_max, y_start, y_end in MONTHS:
        sx0 = int(x_min * scale)
        sy0 = int(y_start * scale)
        block_w = int((x_max - x_min) * scale)
        block_h = int((y_end - y_start) * scale)
        col_w = block_w // 7
        row_h = block_h // 6
        weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)

        for row, week in enumerate(weeks):
            for col, dom in enumerate(week):
                if dom == 0:
                    continue
                x0 = sx0 + col * col_w
                y0 = sy0 + row * row_h + int(row_h * 0.45)
                x1 = x0 + col_w
                y1 = sy0 + (row + 1) * row_h - 2
                counts: dict[str, int] = {}
                outline = 0
                for y in range(y0, y1, 2):
                    for x in range(x0 + 2, x1 - 2, 2):
                        r, g, b = img.getpixel((x, y))[:3]
                        kind = classify_pixel(r, g, b)
                        if kind:
                            counts[kind] = counts.get(kind, 0) + 1
                        if r > 170 and g < 110 and b > 110:
                            outline += 1
                d = date(YEAR, month, dom)
                if counts.get("jaune", 0) >= 2:
                    add(d, "emballages")
                if counts.get("gris", 0) >= 2:
                    add(d, "ordures")
                if counts.get("verre", 0) >= 2:
                    add(d, "verre")
                if outline >= 8:
                    add(d, "encombrants")
    return schedule


def merge(*schedules: dict[date, set[str]]) -> dict[date, list[str]]:
    out: dict[date, set[str]] = {}
    for sched in schedules:
        for d, kinds in sched.items():
            out.setdefault(d, set()).update(kinds)
    return {d: sorted(v) for d, v in sorted(out.items())}


def main() -> None:
    doc = fitz.open(PDF)
    page = doc[0]
    pix = page.get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
    img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

    schedule = merge(extract_vectors(page), extract_pixels(img, SCALE))

    checks = [
        date(2026, 9, 9),
        date(2026, 10, 7),
        date(2026, 10, 8),
        date(2026, 10, 1),
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

    out = Path(__file__).with_name("bouconvillers_2026.json")
    serializable = {d.isoformat(): types for d, types in schedule.items()}
    out.write_text(json.dumps(serializable, indent=2), encoding="utf-8")
    print(f"\nWrote {out} ({len(schedule)} days)")


if __name__ == "__main__":
    main()

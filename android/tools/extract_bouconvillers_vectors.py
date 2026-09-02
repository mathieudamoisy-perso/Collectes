"""Extract CCVT calendar dates from PDF vector bars (final)."""
from __future__ import annotations

import calendar
import json
from collections import defaultdict
from datetime import date
from pathlib import Path

import fitz

PDF = Path(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
YEAR = 2026

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

TEXT_GRAY = (0.13300000131130219, 0.12200000137090683, 0.12200000137090683)
YELLOW = (1.0, 0.9449999928474426, 0.0)
GREEN = (0.24699999392032623, 0.6779999732971191, 0.28200000524520874)


def rgb_close(a, b, tol=0.05) -> bool:
    return all(abs(x - y) <= tol for x, y in zip(a[:3], b[:3]))


def classify_bar(fill) -> str | None:
    if fill is None:
        return None
    if rgb_close(fill, YELLOW, 0.02):
        return "emballages"
    if rgb_close(fill, TEXT_GRAY, 0.02) and False:
        return "ordures"
    if rgb_close(fill, GREEN, 0.05):
        return "verre"
    return None


def classify_encombrants(d) -> bool:
    # Pink/magenta square outline around day number
    fill = d.get("fill")
    if fill and rgb_close(fill, (0.929, 0.11, 0.141), 0.05):
        r = d["rect"]
        return r.height < 8 and r.width < 8
    # Black stroke box
    if d.get("color") == (0.0, 0.0, 0.0) and d.get("fill") is None:
        r = d["rect"]
        return 8 <= r.width <= 12 and 8 <= r.height <= 12
    return False


def cell_for(month, x_min, x_max, y_start, y_end, cx, cy):
    col_w = (x_max - x_min) / 7
    row_h = (y_end - y_start) / 6
    col = int((cx - x_min) / col_w)
    row = int((cy - y_start) / row_h)
    col = max(0, min(6, col))
    row = max(0, min(5, row))
    weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)
    if row >= len(weeks):
        return None
    dom = weeks[row][col]
    return dom or None


def extract():
    doc = fitz.open(PDF)
    page = doc[0]
    schedule: dict[date, set[str]] = defaultdict(set)

    for d in page.get_drawings():
        r = d["rect"]
        if r.height < 15:
            continue
        cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
        fill = d.get("fill")
        kind = classify_bar(fill)
        if kind:
            for month, x_min, x_max, y_start, y_end in MONTHS:
                if x_min <= cx <= x_max and y_start <= cy <= y_end:
                    dom = cell_for(month, x_min, x_max, y_start, y_end, cx, cy)
                    if dom:
                        schedule[date(YEAR, month, dom)].add(kind)
                        if kind == "emballages":
                            # paired gray bar beside yellow
                            schedule[date(YEAR, month, dom)].add("ordures")
                    break
            continue
        # gray bar paired with yellow (same cell, h>=15)
        if fill and rgb_close(fill, TEXT_GRAY, 0.02) and r.height >= 30:
            for month, x_min, x_max, y_start, y_end in MONTHS:
                if x_min <= cx <= x_max and y_start <= cy <= y_end:
                    dom = cell_for(month, x_min, x_max, y_start, y_end, cx, cy)
                    if dom:
                        schedule[date(YEAR, month, dom)].add("ordures")
                    break

    for d in page.get_drawings():
        if not classify_encombrants(d):
            continue
        r = d["rect"]
        cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
        for month, x_min, x_max, y_start, y_end in MONTHS:
            if x_min <= cx <= x_max + 5 and y_start - 15 <= cy <= y_end:
                dom = cell_for(month, x_min, x_max, y_start, y_end, cx, cy)
                if dom:
                    schedule[date(YEAR, month, dom)].add("encombrants")
                break

    # Green verre bars (different green shade)
    for d in page.get_drawings():
        r = d["rect"]
        if r.height < 15:
            continue
        fill = d.get("fill")
        if not fill:
            continue
        r_val, g, b = fill[:3]
        if g > 0.55 and r_val < 0.35 and b < 0.35 and not rgb_close(fill, TEXT_GRAY, 0.02):
            cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
            for month, x_min, x_max, y_start, y_end in MONTHS:
                if x_min <= cx <= x_max and y_start <= cy <= y_end:
                    dom = cell_for(month, x_min, x_max, y_start, y_end, cx, cy)
                    if dom:
                        schedule[date(YEAR, month, dom)].add("verre")
                    break

    return {d: sorted(v) for d, v in sorted(schedule.items())}


def main():
    schedule = extract()
    for d in [date(2026, 9, 2), date(2026, 9, 9), date(2026, 10, 7), date(2026, 10, 8)]:
        print(d.isoformat(), "->", schedule.get(d, []))

    print("\nSeptember:")
    for d, types in schedule.items():
        if d.month == 9:
            print(d.isoformat(), types)

    print("\nOctober:")
    for d, types in schedule.items():
        if d.month == 10:
            print(d.isoformat(), types)

    print("\nTotal:", len(schedule))


if __name__ == "__main__":
    main()

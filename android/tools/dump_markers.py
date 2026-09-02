import fitz
import calendar
from datetime import date
from collections import defaultdict

PDF = r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf"
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

YELLOW = (1.0, 0.9449999928474426, 0.0)


def rgb_close(a, b, tol=0.02):
    return all(abs(x - y) <= tol for x, y in zip(a[:3], b[:3]))


def dom_at(month, x_min, x_max, y_start, y_end, cx, cy):
    col_w = (x_max - x_min) / 7
    row_h = (y_end - y_start) / 6
    col = max(0, min(6, int((cx - x_min) / col_w)))
    row = max(0, min(5, int((cy - y_start) / row_h)))
    weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)
    if row >= len(weeks):
        return None
    return weeks[row][col] or None


doc = fitz.open(PDF)
page = doc[0]

print("YELLOW BARS -> dates:")
for d in page.get_drawings():
    r = d["rect"]
    fill = d.get("fill")
    if not fill or not rgb_close(fill, YELLOW):
        continue
    if r.height < 20:
        continue
    cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
    for month, x_min, x_max, y_start, y_end in MONTHS:
        if x_min <= cx <= x_max and y_start <= cy <= y_end:
            dom = dom_at(month, x_min, x_max, y_start, y_end, cx, cy)
            if dom:
                print(f"  {date(YEAR, month, dom).isoformat()} at ({cx:.1f},{cy:.1f}) row={int((cy-y_start)/((y_end-y_start)/6))} col={int((cx-x_min)/((x_max-x_min)/7))}")
            break

print("\nSTROKE BOXES -> dates:")
for d in page.get_drawings():
    r = d["rect"]
    if d.get("color") != (0.0, 0.0, 0.0) or d.get("fill") is not None:
        continue
    if not (8 <= r.width <= 12 and 8 <= r.height <= 12):
        continue
    cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
    for month, x_min, x_max, y_start, y_end in MONTHS:
        if x_min - 5 <= cx <= x_max + 5 and y_start - 10 <= cy <= y_end + 5:
            dom = dom_at(month, x_min, x_max, y_start, y_end, cx, cy)
            if dom:
                print(f"  {date(YEAR, month, dom).isoformat()} at ({cx:.1f},{cy:.1f})")
            else:
                print(f"  ? month={month} at ({cx:.1f},{cy:.1f})")
            break

print("\nRED DAY DIGITS -> dates:")
RED = (0.9290000200271606, 0.10999999940395355, 0.14100000262260437)
for d in page.get_drawings():
    r = d["rect"]
    fill = d.get("fill")
    if not fill or not rgb_close(fill, RED, 0.02):
        continue
    if r.height > 8:
        continue
    cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
    for month, x_min, x_max, y_start, y_end in MONTHS:
        if x_min <= cx <= x_max and y_start <= cy <= y_end:
            dom = dom_at(month, x_min, x_max, y_start, y_end, cx, cy)
            if dom:
                print(f"  {date(YEAR, month, dom).isoformat()} digit at ({cx:.1f},{cy:.1f})")
            break

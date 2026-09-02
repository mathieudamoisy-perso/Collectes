"""Validate Kotlin CCVT parser logic against PNG fixture."""
from __future__ import annotations

import calendar
from datetime import date
from pathlib import Path

from PIL import Image

PNG = Path(r"c:\Projets Cursor\Smirtom\android\app\src\test\resources\calendars\bouconvillers_page.png")
SCALE = 6
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

COL_YELLOW_THRESHOLD = 1500
BORDER_ENCOMBRANTS_THRESHOLD = 6
BORDER_VERRE_THRESHOLD = 8


def is_yellow(r, g, b):
    return r >= 240 and g >= 200 and b <= 60


def border_marks(img, x0, y0, x1, y1):
    enc = verre = 0
    for x in range(x0, x1, 2):
        for y in (y0 + 1, y1 - 2):
            if y0 <= y < y1:
                r, g, b = img.getpixel((x, y))[:3]
                if r >= 180 and g <= 130 and b >= 130:
                    enc += 1
                if g >= 170 and r <= 120 and b <= 140:
                    verre += 1
    for y in range(y0, y1, 2):
        for x in (x0 + 1, x1 - 2):
            if x0 <= x < x1:
                r, g, b = img.getpixel((x, y))[:3]
                if r >= 180 and g <= 130 and b >= 130:
                    enc += 1
                if g >= 170 and r <= 120 and b <= 140:
                    verre += 1
    return enc >= BORDER_ENCOMBRANTS_THRESHOLD, verre >= BORDER_VERRE_THRESHOLD


def weekday_dates(year, month, weekday_col):
    # 0=Mon ... 6=Sun
    weekday = weekday_col
    dates = []
    weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(year, month)
    for week in weeks:
        dom = week[weekday]
        if dom:
            dates.append(date(year, month, dom))
    return dates


img = Image.open(PNG)
schedule = {}

for month, x_min, x_max, y_start, y_end in MONTHS:
    sx0 = int(x_min * SCALE)
    sy0 = int(y_start * SCALE)
    block_w = int((x_max - x_min) * SCALE)
    block_h = int((y_end - y_start) * SCALE)
    col_w = block_w // 7
    row_h = block_h // 6
    weeks = calendar.Calendar(firstweekday=0).monthdayscalendar(YEAR, month)

    for col in range(7):
        x0 = sx0 + col * col_w
        x1 = x0 + col_w
        yellow = sum(
            1
            for y in range(sy0, sy0 + block_h)
            for x in range(x0, x1)
            if is_yellow(*img.getpixel((x, y))[:3])
        )
        if yellow >= COL_YELLOW_THRESHOLD:
            for d in weekday_dates(YEAR, month, col):
                schedule.setdefault(d, set()).update({"emballages", "ordures"})

    for row, week in enumerate(weeks):
        for col, dom in enumerate(week):
            if dom == 0:
                continue
            x0 = sx0 + col * col_w
            y0 = sy0 + row * row_h
            x1 = x0 + col_w
            y1 = y0 + row_h
            d = date(YEAR, month, dom)
            enc, ver = border_marks(img, x0, y0, x1, y1)
            if enc:
                schedule.setdefault(d, set()).add("encombrants")
            if ver:
                schedule.setdefault(d, set()).add("verre")

for d in [date(2026, 9, 9), date(2026, 10, 7), date(2026, 10, 8)]:
    print(d.isoformat(), sorted(schedule.get(d, [])))

print("Oct Wednesdays:")
for d in sorted(schedule):
    if d.month == 10 and d.weekday() == 2:
        print(d.isoformat(), sorted(schedule[d]))

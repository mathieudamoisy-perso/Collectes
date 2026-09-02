import pdfplumber
import re
from collections import defaultdict
from datetime import date

PDF = r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf"
YEAR = 2026

# Grid layout from PDF analysis
MONTH_BLOCKS = [
    # (month, header_top, weekday_row_top, col_x for LU..DI)
    (1, 214.1, 226.5, [22.5, 37.3, 54.0, 68.5, 85.5, 102.8, 115.9]),
    (2, 214.1, 226.7, [158.1, 172.9, 189.6, 204.1, 221.1, 238.4, 251.5]),
    (3, 214.1, 226.7, [289.4, 304.2, 321.0, 335.5, 352.4, 369.8, 382.9]),
    (4, 297.5, 310.8, [22.5, 37.3, 54.0, 68.5, 85.5, 102.8, 116.9]),
    (5, 297.5, 310.8, [158.1, 172.9, 189.6, 204.2, 221.1, 238.4, 252.6]),
    (6, 297.5, 310.8, [290.4, 305.2, 321.9, 337.5, 353.4, 370.7, 384.8]),
    (7, 374.3, 389.7, [22.3, 37.1, 53.8, 68.4, 85.3, 102.6, 116.7]),
    (8, 374.3, 389.7, [158.0, 172.9, 189.6, 204.1, 221.0, 238.4, 252.5]),
    (9, 374.3, 389.7, [290.2, 305.0, 321.7, 336.3, 353.2, 370.6, 385.7]),
    (10, 461.1, 476.3, [22.3, 37.1, 53.8, 68.4, 85.3, 101.7, 116.8]),
    (11, 461.1, 476.5, [158.2, 173.0, 189.7, 204.2, 221.2, 237.5, 252.6]),
    (12, 461.1, 476.7, [290.4, 305.2, 321.9, 336.5, 353.4, 369.7, 384.9]),
]

# Legend x ranges (approx from PDF)
LEGEND = {
    "emballages_ordures": (111.4, 170.8),  # Bac jaune + Bac gris area - both collected together
    "verre": (223.1, 265.6),
    "encombrants": (265.6, 330.0),
}

import calendar

def month_week_rows(month, year):
    cal = calendar.Calendar(firstweekday=0)  # Monday
    weeks = cal.monthdayscalendar(year, month)
    return weeks

with pdfplumber.open(PDF) as pdf:
    page = pdf.pages[0]
    chars = page.chars
    
    # Find numeric day chars in calendar area
    day_chars = []
    for c in chars:
        t = c["text"]
        if not t.isdigit():
            continue
        top = c["top"]
        x0 = c["x0"]
        if top < 230 or top > 550:
            continue
        day_chars.append(c)
    
    print(f"Found {len(day_chars)} digit chars in calendar area")
    
    # Group chars into numbers by proximity
    numbers = []
    used = set()
    for i, c in enumerate(day_chars):
        if i in used:
            continue
        num = c["text"]
        used.add(i)
        # try merge with next digit on same line
        for j, c2 in enumerate(day_chars):
            if j in used:
                continue
            if abs(c2["top"] - c["top"]) < 3 and 0 < c2["x0"] - c["x1"] < 5:
                num += c2["text"]
                used.add(j)
        numbers.append({
            "day": int(num),
            "top": c["top"],
            "x0": c["x0"],
            "x1": c.get("x1", c["x0"]+5),
            "color": c.get("non_stroking_color"),
            "font": c.get("fontname"),
        })
    
    print(f"Parsed {len(numbers)} day numbers")
    for n in sorted(numbers, key=lambda x: (x["top"], x["x0"]))[:30]:
        print(n)

    # Find colored marker chars/symbols near day numbers
    # Look for non-digit chars near day numbers with distinct colors
    marker_chars = [c for c in chars if c["top"] > 230 and c["top"] < 550]
    print(f"\nTotal chars in grid: {len(marker_chars)}")
    
    # Color distribution
    colors = defaultdict(int)
    for c in marker_chars:
        colors[str(c.get("non_stroking_color"))] += 1
    print("Colors:", dict(colors))

    # Show chars near Sept 9 and Oct 7-8
    for target_month, target_day in [(9, 9), (10, 7), (10, 8)]:
        print(f"\n=== Looking for {target_day}/{target_month} ===")
        # find block
        block = next(b for b in MONTH_BLOCKS if b[0] == target_month)
        month, header_top, weekday_top, col_xs = block
        
        # estimate row tops - 6 rows below weekday header, ~11pt spacing
        weeks = month_week_rows(target_month, YEAR)
        row_tops = [weekday_top + 12 + i * 11.2 for i in range(6)]
        
        for wi, week in enumerate(weeks):
            for di, dom in enumerate(week):
                if dom == target_day:
                    cx = col_xs[di]
                    rt = row_tops[wi]
                    print(f"  Expected at col={di} x~{cx}, row top~{rt}")
                    nearby = [c for c in chars if abs(c["top"] - rt) < 8 and abs(c["x0"] - cx) < 20]
                    for c in sorted(nearby, key=lambda x: x["x0"]):
                        print(f"    {c['text']!r} top={c['top']:.1f} x={c['x0']:.1f} color={c.get('non_stroking_color')} font={c.get('fontname','')[:20]}")

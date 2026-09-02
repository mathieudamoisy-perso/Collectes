import fitz

pdf = fitz.open(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
page = pdf[0]

def inspect(month_block, label, col, row):
    x_min, x_max, y_start, y_end = month_block
    col_w = (x_max - x_min) / 7
    row_h = (y_end - y_start) / 6
    cell_x0 = x_min + col * col_w
    cell_x1 = cell_x0 + col_w
    cell_y0 = y_start + row * row_h
    cell_y1 = cell_y0 + row_h
    print(f"\n{label} [{cell_x0:.1f},{cell_y0:.1f}]-[{cell_x1:.1f},{cell_y1:.1f}]")
    for d in page.get_drawings():
        r = d["rect"]
        cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
        if cell_x0 - 2 <= cx <= cell_x1 + 2 and cell_y0 - 2 <= cy <= cell_y1 + 2:
            fill, color = d.get("fill"), d.get("color")
            print(
                f"  ({cx:.1f},{cy:.1f}) {r.width:.1f}x{r.height:.1f}"
                f" fill={tuple(round(x,3) for x in fill[:3]) if fill else None}"
                f" stroke={tuple(round(x,3) for x in color[:3]) if color else None}"
            )

inspect((12, 132, 246, 330), "Jan 7", 2, 1)
inspect((12, 132, 246, 330), "Jan 14", 2, 2)
inspect((284, 404, 406, 490), "Sep 2", 2, 0)
inspect((284, 404, 406, 490), "Sep 9", 2, 1)
inspect((12, 132, 493, 577), "Oct 1", 3, 0)
inspect((12, 132, 493, 577), "Oct 7", 2, 1)
inspect((12, 132, 493, 577), "Oct 8", 3, 1)

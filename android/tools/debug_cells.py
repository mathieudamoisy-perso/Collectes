from PIL import Image
import fitz

SCALE = 6
pdf = fitz.open(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
pix = pdf[0].get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

# Oct 7: month 10, row 1 col 2
# Oct 8: row 1 col 3
# Oct 14: row 2 col 2
blocks = {
    "Oct 7": (10, 1, 2),
    "Oct 8": (10, 1, 3),
    "Oct 14": (10, 2, 2),
    "Sep 9": (9, 1, 2),
}

MONTHS = {
    9: (284, 404, 406, 490),
    10: (12, 132, 493, 577),
}

for label, (month, row, col) in blocks.items():
    x_min, x_max, y_start, y_end = MONTHS[month]
    sx0 = int(x_min * SCALE)
    sy0 = int(y_start * SCALE)
    block_w = int((x_max - x_min) * SCALE)
    block_h = int((y_end - y_start) * SCALE)
    col_w = block_w // 7
    row_h = block_h // 6
    x0 = sx0 + col * col_w
    y0 = sy0 + row * row_h
    x1 = x0 + col_w
    y1 = y0 + row_h
    print(f"\n{label} cell pixels [{x0},{y0}]-[{x1},{y1}]")
    colors = {}
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = img.getpixel((x, y))[:3]
            colors[c] = colors.get(c, 0) + 1
    top = sorted(colors.items(), key=lambda kv: -kv[1])[:8]
    for (r, g, b), n in top:
        print(f"  rgb=({r:3},{g:3},{b:3}) count={n}")

from PIL import Image
import fitz

SCALE = 6
pdf = fitz.open(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
pix = pdf[0].get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

# Oct block cells 7 and 8
for label, row, col in [("Oct7", 1, 2), ("Oct8", 1, 3), ("Oct1", 0, 3)]:
    x_min, x_max, y_start, y_end = 12, 132, 493, 577
    sx0 = int(x_min * SCALE)
    sy0 = int(y_start * SCALE)
    bw = int((x_max - x_min) * SCALE)
    bh = int((y_end - y_start) * SCALE)
    cw = bw // 7
    rh = bh // 6
    x0 = sx0 + col * cw
    y0 = sy0 + row * rh
    x1 = x0 + cw
    y1 = y0 + rh
    crop = img.crop((x0, y0, x1, y1))
    crop.save(rf"C:\Users\mathieu.damoisy\AppData\Local\Temp\{label}.png")
    print(label, crop.size)

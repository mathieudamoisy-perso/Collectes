from PIL import Image
import fitz

SCALE = 6
pdf = fitz.open(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
pix = pdf[0].get_pixmap(matrix=fitz.Matrix(SCALE, SCALE))
img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)
x_min, x_max, y_start, y_end = 12, 132, 493, 577
crop = img.crop((int(x_min * SCALE), int(y_start * SCALE), int(x_max * SCALE), int(y_end * SCALE)))
crop.save(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\oct_full.png")
print(crop.size)

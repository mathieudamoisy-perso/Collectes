import fitz

pdf = fitz.open(r"C:\Users\mathieu.damoisy\AppData\Local\Temp\BOUCONVILLERS-2026.pdf")
page = pdf[0]

print("Drawings near Oct 7-8 (cx 60-85, cy 495-525):")
for d in page.get_drawings():
    r = d["rect"]
    cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
    if not (60 <= cx <= 85 and 495 <= cy <= 525):
        continue
    print(
        f"  ({cx:.1f},{cy:.1f}) {r.width:.1f}x{r.height:.1f}"
        f" fill={d.get('fill')} stroke={d.get('color')}"
    )

# Any pink/magenta filled or stroked shapes in october block
print("\nPink/magenta in oct block:")
for d in page.get_drawings():
    r = d["rect"]
    cx, cy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
    if not (12 <= cx <= 132 and 493 <= cy <= 577):
        continue
    fill, color = d.get("fill"), d.get("color")
    for c in [fill, color]:
        if not c:
            continue
        r1, g, b = c[:3]
        if r1 > 0.7 and b > 0.4 and g < 0.4:
            print(f"  ({cx:.1f},{cy:.1f}) fill={fill} color={color} size={r.width:.1f}x{r.height:.1f}")

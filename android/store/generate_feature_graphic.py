"""Generate Play Store feature graphic (1024x500)."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

WIDTH, HEIGHT = 1024, 500
ROOT = Path(__file__).resolve().parent
ICON = ROOT / "ic_launcher_play_store_512.png"
OUT = ROOT / "feature_graphic_1024x500.png"

# App theme (Theme.kt)
SAGE = (0x5E, 0x8F, 0x66)
SAGE_SOFT = (0xA8, 0xC5, 0xAC)
MINT = (0xDC, 0xEF, 0xDE)
BG = (0xF7, 0xFB, 0xF6)
TEXT = (0x1A, 0x1C, 0x1A)
TEXT_MUTED = (0x2F, 0x4A, 0x34)


def lerp(a: int, b: int, t: float) -> int:
    return int(a + (b - a) * t)


def gradient_background() -> Image.Image:
    img = Image.new("RGB", (WIDTH, HEIGHT))
    px = img.load()
    for x in range(WIDTH):
        t = x / (WIDTH - 1)
        r = lerp(BG[0], MINT[0], t * 0.55)
        g = lerp(BG[1], MINT[1], t * 0.55)
        b = lerp(BG[2], MINT[2], t * 0.55)
        for y in range(HEIGHT):
            ty = y / (HEIGHT - 1)
            rr = lerp(r, SAGE_SOFT[0], ty * 0.12)
            gg = lerp(g, SAGE_SOFT[1], ty * 0.12)
            bb = lerp(b, SAGE_SOFT[2], ty * 0.12)
            px[x, y] = (rr, gg, bb)
    return img


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def main() -> None:
    canvas = gradient_background()
    draw = ImageDraw.Draw(canvas)

    # Decorative circles
    draw.ellipse((820, -80, 1120, 220), fill=(*SAGE_SOFT, 40))
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    odraw = ImageDraw.Draw(overlay)
    odraw.ellipse((780, 280, 1060, 560), fill=(94, 143, 102, 28))
    odraw.ellipse((-60, 320, 220, 560), fill=(168, 197, 172, 35))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), overlay).convert("RGB")
    draw = ImageDraw.Draw(canvas)

    # Accent bar
    draw.rounded_rectangle((72, 96, 78, 404), radius=3, fill=SAGE)

    # Icon
    icon = Image.open(ICON).convert("RGBA")
    icon_size = 280
    icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    canvas.paste(icon, (110, (HEIGHT - icon_size) // 2), icon)
    draw = ImageDraw.Draw(canvas)

    title_font = load_font(72, bold=True)
    subtitle_font = load_font(30, bold=False)
    tag_font = load_font(22, bold=False)

    title = "Collectes"
    subtitle = "Rappels la veille des collectes de déchets"
    tagline = "Vexin · Sannois · Ermont · Guide du tri intégré"

    text_x = 430
    title_y = 148
    draw.text((text_x, title_y), title, fill=TEXT, font=title_font)
    draw.text((text_x, title_y + 92), subtitle, fill=TEXT_MUTED, font=subtitle_font)
    draw.text((text_x, title_y + 142), tagline, fill=SAGE, font=tag_font)

    # Bottom feature pills
    pills = ["Ordures", "Emballages", "Verre", "Végétaux", "Notifications"]
    pill_y = 380
    pill_x = text_x
    pill_font = load_font(18, bold=True)
    for label in pills:
        bbox = draw.textbbox((0, 0), label, font=pill_font)
        pw = bbox[2] - bbox[0] + 28
        ph = 36
        draw.rounded_rectangle(
            (pill_x, pill_y, pill_x + pw, pill_y + ph),
            radius=18,
            fill=(220, 239, 222),
            outline=SAGE_SOFT,
            width=1,
        )
        draw.text((pill_x + 14, pill_y + 8), label, fill=TEXT_MUTED, font=pill_font)
        pill_x += pw + 10

    canvas.save(OUT, format="PNG", optimize=True)
    print(f"Saved {OUT} ({OUT.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()

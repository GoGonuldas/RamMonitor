"""
RamMonitor icon generator.
Outputs:
  app/play-store-icon-512.png            (Store icon 512x512)
  app/play-store-icon-216.png            (Huawei alt boyut)
  app/play-feature-graphic-1024x500.png  (Feature graphic)

Tasarım: Koyu mavi degrade arka plan + beyaz "memory chip" çerçeve içinde
yükselen bar chart (RAM kullanımı metaforu) + alt köşede yeşil aktivite noktası.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app")
os.makedirs(OUT_DIR, exist_ok=True)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vertical_gradient(size, top, bottom):
    img = Image.new("RGB", size, top)
    px = img.load()
    w, h = size
    for y in range(h):
        c = lerp(top, bottom, y / max(1, h - 1))
        for x in range(w):
            px[x, y] = c
    return img


def diagonal_gradient(size, top_left, bottom_right):
    w, h = size
    img = Image.new("RGB", size, top_left)
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x + y) / (w + h)
            px[x, y] = lerp(top_left, bottom_right, t)
    return img


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def make_icon(size=1024):
    """Render at high resolution then downscale for crisp anti-aliasing."""
    s = size
    bg_top = (28, 50, 110)        # deep blue
    bg_bottom = (10, 22, 60)      # darker blue
    accent = (88, 160, 255)       # light blue
    bar_low = (90, 200, 140)      # green
    bar_mid = (255, 200, 80)      # amber
    bar_high = (255, 110, 110)    # red
    white = (255, 255, 255)

    img = vertical_gradient((s, s), bg_top, bg_bottom)

    draw = ImageDraw.Draw(img, "RGBA")

    # Soft inner glow circle
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([s * 0.05, s * 0.05, s * 0.95, s * 0.95],
                  fill=(88, 160, 255, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(s * 0.06))
    img.paste(glow, (0, 0), glow)

    # Memory chip outline (rounded rect) centered
    pad = int(s * 0.18)
    chip_box = [pad, pad, s - pad, s - pad]
    chip_radius = int(s * 0.08)
    border = int(s * 0.025)
    # outer chip border
    draw.rounded_rectangle(chip_box, radius=chip_radius,
                           outline=white + (255,), width=border)

    # "Pins" on left & right (memory chip legs)
    pin_w = int(s * 0.018)
    pin_h = int(s * 0.04)
    pin_gap = int(s * 0.07)
    chip_top = chip_box[1]
    chip_bot = chip_box[3]
    pin_y_positions = [chip_top + int(s * 0.12) + i * pin_gap for i in range(5)]
    for py in pin_y_positions:
        # left
        draw.rounded_rectangle(
            [chip_box[0] - pin_h, py, chip_box[0], py + pin_w * 2],
            radius=pin_w, fill=accent + (255,))
        # right
        draw.rounded_rectangle(
            [chip_box[2], py, chip_box[2] + pin_h, py + pin_w * 2],
            radius=pin_w, fill=accent + (255,))

    # Bar chart inside the chip
    inner_pad = int(s * 0.07)
    inner_box = [chip_box[0] + inner_pad, chip_box[1] + inner_pad,
                 chip_box[2] - inner_pad, chip_box[3] - inner_pad]
    iw = inner_box[2] - inner_box[0]
    ih = inner_box[3] - inner_box[1]

    bars = 5
    gap = int(iw * 0.06)
    bar_w = (iw - gap * (bars - 1)) // bars
    # heights (percentages) ascending
    heights = [0.30, 0.55, 0.42, 0.78, 0.92]
    colors = [bar_low, bar_low, bar_mid, bar_mid, bar_high]
    base_y = inner_box[3]
    for i in range(bars):
        x0 = inner_box[0] + i * (bar_w + gap)
        bh = int(ih * heights[i])
        y0 = base_y - bh
        draw.rounded_rectangle(
            [x0, y0, x0 + bar_w, base_y],
            radius=int(bar_w * 0.22),
            fill=colors[i] + (255,))

    # baseline
    draw.rounded_rectangle(
        [inner_box[0], base_y + int(s * 0.005),
         inner_box[2], base_y + int(s * 0.018)],
        radius=int(s * 0.01), fill=white + (220,))

    # downscale for AA
    if size != 1024:
        img = img.resize((size, size), Image.LANCZOS)
    return img


def make_feature_graphic():
    w, h = 1024, 500
    img = diagonal_gradient((w, h), (28, 50, 110), (10, 22, 60))
    draw = ImageDraw.Draw(img, "RGBA")

    # Subtle bars on the right side
    icon = make_icon(360)
    img.paste(icon, (w - 360 - 60, (h - 360) // 2), icon.convert("RGBA"))

    # Title
    try:
        font_title = ImageFont.truetype(
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf", 78)
        font_sub = ImageFont.truetype(
            "/System/Library/Fonts/Supplemental/Arial.ttf", 36)
    except Exception:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()

    draw.text((60, 170), "RamMonitor", font=font_title, fill=(255, 255, 255))
    draw.text((62, 270),
              "RAM • Battery • Network • Storage",
              font=font_sub, fill=(180, 210, 255))
    draw.text((62, 320),
              "Lightweight  •  Ad-free  •  On-device",
              font=font_sub, fill=(140, 180, 240))
    return img


def save(img, name):
    path = os.path.abspath(os.path.join(OUT_DIR, name))
    img.save(path, "PNG", optimize=True)
    print("wrote", path)


if __name__ == "__main__":
    icon512 = make_icon(512)
    save(icon512, "play-store-icon-512.png")

    icon216 = make_icon(216)
    save(icon216, "play-store-icon-216.png")

    # Adaptive icon foreground (Android launcher 432x432 safe zone in 512)
    save(make_icon(1024), "play-store-icon-1024.png")

    fg = make_feature_graphic()
    save(fg, "play-feature-graphic-1024x500.png")


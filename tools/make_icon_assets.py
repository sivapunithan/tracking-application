#!/usr/bin/env python3
"""Mathi icon pipeline, step 2: turn design/icon-source/symbol_path.json (traced by tools/trace_symbol.py) into the Android vector layers and previews.

Writes (res = app/src/main/res):
  drawable/ic_launcher_foreground.xml   gold gradient symbol, ~58% of the 108dp canvas (adaptive-icon safe zone)
  drawable/ic_launcher_background.xml   crimson -> maroon + soft radial vignette (no border)
  drawable/ic_launcher_monochrome.xml   the symbol alone (the system tints it for Android 13 themed icons)
  drawable/ic_splash.xml                symbol inside the splash safe circle (288dp canvas, 192dp circle)
  drawable/app_logo.xml                 rounded crimson tile with the symbol (lock / welcome badge)
  drawable/ic_stat_wrapup.xml           one-colour 24dp notification icon
and design/icon-preview/*.png (masks x sizes) from the SAME path data.
"""
import json, math, re, os
from PIL import Image, ImageDraw, ImageChops, ImageFont, ImageFilter

RES = "app/src/main/res"
d = json.load(open("design/icon-source/symbol_path.json"))
x0, y0, x1, y1 = d["bbox"]
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
W = x1 - x0
GOLD_A, GOLD_B = "#E9CB82", "#B8862B"
CRIMSON, MAROON = "#B0102C", "#6E0A1E"

def parse(path):
    """-> list of subpaths; each a list of ('M'|'L'|'C', [points])."""
    subs, cur = [], None
    for cmd, args in re.findall(r"([MLCZ])([^MLCZ]*)", path):
        nums = [tuple(map(float, p.split(","))) for p in args.split()]
        if cmd == "M": cur = [("M", nums)]; subs.append(cur)
        elif cmd in "LC": cur.append((cmd, nums))
    return subs

SUBS = parse(d["path"])

def path_data(width, cx_t, cy_t, prec=1):
    """Symbol scaled so its bbox is `width` wide, centred at (cx_t, cy_t)."""
    k = width / W
    f = lambda p: f"{(p[0] - cx) * k + cx_t:.{prec}f},{(p[1] - cy) * k + cy_t:.{prec}f}"
    out = []
    for sub in SUBS:
        for cmd, pts in sub:
            out.append(cmd + " ".join(f(p) for p in pts))
        out.append("Z")
    return " ".join(out)

def write(name, text):
    os.makedirs(os.path.dirname(name), exist_ok=True)
    open(name, "w").write(text)

NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"'
def gradient_fill(x1_, y1_, x2_, y2_):
    return (f'<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="{x1_:.1f}" android:startY="{y1_:.1f}" '
            f'android:endX="{x2_:.1f}" android:endY="{y2_:.1f}" android:startColor="{GOLD_A}" android:endColor="{GOLD_B}" /></aapt:attr>')

# ---- adaptive foreground: 58% of 108 = 62.64dp wide, centred ----
fw = 108 * 0.58
fk = fw / W; fh = (y1 - y0) * fk
write(f"{RES}/drawable/ic_launcher_foreground.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Mathi symbol (traced from design/icon-source by tools/trace_symbol.py + tools/make_icon_assets.py). Inside the adaptive-icon safe zone. -->
<vector {NS} android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillType="evenOdd" android:pathData="{path_data(fw, 54, 54)}">
        {gradient_fill(54 - fw / 2, 54 - fh / 2, 54 + fw / 2, 54 + fh / 2)}
    </path>
</vector>
''')
write(f"{RES}/drawable/ic_launcher_monochrome.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Android 13+ themed icon: the system supplies the colour, only the shape matters. -->
<vector {NS} android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FF000000" android:fillType="evenOdd" android:pathData="{path_data(fw, 54, 54)}" />
</vector>
''')
write(f"{RES}/drawable/ic_launcher_background.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Crimson to maroon with a soft radial vignette. No border: launcher masks differ. -->
<vector {NS} android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108" android:startColor="{CRIMSON}" android:endColor="{MAROON}" /></aapt:attr>
    </path>
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="54" android:centerY="48" android:gradientRadius="80" android:startColor="#26FFFFFF" android:centerColor="#00000000" android:endColor="#4D000000" /></aapt:attr>
    </path>
</vector>
''')
# ---- splash: 288dp canvas, visible circle 192dp: keep the symbol inside it (width 138 -> half diagonal ~87 < 96) ----
sw = 138; sk = sw / W; sh = (y1 - y0) * sk
write(f"{RES}/drawable/ic_splash.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Splash icon: inside the 192dp safe circle of the 288dp canvas, so every device mask keeps it whole. -->
<vector {NS} android:width="288dp" android:height="288dp" android:viewportWidth="288" android:viewportHeight="288">
    <path android:fillType="evenOdd" android:pathData="{path_data(sw, 144, 144)}">
        {gradient_fill(144 - sw / 2, 144 - sh / 2, 144 + sw / 2, 144 + sh / 2)}
    </path>
</vector>
''')
# ---- in-app tile (lock + welcome badge): crimson rounded square, symbol ~70% wide ----
tw = 108 * 0.70; tk = tw / W; th = (y1 - y0) * tk
write(f"{RES}/drawable/app_logo.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Brand tile shown on the lock and welcome screens (looked up BY NAME: see res/raw/keep.xml). -->
<vector {NS} android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M24,0h60a24,24 0,0 1,24 24v60a24,24 0,0 1,-24 24h-60a24,24 0,0 1,-24 -24v-60a24,24 0,0 1,24 -24z">
        <aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108" android:startColor="{CRIMSON}" android:endColor="{MAROON}" /></aapt:attr>
    </path>
    <path android:fillType="evenOdd" android:pathData="{path_data(tw, 54, 56)}">
        {gradient_fill(54 - tw / 2, 56 - th / 2, 54 + tw / 2, 56 + th / 2)}
    </path>
</vector>
''')
# ---- notification: white, 24dp, symbol 20 wide ----
write(f"{RES}/drawable/ic_stat_wrapup.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Notification small icon: one colour (the system tints it), the Mathi symbol. -->
<vector {NS} android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="{path_data(20, 12, 12.4, 2)}" />
</vector>
''')
print("vector files written; foreground pathData chars:", len(path_data(fw, 54, 54)))

# ---------------------------------------------------------------- previews (PIL, same path) ----------------
def flatten(sub, steps=14):
    pts = []; cur = None
    for cmd, p in sub:
        if cmd == "M": cur = p[0]; pts.append(cur)
        elif cmd == "L":
            for q in p: pts.append(q); cur = q
        else:
            for i in range(0, len(p), 3):
                a, b, c = p[i:i + 3]
                for t in range(1, steps + 1):
                    u = t / steps; m = 1 - u
                    pts.append((m**3 * cur[0] + 3 * m * m * u * a[0] + 3 * m * u * u * b[0] + u**3 * c[0],
                                m**3 * cur[1] + 3 * m * m * u * a[1] + 3 * m * u * u * b[1] + u**3 * c[1]))
                cur = c
    return pts
FLAT = [flatten(s) for s in SUBS]

def symbol_mask(size, width_frac, ss=4):
    """Anti-aliased L mask of the symbol, `width_frac` of the canvas wide, centred (even-odd)."""
    S = size * ss; k = (width_frac * S) / W
    m = Image.new("L", (S, S), 0)
    for poly in FLAT:
        layer = Image.new("L", (S, S), 0)
        ImageDraw.Draw(layer).polygon([((x - cx) * k + S / 2, (y - cy) * k + S / 2) for x, y in poly], fill=255)
        m = ImageChops.difference(m, layer)          # XOR = even-odd
    return m.resize((size, size), Image.LANCZOS)

def lerp(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))
def hexrgb(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))

def icon_canvas(size=432):
    """Full square icon (background + symbol) as RGB, before any launcher mask."""
    n = size
    bg = Image.new("RGB", (n, n))
    px = bg.load(); A, B = hexrgb(CRIMSON), hexrgb(MAROON)
    for y in range(n):
        for x in range(n):
            t = (x + y) / (2 * (n - 1))
            c = lerp(A, B, t)
            r = math.hypot(x - n / 2, y - n * 0.44) / (n * 0.74)       # radial vignette: lighter centre, darker edge
            f = 1.12 - 0.5 * min(1.0, r) ** 1.6
            px[x, y] = tuple(max(0, min(255, int(v * f))) for v in c)
    m = symbol_mask(n, 0.58)
    ga, gb = hexrgb(GOLD_A), hexrgb(GOLD_B)
    grad = Image.new("RGB", (n, n)); gp = grad.load()
    for y in range(n):
        for x in range(n):
            gp[x, y] = lerp(ga, gb, max(0.0, min(1.0, ((x - n * 0.21) + (y - n * 0.32)) / (n * 0.58 + n * 0.46))))
    bg.paste(grad, (0, 0), m)
    return bg

def mask_shape(kind, n):
    ss = 4; S = n * ss; m = Image.new("L", (S, S), 0); dr = ImageDraw.Draw(m)
    if kind == "circle": dr.ellipse([0, 0, S - 1, S - 1], fill=255)
    elif kind == "rounded":
        dr.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.22), fill=255)
    elif kind == "squircle":
        pts = []
        for i in range(720):
            t = 2 * math.pi * i / 720; c, s = math.cos(t), math.sin(t)
            pts.append((S / 2 + S / 2 * math.copysign(abs(c) ** (2 / 5), c), S / 2 + S / 2 * math.copysign(abs(s) ** (2 / 5), s)))
        dr.polygon(pts, fill=255)
    elif kind == "teardrop":
        r = S // 2; dr.rounded_rectangle([0, 0, S - 1, S - 1], radius=r, fill=255)
        dr.rectangle([S // 2, 0, S - 1, S // 2], fill=255)                    # one square corner (top-right)
        dr.rounded_rectangle([S // 2, 0, S - 1, S // 2], radius=int(S * 0.07), fill=255)
    return m.resize((n, n), Image.LANCZOS)

full = icon_canvas(432)
os.makedirs("design/icon-preview", exist_ok=True)
sheet_sizes = (48, 72, 192)
kinds = ("circle", "squircle", "rounded", "teardrop")
sheet = Image.new("RGB", (len(sheet_sizes) * 0 + 4 * 220 + 20, 3 * 230 + 20), (236, 232, 228))
for r, sz in enumerate(sheet_sizes):
    for ci, kind in enumerate(kinds):
        ic = full.resize((sz, sz), Image.LANCZOS)
        tile = Image.new("RGB", (sz, sz), (236, 232, 228)); tile.paste(ic, (0, 0), mask_shape(kind, sz))
        tile.save(f"design/icon-preview/mathi_{kind}_{sz}px.png")
        show = tile.resize((192, 192), Image.BOX) if sz < 192 else tile      # upscale small sizes (nearest) so they are visible on the sheet
        sheet.paste(show, (10 + ci * 220, 10 + r * 230 + (192 - show.size[1]) // 2))
sheet.save("design/icon-preview/mathi_masks_sheet.png")
full.save("design/icon-preview/mathi_icon_full_432.png")
# safe-zone check: symbol bbox vs the 66% (72dp) circle
k = 0.58 * 108 / W
print("symbol size in 108dp canvas: %.1f x %.1f dp; half-diagonal %.1f dp (safe circle radius 33 dp)" % (W * k, (y1 - y0) * k, math.hypot(W * k, (y1 - y0) * k) / 2))

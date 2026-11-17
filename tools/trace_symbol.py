#!/usr/bin/env python3
"""Mathi icon pipeline, step 1: white background -> alpha, then trace the symbol with the real potrace library (libpotrace0 via ctypes).

Input : design/icon-source/mathi_symbol.png (gold symbol on white)
Output: design/icon-source/mathi_symbol_alpha.png (transparent background, no white halo)
        design/icon-source/symbol_path.json  (one path in image pixel coordinates + its bounding box)
Why ctypes: the potrace command line tool is not installed here, but libpotrace0 is, and it is the same tracer.
"""
import ctypes, json, sys
from PIL import Image

BG = 253          # the "white" of the source picture
UP = 4            # trace at 4x for smoother curves
src = Image.open("design/icon-source/mathi_symbol.png").convert("RGB")
w, h = src.size
px = src.load()

# ---- 1. colour-to-alpha: pixel = a*C + (1-a)*white, solve for a and C (no halo) ----
out = Image.new("RGBA", (w, h))
op = out.load()
alpha = Image.new("L", (w, h))
ap = alpha.load()
for y in range(h):
    for x in range(w):
        r, g, b = px[x, y]
        d = BG - min(r, g, b)
        a = max(0.0, min(1.0, d / 95.0))
        if a <= 0.02:
            op[x, y] = (0, 0, 0, 0); ap[x, y] = 0; continue
        c = [max(0, min(255, int(round((v - (1 - a) * BG) / a)))) for v in (r, g, b)]
        op[x, y] = (c[0], c[1], c[2], int(round(a * 255))); ap[x, y] = int(round(a * 255))
out.save("design/icon-source/mathi_symbol_alpha.png")

# ---- 2. potrace through ctypes ----
lib = ctypes.CDLL("libpotrace.so.0")
class Dpoint(ctypes.Structure): _fields_ = [("x", ctypes.c_double), ("y", ctypes.c_double)]
class Curve(ctypes.Structure):
    _fields_ = [("n", ctypes.c_int), ("tag", ctypes.POINTER(ctypes.c_int)), ("c", ctypes.POINTER(Dpoint * 3))]
class Path(ctypes.Structure): pass
Path._fields_ = [("area", ctypes.c_int), ("sign", ctypes.c_int), ("curve", Curve), ("next", ctypes.POINTER(Path)),
                 ("childlist", ctypes.POINTER(Path)), ("sibling", ctypes.POINTER(Path)), ("priv", ctypes.c_void_p)]
class State(ctypes.Structure): _fields_ = [("status", ctypes.c_int), ("plist", ctypes.POINTER(Path)), ("priv", ctypes.c_void_p)]
class Progress(ctypes.Structure):
    _fields_ = [("callback", ctypes.c_void_p), ("data", ctypes.c_void_p), ("min", ctypes.c_double), ("max", ctypes.c_double), ("epsilon", ctypes.c_double)]
class Param(ctypes.Structure):
    _fields_ = [("turdsize", ctypes.c_int), ("turnpolicy", ctypes.c_int), ("alphamax", ctypes.c_double), ("opticurve", ctypes.c_int),
                ("opttolerance", ctypes.c_double), ("progress", Progress)]
class Bitmap(ctypes.Structure):
    _fields_ = [("w", ctypes.c_int), ("h", ctypes.c_int), ("dy", ctypes.c_int), ("map", ctypes.POINTER(ctypes.c_ulong))]
lib.potrace_param_default.restype = ctypes.POINTER(Param)
lib.potrace_trace.restype = ctypes.POINTER(State)
lib.potrace_trace.argtypes = [ctypes.POINTER(Param), ctypes.POINTER(Bitmap)]

from PIL import ImageFilter
big = alpha.resize((w * UP, h * UP), Image.LANCZOS).filter(ImageFilter.GaussianBlur(UP * 1.2))   # smooth the source's pixel/compression noise before thresholding
bw, bh = big.size
bp = big.load()
WORD = ctypes.sizeof(ctypes.c_ulong) * 8
dy = (bw + WORD - 1) // WORD
buf = (ctypes.c_ulong * (dy * bh))()
for y in range(bh):
    for x in range(bw):
        if bp[x, y] >= 128:
            buf[y * dy + x // WORD] |= 1 << (WORD - 1 - (x % WORD))
bm = Bitmap(bw, bh, dy, ctypes.cast(buf, ctypes.POINTER(ctypes.c_ulong)))
prm = lib.potrace_param_default().contents
prm.turdsize = 40 * UP * UP // 4   # drop specks
prm.alphamax = 1.2                 # keep the sharp stem corners, smooth the rest
prm.opticurve = 1
prm.opttolerance = 4.0             # merge curve segments: fewer points
st = lib.potrace_trace(ctypes.byref(prm), ctypes.byref(bm)).contents
if st.status != 0: sys.exit("potrace failed")

# rows were fed top-to-bottom, so potrace's y equals image y (orientation does not matter for an even-odd fill)
cmds, pts = [], []
def P(p): pts.append((p.x / UP, p.y / UP)); return f"{p.x / UP:.2f},{p.y / UP:.2f}"
p = st.plist
while p:
    cv = p.contents.curve
    n = cv.n
    start = cv.c[n - 1][2]
    cmds.append("M" + P(start))
    for i in range(n):
        seg = cv.c[i]
        if cv.tag[i] == 1:   # curveto
            cmds.append("C" + " ".join(P(seg[k]) for k in range(3)))
        else:                # corner: line to the vertex, then to the end point
            cmds.append("L" + P(seg[1]) + " L" + P(seg[2]))
    cmds.append("Z")
    p = p.contents.next
xs = [q[0] for q in pts]; ys = [q[1] for q in pts]
json.dump({"path": " ".join(cmds), "bbox": [min(xs), min(ys), max(xs), max(ys)], "source": [w, h], "segments": len(pts)},
          open("design/icon-source/symbol_path.json", "w"))
print("paths ok, points:", len(pts), "bbox", [round(v, 1) for v in (min(xs), min(ys), max(xs), max(ys))], "pathData chars:", len(" ".join(cmds)))

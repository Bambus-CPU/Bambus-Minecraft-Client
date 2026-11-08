"""Bambus Client 3.3 – Flügel in HD mit zwei Ebenen (für den 3D-Flügel-Renderer).

Je Flügel entstehen drei Bilder (512 x 256):
  <id>_hinten.png  Schwungfedern bzw. Flughaut (hintere Ebene)
  <id>_vorne.png   Deckfedern, Knochen, Adern (vordere Ebene, liegt im Spiel davor)
  <id>.png         beides zusammen (Vorschau im Launcher und im Menü)
Layout: Schulter links (x=0), Spitze rechts; der linke Flügel wird gespiegelt.
Die Vorderkante (EDGE_PTS bzw. DRACHE_KANTE) muss zu Kosmetik.FluegelForm in der Mod passen.

Aufruf: python3 fluegel_3d.py <zielordner> [id ...]"""
import numpy as np
from PIL import Image, ImageDraw
import os, sys

S = 8                 # Supersampling (HD: 1024x512 intern -> 512x256)
OUT_SCALE = 4
W, H = 128, 64
OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
NUR = set(sys.argv[2:])
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)

def canvas():
    return np.zeros((H * S, W * S, 4), float)

def mix(a, b, t):
    t = np.clip(t, 0, 1)[..., None] if np.ndim(t) else np.clip(t, 0, 1)
    return np.asarray(a) * (1 - t) + np.asarray(b) * t

EDGE_PTS = np.array([(0, 12), (16, 6), (36, 3), (58, 3), (80, 6), (96, 10), (106, 14), (121, 25)], float)
def edge_y(x):
    return np.interp(x, EDGE_PTS[:, 0], EDGE_PTS[:, 1])

def feather_to(img, base, end, Wd, colfn, shape="feather", seed=0.0):
    v = np.asarray(end, float) - np.asarray(base, float)
    feather(img, base, np.degrees(np.arctan2(v[1], v[0])), np.hypot(*v), Wd, colfn, shape, seed)

def feather(img, base, ang_deg, L, Wd, colfn, shape="feather", seed=0.0):
    a = np.radians(ang_deg)
    dx, dy = np.cos(a), np.sin(a)
    # Länge an Texturgrenzen anpassen (2px Rand)
    for _ in range(60):
        ex, ey = base[0] + dx * L, base[1] + dy * L
        if 1 <= ex <= W - 2 and 1 <= ey <= H - 2:
            break
        L *= 0.97
    bx, by, Ls, Ws = base[0] * S, base[1] * S, L * S, Wd * S
    xs_ = [bx, bx + dx * Ls]; ys_ = [by, by + dy * Ls]
    x0 = int(max(0, min(xs_) - Ws - 2)); x1 = int(min(W * S, max(xs_) + Ws + 2))
    y0 = int(max(0, min(ys_) - Ws - 2)); y1 = int(min(H * S, max(ys_) + Ws + 2))
    if x1 <= x0 or y1 <= y0:
        return
    ys, xs = np.mgrid[y0:y1, x0:x1] + 0.5
    rx, ry = xs - bx, ys - by
    s = (rx * dx + ry * dy) / Ls
    d = -rx * dy + ry * dx
    sc = np.clip(s, 0, 1)
    if shape == "leaf":
        w = Ws * np.sin(np.pi * sc) ** 0.75 * (1 - 0.25 * sc)
    elif shape == "shard":                                 # Kristall: gerade Kanten, spitz
        w = Ws * np.minimum(1, sc * 4) * np.clip(1 - sc, 0, 1) ** 0.85 * 1.15
    else:  # Feder: schmaler Kiel, breite Fahne, runde Spitze
        w = Ws * np.sqrt(np.clip(1 - ((sc - 0.55) / 0.55) ** 2, 0, 1)) * (0.55 + 0.45 * np.minimum(1, sc * 3))
    m = (s >= 0) & (s <= 1) & (np.abs(d) < w)
    if not m.any():
        return
    u = np.where(w > 0, d / np.maximum(w, 1e-6), 0)
    col = colfn(sc, u)
    shade = 1 - 0.18 * u ** 2 - 0.08 * u                   # leichte Wölbung, Licht von vorn
    sm = lambda a, b, x: np.clip((x - a) / (b - a), 0, 1) ** 2 * (3 - 2 * np.clip((x - a) / (b - a), 0, 1))
    shade = shade - 0.26 * sm(0.55, 1.0, u) - 0.08 * sm(-0.8, -1.0, u)  # weicher Schatten, wo die Nachbarfeder aufliegt
    rach = (np.abs(d) < 0.22 * S) & (sc < 0.94)            # Kiel / Mittelrippe
    shade = np.where(rach, 1.10, shade)
    shade = shade * (0.9 + 0.1 * sc)                       # Spitzen etwas heller
    if shape == "shard":                                   # Facetten: harte Licht-/Schattenseite
        shade = np.where(u < 0, 1.18 - 0.1 * sc, 0.78 + 0.1 * sc)
        shade = np.where(np.abs(u) > 0.86, 1.35, shade)
        shade = np.where(np.abs(d) < 0.18 * S, 1.4, shade)
    if shape == "feather":                                 # feine Fahnen-Streifen
        barb = 0.035 * np.sin((s * L * 4.5 + np.abs(u) * 9 + seed) * np.pi) * (np.abs(u) > 0.1)
        shade = shade + np.where(rach, 0, barb)
    col = np.clip(col * shade[..., None], 0, 1)
    reg = img[y0:y1, x0:x1]
    reg[m, :3] = col[m]
    reg[m, 3] = 1

def bone_edge(img, x_from, x_to, th, colfn):
    ys, xs = np.mgrid[0:H * S, 0:W * S] + 0.5
    x = xs / S; y = ys / S
    e = edge_y(x)
    t = (x - x_from) / (x_to - x_from)
    thick = th * (1 - 0.55 * np.clip(t, 0, 1))
    v = (y - e) / thick
    m = (np.abs(v) < 1) & (x >= x_from) & (x <= x_to)
    col = colfn(np.clip(t, 0, 1), v, x)
    img[m, :3] = col[m]
    img[m, 3] = 1

def seg_dist(xs, ys, p, q):
    p = np.asarray(p, float) * S; q = np.asarray(q, float) * S
    d = q - p; ll = (d ** 2).sum()
    t = np.clip(((xs - p[0]) * d[0] + (ys - p[1]) * d[1]) / ll, 0, 1)
    return np.hypot(xs - p[0] - t * d[0], ys - p[1] - t * d[1]) / S, t

def finish(img, name, outline=0.55, colors=None, rim_color=None):
    from PIL import ImageFilter
    f = S // OUT_SCALE
    h, w = H * OUT_SCALE, W * OUT_SCALE
    a = img.reshape(h, f, w, f, 4)
    alpha = a[..., 3].mean(axis=(1, 3))
    rgb = (a[..., :3] * a[..., 3:4]).sum(axis=(1, 3)) / np.maximum(a[..., 3].sum(axis=(1, 3)), 1e-6)[..., None]
    al = Image.fromarray((alpha * 255).astype(np.uint8))
    er = np.array(al.filter(ImageFilter.MinFilter(5))) / 255.0      # feiner dunkler Rand
    rim = np.clip(alpha - er, 0, 1)
    if rim_color is not None:
        rgb = rgb * (1 - rim[..., None]) + np.asarray(rim_color) * rim[..., None]
    else:
        rgb = rgb * (1 - rim[..., None] * (1 - outline))
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = np.clip(rgb * 255 + 0.5, 0, 255)
    out[..., 3] = np.clip(alpha * 255 + 0.5, 0, 255)
    im = Image.fromarray(out, "RGBA")
    im.save(os.path.join(OUT, f"{name}.png"))
    return im

def root_mask(R):
    ys, xs = np.mgrid[0:H * S, 0:W * S] + 0.5
    x, y = xs / S, ys / S
    lim = edge_y(x) + 2.5 + H * np.clip(x / R, 0, 1) ** 0.55
    return np.clip(lim - y, 0, 1)

def finish_layers(back, front, name, outline, R, rim_color=None):
    m = root_mask(R)
    back[..., 3] *= m; front[..., 3] *= m
    b = finish(back, name + "_hinten", outline, rim_color=rim_color)
    f = finish(front, name + "_vorne", outline)
    full = b.copy(); full.alpha_composite(f)
    full.save(os.path.join(OUT, name + ".png"))
    return full

# ---------- Feder-Layout (Engel, Phönix, Bambus) ----------
def feather_wing(back, front, cols, shape="feather", leaf_scale=1.0):
    img = back
    prim, sec, cov, mar = cols
    k = 1.15 if shape == "leaf" else 1.0
    n = 10                                                  # Handschwingen: fächern zur Spitze
    for i in range(n):
        t = i / (n - 1)
        x = 60 + 42 * t
        ex = 68 + 54 * t ** 0.85
        ey = 61 - 3 * t - 22 * t ** 3                       # Spitze unten rechts
        feather_to(img, (x, edge_y(x) + 1.5), (ex, ey), 3.7 * k, lambda s, u, t=t: prim(s, u, t), shape, seed=i)
    n = 11                                                  # Armschwingen: runder Hinterrand
    for i in range(n - 1, -1, -1):
        t = i / (n - 1)
        x = 3 + 60 * t
        ex = x - 3 + 2 * t
        ey = 49 + 10 * t ** 1.3                             # Hinterrand steigt zur Hand hin an
        feather_to(img, (x, edge_y(x) + 1.5), (ex, ey), 3.9 * k, lambda s, u, t=t: sec(s, u, t), shape, seed=i * 3)
    img = front
    n = 14                                                  # große Deckfedern
    for i in range(n - 1, -1, -1):
        t = i / (n - 1)
        x = 2 + 96 * t
        feather(img, (x, edge_y(x) + 1), 98 - 46 * t, (21 - 5 * t) * leaf_scale, 3.6 * k,
                lambda s, u, t=t: cov(s, u, t), shape, seed=i * 5)
    n = 18                                                  # kleine Deckfedern
    for i in range(n - 1, -1, -1):
        t = i / (n - 1)
        x = 1 + 100 * t
        feather(img, (x, edge_y(x)), 100 - 46 * t, (11 - 3 * t) * leaf_scale, 2.9 * k,
                lambda s, u, t=t: mar(s, u, t), shape, seed=i * 7)

# ---- Engel: Weiß mit kühlen Schatten, goldene Spitzen ----
def engel():
    back, front = canvas(), canvas()
    WHITE = np.array([0.97, 0.97, 1.0]); COOL = np.array([0.70, 0.76, 0.92])
    GOLD = np.array([1.0, 0.80, 0.32]); GOLD_D = np.array([0.78, 0.52, 0.16])
    def prim(s, u, t):
        c = mix(WHITE, COOL, 0.25 + 0.35 * s)
        return mix(c, GOLD, np.clip((s - 0.78) * 5, 0, 1) * 0.9)
    sec = lambda s, u, t: mix(WHITE, COOL, 0.15 + 0.4 * s)
    cov = lambda s, u, t: mix(WHITE, COOL, 0.1 + 0.25 * s)
    mar = lambda s, u, t: mix(WHITE, GOLD, 0.12 * s) * np.ones_like(s)[..., None]
    feather_wing(back, front, (prim, sec, cov, mar))
    bone_edge(front, 0, 104, 2.3, lambda t, v, x: mix(GOLD, GOLD_D, (v + 1) / 2 * 0.9 + 0 * t)
              + 0.18 * (np.abs(v + 0.45) < 0.25)[..., None])
    return finish_layers(back, front, "engel", 0.62, 16)

# ---- Phönix: Glut von Rot über Orange zu Weißgelb ----
def phoenix():
    back, front = canvas(), canvas()
    RED = np.array([0.62, 0.06, 0.05]); ORA = np.array([1.0, 0.42, 0.05])
    YEL = np.array([1.0, 0.86, 0.30]); HOT = np.array([1.0, 0.98, 0.80])
    def grad(s, k):
        c = mix(RED, ORA, s * 1.6 + k)
        c = mix(c, YEL, (s - 0.55) * 2.6 + k)
        return mix(c, HOT, (s - 0.9) * 8 + k * 0.5)
    prim = lambda s, u, t: grad(s, 0.15 * t)
    sec = lambda s, u, t: grad(s * 0.95, -0.05)
    cov = lambda s, u, t: grad(0.35 + s * 0.5, 0.0)
    mar = lambda s, u, t: grad(0.6 + s * 0.35, 0.05)
    feather_wing(back, front, (prim, sec, cov, mar))
    bone_edge(front, 0, 104, 2.3, lambda t, v, x: mix(HOT, ORA, (v + 1) / 2 + 0 * t))
    return finish_layers(back, front, "phoenix", 0.5, 16)

# ---- Bambus: Bambusblätter am Bambusrohr (passend zum Client-Namen) ----
def bambus():
    back, front = canvas(), canvas()
    DARK = np.array([0.12, 0.36, 0.14]); MID = np.array([0.27, 0.60, 0.22])
    LITE = np.array([0.62, 0.84, 0.32]); TIP = np.array([0.86, 0.90, 0.45])
    def leaf(base_t):
        def f(s, u, t):
            c = mix(DARK, MID, 0.35 + s * 0.9 + base_t)
            c = mix(c, LITE, (s - 0.6) * 1.8)
            return mix(c, TIP, (s - 0.93) * 10)
        return f
    prim = lambda s, u, t: leaf(0.05 * t)(s, u, t)
    sec = lambda s, u, t: leaf(-0.1)(s, u, t)
    cov = lambda s, u, t: leaf(0.15)(s, u, t)
    mar = lambda s, u, t: leaf(0.3)(s, u, t)
    feather_wing(back, front, (prim, sec, cov, mar), shape="leaf")
    STALK = np.array([0.58, 0.72, 0.30]); STALK_D = np.array([0.30, 0.45, 0.14]); NODE = np.array([0.24, 0.32, 0.10])
    def stalk(t, v, x):
        c = mix(STALK, STALK_D, (v + 1) / 2)
        c = c + 0.22 * (np.abs(v + 0.5) < 0.22)[..., None]   # Glanzstreifen
        node = (np.mod(x, 15) < 1.2)                          # Knoten des Rohrs
        return np.where(node[..., None], NODE, c)
    bone_edge(front, 0, 106, 2.8, stalk)
    return finish_layers(back, front, "bambus", 0.55, 16)

# ---- Nachtdrache: Membran mit Fingerknochen ----
def drache():
    return membran("drache", (np.array([0.20, 0.06, 0.30]), np.array([0.46, 0.10, 0.42]), np.array([0.78, 0.20, 0.38])),
                   (np.array([0.30, 0.27, 0.32]), np.array([0.10, 0.08, 0.12]), np.array([0.55, 0.50, 0.58])))

def membran(name, haut, knochen, muster=None, rim_color=None, krallen=True):
    back, front = canvas(), canvas()
    img = back
    J = np.array([44.0, 4.5])                                # Handgelenk
    tips = np.array([(125, 17), (113, 44), (88, 59), (60, 61), (32, 57)], float)
    root_bottom = np.array([1.0, 46.0])
    # Membran-Polygon mit Bögen zwischen den Fingerspitzen
    poly = [(0.0, 13.0)]
    for x in np.linspace(0, J[0], 12): poly.append((x, np.interp(x, [0, 20, 44], [13, 7, 4.5])))
    poly.append(tuple(tips[0]))
    chain = list(tips) + [root_bottom]
    for a, b in zip(chain[:-1], chain[1:]):
        for k in np.linspace(0, 1, 18)[1:]:
            p = a * (1 - k) + b * k
            toward = J - p
            bow = np.sin(np.pi * k) * 0.16 * np.linalg.norm(b - a)
            poly.append(tuple(p + toward / np.linalg.norm(toward) * bow))
    mimg = Image.new("L", (W * S, H * S), 0)
    ImageDraw.Draw(mimg).polygon([(x * S, y * S) for x, y in poly], fill=255)
    m = np.array(mimg) > 127
    ys, xs = np.mgrid[0:H * S, 0:W * S] + 0.5
    dist = np.hypot(xs / S - J[0], ys / S - J[1]) / 60
    C1, C2, C3 = haut
    col = mix(C1, C2, dist * 1.3)
    col = mix(col, C3, (dist - 0.75) * 2.2)
    noise = rng.normal(0, 1, (H // 2 + 1, W // 2 + 1))
    noise = np.array(Image.fromarray(((noise+3)/6*255).clip(0,255).astype(np.uint8)).resize((W*S, H*S), Image.BICUBIC))/255*6-3
    col = col * (1 + 0.04 * noise)[..., None]
    # Falten-Schatten neben jedem Finger + Adern
    bones = [(J, t) for t in tips] + [((0, 13), J)]
    shade = np.ones(m.shape)
    for p, q in bones[:-1]:
        d, tt = seg_dist(xs, ys, p, q)
        shade -= 0.22 * np.exp(-(d / 3.5) ** 2)
        for k in (0.35, 0.6, 0.82):                         # Adern zweigen ab
            src = np.asarray(p) * (1 - k) + np.asarray(q) * k
            for side in (-1, 1):
                dirv = np.asarray(q) - np.asarray(p); nrm = np.array([-dirv[1], dirv[0]]) * side
                dst = src + nrm / np.linalg.norm(nrm) * (6 + 8 * k) + dirv / np.linalg.norm(dirv) * 4
                dv, _ = seg_dist(xs, ys, src, dst)
                shade += 0.16 * np.exp(-(dv / 0.35) ** 2) * (1 - k * 0.5)
    col = np.clip(col * shade[..., None], 0, 1)
    if muster is not None:
        col = muster(xs / S, ys / S, col)
    img[m, :3] = col[m]; img[m, 3] = 1
    # Knochen
    B1, B2, HL = knochen
    for idx, (p, q) in enumerate(bones):
        d, tt = seg_dist(xs, ys, p, q)
        th = (2.6 if idx == len(bones) - 1 else 2.0) * (1 - 0.55 * tt) + 0.5
        bm = d < th
        c = mix(B1, B2, d / th)
        c = np.where((d < th * 0.3)[..., None], HL, c)
        front[bm, :3] = c[bm]; front[bm, 3] = 1
    # Krallen am Handgelenk und an der vordersten Spitze
    for base, ang, ln in (((J, -100, 6.5), (tips[0], 10, 4.0)) if krallen else ()):
        a = np.radians(ang); tip = base + np.array([np.cos(a), np.sin(a)]) * ln
        d, tt = seg_dist(xs, ys, base, tip)
        km = d < (1.6 * (1 - tt) + 0.2)
        front[km, :3] = mix(np.array([0.92, 0.88, 0.80]), np.array([0.55, 0.50, 0.45]), tt)[km]
        front[km, 3] = 1
    return finish_layers(back, front, name, 0.45, 9, rim_color)

C = lambda h: np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)]) / 255.0

def federfluegel(name, prim, sec, cov, mar, bone, shape="feather", bone_to=104, th=2.3, outline=0.55):
    back, front = canvas(), canvas()
    feather_wing(back, front, (prim, sec, cov, mar), shape=shape)
    bone_edge(front, 0, bone_to, th, bone)
    return finish_layers(back, front, name, outline, 16)

def knochen_glanz(hell, dunkel):
    return lambda t, v, x: mix(hell, dunkel, (v + 1) / 2) + 0.18 * (np.abs(v + 0.45) < 0.25)[..., None]

# ---- Seraph: Elfenbein und Gold, leuchtende Spitzen ----
def seraph():
    IV, CR, GO, GD, WH = C("#fff8e6"), C("#f2dfb0"), C("#ffc846"), C("#b9811f"), C("#ffffff")
    prim = lambda s, u, t: mix(mix(IV, CR, 0.2 + 0.5 * s), GO, np.clip((s - 0.62) * 3, 0, 1))
    sec = lambda s, u, t: mix(mix(IV, CR, 0.15 + 0.45 * s), GO, np.clip((s - 0.75) * 3, 0, 0.8))
    cov = lambda s, u, t: mix(GO, CR, 0.2 + 0.6 * s)
    mar = lambda s, u, t: mix(GO, WH, 0.15 + 0.3 * s)
    return federfluegel("seraph", prim, sec, cov, mar, knochen_glanz(C("#fff0a8"), GD), outline=0.6)

# ---- Nachtschwinge: fast schwarze Federn, violett glühende Säume ----
def nachtschwinge():
    BK, DV, VI, GL = C("#0e0814"), C("#2a1240"), C("#7a2ccf"), C("#d58cff")
    def p(s, u, t):
        c = mix(BK, DV, 0.3 + 0.6 * s)
        c = mix(c, VI, np.clip((np.abs(u) - 0.55) * 3, 0, 1) * (0.4 + 0.6 * s))
        return mix(c, GL, np.clip((s - 0.9) * 8, 0, 1))
    cov = lambda s, u, t: mix(mix(BK, DV, 0.4 + 0.5 * s), VI, np.clip((np.abs(u) - 0.6) * 3, 0, 1) * 0.6)
    mar = lambda s, u, t: mix(DV, VI, 0.2 + 0.3 * s)
    return federfluegel("nachtschwinge", p, p, cov, mar, knochen_glanz(C("#6a5a78"), C("#140c1c")), outline=0.4)

# ---- Aurora: Polarlicht-Verlauf über die Spannweite ----
def aurora():
    GR, CY, VI, PK, DK = C("#2bff9a"), C("#2ad8ff"), C("#7a5cff"), C("#ff6ad5"), C("#0c2a3a")
    def span(t, s):
        c = mix(GR, CY, t * 1.8)
        c = mix(c, VI, (t - 0.55) * 2.2)
        c = mix(DK, c, 0.45 + 0.55 * s)
        return mix(c, PK, np.clip((s - 0.88) * 7, 0, 1) * 0.8)
    prim = lambda s, u, t: span(0.5 + 0.5 * t, s)
    sec = lambda s, u, t: span(0.5 * t, s)
    cov = lambda s, u, t: span(0.3 + 0.4 * t, 0.6 + 0.4 * s)
    mar = lambda s, u, t: span(0.2 + 0.5 * t, 0.8 + 0.2 * s)
    return federfluegel("aurora", prim, sec, cov, mar, knochen_glanz(C("#c9fff0"), C("#1b6f8a")))

# ---- Sakura: Kirschblüten-Blätter an einem Kirschzweig ----
def sakura():
    WH, PK, DP, RO = C("#fff4f8"), C("#ffb3d1"), C("#e9709f"), C("#fff0f5")
    def petal(k):
        return lambda s, u, t: mix(mix(DP, PK, 0.3 + 0.9 * s + k), RO, np.clip((s - 0.75) * 3, 0, 1))
    BR, BD, NO = C("#7a4a36"), C("#3a2016"), C("#2a160f")
    def zweig(t, v, x):
        c = mix(BR, BD, (v + 1) / 2) + 0.12 * (np.abs(v + 0.5) < 0.2)[..., None]
        knot = np.mod(x + 5, 23) < 1.6
        return np.where(knot[..., None], NO, c)
    return federfluegel("sakura", petal(0.0), petal(-0.1), petal(0.2), petal(0.35), zweig, shape="leaf", bone_to=106, th=2.5)

# ---- Kristall: Eissplitter mit Facetten ----
def kristall():
    D, M, L, W_ = C("#0b3a6a"), C("#2aa8e6"), C("#9ef0ff"), C("#ffffff")
    shard = lambda k: (lambda s, u, t: mix(mix(D, M, 0.35 + 0.6 * s + k), L, np.clip((s - 0.55) * 2, 0, 1)))
    return federfluegel("kristall", shard(0.1), shard(0.0), shard(0.25), shard(0.4),
                        knochen_glanz(W_, C("#3a7fb8")), shape="shard", outline=0.9)

# ---- Cyber: dunkle Haut mit Neon-Gitter und Leuchtrand ----
def cyber():
    NE = C("#3dff8a")
    def gitter(x, y, col):
        g = np.zeros_like(x)
        for a in (0, 60, 120):
            r = np.radians(a)
            d = (x * np.cos(r) + y * np.sin(r)) / 7.0
            g = np.maximum(g, np.exp(-((d - np.round(d)) / 0.05) ** 2))
        puls = 0.55 + 0.45 * np.sin(x * 0.12)
        return col * (1 - g[..., None] * 0.6) + NE * (g * puls * 0.85)[..., None]
    return membran("cyber", (C("#050a14"), C("#0a1a2a"), C("#0d3a3a")),
                   (C("#1e2a36"), C("#05080c"), NE), muster=gitter, rim_color=NE, krallen=False)

# ---- Schmetterling: Vorder- und Hinterflügel mit Adern, Saum und Augenfleck ----
def schmetterling():
    from PIL import ImageFilter
    back, front = canvas(), canvas()
    ys, xs = np.mgrid[0:H * S, 0:W * S] + 0.5
    x, y = xs / S, ys / S
    def poly_mask(pts):
        im = Image.new("L", (W * S, H * S), 0)
        ImageDraw.Draw(im).polygon([(px * S, py * S) for px, py in pts], fill=255)
        return np.array(im) > 127, im
    tt = np.linspace(0, 1, 40)
    vorne_pts = [(1, 12)] + [(1 + 121 * t, 12 - 8 * np.sin(np.pi * t * 0.9) + 6 * t ** 3) for t in tt] + \
                [(122 - 14 * t, 18 + 16 * np.sin(np.pi * t * 0.5)) for t in tt] + \
                [(108 - 104 * t, 34 - 6 * np.sin(np.pi * t) - 4 * t) for t in tt]
    hinten_pts = [(2, 24)] + [(2 + 70 * t, 24 + 4 * np.sin(np.pi * t)) for t in tt] + \
                 [(72 + 14 * np.sin(np.pi * t), 28 + 32 * t) for t in tt] + \
                 [(72 - 64 * t, 60 - 8 * t ** 2 + 2 * np.sin(np.pi * t * 3)) for t in tt] + [(4, 42)]
    OR, YE, BK, WH, BL = C("#ff7a12"), C("#ffd54a"), C("#120c08"), C("#fffbea"), C("#2a7bff")
    for pts, base_d in ((hinten_pts, 0.0), (vorne_pts, 0.0)):
        m, im = poly_mask(pts)
        er = np.array(im.filter(ImageFilter.MinFilter(4 * S + 1))) > 127
        dist = np.hypot(x - 2, y - 20) / 120
        col = mix(OR, YE, dist * 1.6 + 0.1)
        saum = m & ~er
        col = np.where(saum[..., None], BK, col)
        back[m, :3] = col[m]; back[m, 3] = 1
        # weiße Punkte im Saum
        for k in range(0, len(pts), 5):
            px, py = pts[k]
            cxp, cyp = px * 0.94 + 2 * 0.06, py * 0.94 + 20 * 0.06
            dm = (np.hypot(x - cxp, y - cyp) < 1.3) & saum
            back[dm, :3] = WH
    # Augenfleck im Hinterflügel
    r = np.hypot(x - 52, y - 46)
    for rad, c in ((7.5, BK), (6, YE), (4.2, BL), (2.2, BK), (0.9, WH)):
        mm = (r < rad) & (back[..., 3] > 0)
        back[mm, :3] = c
    # Adern auf der vorderen Ebene
    for ex, ey in [(118, 14), (110, 26), (96, 33), (72, 31), (48, 31), (66, 40), (78, 52), (60, 59), (36, 60), (18, 52)]:
        d, tt2 = seg_dist(xs, ys, (2, 20), (ex, ey))
        vm = d < 0.55 * (1 - 0.5 * tt2)
        front[vm, :3] = BK; front[vm, 3] = 1
    front[..., 3] *= (back[..., 3] > 0)                    # Adern nur auf dem Flügel
    bone_edge(front, 0, 110, 1.3, lambda t, v, xx: mix(C("#3a2a1a"), BK, (v + 1) / 2))
    return finish_layers(back, front, "schmetterling", 0.35, 9)

ALLE = dict(engel=engel, drache=drache, cyber=cyber, schmetterling=schmetterling, phoenix=phoenix,
            seraph=seraph, nachtschwinge=nachtschwinge, kristall=kristall, aurora=aurora, sakura=sakura, bambus=bambus)
for name, f in ALLE.items():
    if not NUR or name in NUR:
        f()
print("fertig:", sorted(os.listdir(OUT)))

"""Bambus Client 3.3 – Umhänge in HD für den wehenden Umhang-Renderer.

Je Umhang entstehen:
  <id>.png       1280 x 640, Minecraft-Cape-Layout (64 x 32 Raster, 20-fach):
                 Außenseite (1,1)-(11,17), Innenseite (12,1)-(22,17), Kanten, Elytra-Bereich
  <id>_anim.png  200 x 320, nahtlos kachelbare Animations-Ebene (wird im Spiel verschoben)
Die Laufrichtung der Animation steht in Kosmetik.CAPES (animX / animY).

Aufruf: python3 capes_hd.py <zielordner> [id ...]"""
import numpy as np
from PIL import Image
import os, sys

W, H = 320, 512
S = 2
OUT = sys.argv[1] if len(sys.argv) > 1 else "capes"
NUR = set(sys.argv[2:])
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(11)

def grid(s=S):
    ys, xs = np.mgrid[0:H * s, 0:W * s] + 0.5
    return xs / s, ys / s

def mix(a, b, t):
    t = np.clip(t, 0, 1)
    if np.ndim(t): t = t[..., None]
    return np.asarray(a, float) * (1 - t) + np.asarray(b, float) * t

def hexc(h):
    h = h.lstrip("#"); return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)]) / 255

def periodic_noise(h, w, sy, sx, seed):
    """Glattes Rauschen (Strukturgröße sy x sx Pixel), nahtlos an allen Rändern."""
    r = np.random.default_rng(seed).normal(size=(h, w))
    fy = np.fft.fftfreq(h)[:, None]; fx = np.fft.fftfreq(w)[None, :]
    f = np.exp(-0.5 * ((fy * sy * 2.5) ** 2 + (fx * sx * 2.5) ** 2))
    n = np.real(np.fft.ifft2(np.fft.fft2(r) * f))
    return (n - n.min()) / (n.max() - n.min())

def down(img):
    return img.reshape(H, S, W, S, -1).mean(axis=(1, 3))

ERGEBNIS = {}
def save(rgb, name, alpha=None):
    a = np.ones(rgb.shape[:2]) if alpha is None else alpha
    out = np.dstack([np.clip(rgb, 0, 1), np.clip(a, 0, 1)])
    ERGEBNIS[name] = Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA")

def border(rgb, x, y, c_out, c_in, w=7):
    d = np.minimum(np.minimum(x, W - x), np.minimum(y, H - y))
    band = d < w
    t = d / w
    col = mix(c_out, c_in, t)
    col = col * (1 + 0.25 * np.exp(-((t - 0.35) / 0.12) ** 2))[..., None]   # Glanzlinie
    rgb[band] = col[band]
    inner = (d >= w) & (d < w + 2)
    rgb[inner] *= 0.45
    return rgb

def leaf(rgb, x, y, base, ang, L, Wd, c1, c2):
    a = np.radians(ang); dx, dy = np.cos(a), np.sin(a)
    rx, ry = x - base[0], y - base[1]
    s = (rx * dx + ry * dy) / L; d = -rx * dy + ry * dx
    sc = np.clip(s, 0, 1)
    w = Wd * np.sin(np.pi * sc) ** 0.8 * (1 - 0.3 * sc)
    m = (s > 0) & (s < 1) & (np.abs(d) < w)
    if not m.any(): return
    u = d / np.maximum(w, 1e-6)
    col = mix(c1, c2, 0.3 + 0.7 * sc)
    sh = 1 - 0.25 * u - 0.1 * u ** 2
    sh = np.where(np.abs(d) < 0.6, 1.25, sh)
    rgb[m] = (col * sh[..., None])[m]

# ---------------- Bambuswald ----------------
def bambus():
    x, y = grid()
    top, bot = hexc("#1b4a26"), hexc("#06140b")
    rgb = mix(top, bot, (y / H) ** 0.8)
    glow = np.exp(-(((x - W * 0.5) / 120) ** 2 + ((y + 40) / 260) ** 2))
    rgb = rgb + glow[..., None] * hexc("#3d8a3a") * 0.55
    # Lichtstrahlen
    ray = (np.sin((x - W / 2) / (y + 80) * 9) * 0.5 + 0.5) ** 6 * np.exp(-y / 300)
    rgb = rgb + ray[..., None] * hexc("#9be36b") * 0.12
    stalks = [  # x, Breite, Tiefe(0 hinten .. 1 vorn), Neigung
        (40, 16, 0.15, 0.03), (250, 14, 0.1, -0.02), (120, 18, 0.35, -0.015), (205, 20, 0.4, 0.02),
        (70, 28, 0.75, 0.01), (265, 30, 0.8, -0.01), (165, 34, 1.0, 0.0)]
    for sx, sw, depth, lean in stalks:
        cx = sx + lean * (y - H / 2)
        u = (x - cx) / (sw / 2)
        m = np.abs(u) < 1
        light = mix(hexc("#1f4a20"), hexc("#a7d65a"), 0.25 + 0.75 * depth)
        dark = light * 0.35
        col = mix(light, dark, (u + 1) / 2 * 0.9)
        col = col + 0.35 * np.exp(-((u + 0.45) / 0.18) ** 2)[..., None] * light   # Glanz
        seg = 60 + 30 * depth
        ph = (y + sx * 3) % seg
        node = ph < 4
        col = np.where(node[..., None], dark * 0.8, col)
        col = col + (np.abs(ph - 5) < 1.2)[..., None] * light * 0.35
        rgb[m] = col[m]
        # Blätter an einigen Knoten
        for k in range(int(H / seg) + 1):
            ny = k * seg - (sx * 3) % seg
            if ny < 20 or ny > H - 30 or (k + int(sx)) % 2: continue
            side = 1 if (k + int(sx / 10)) % 3 else -1
            nx = sx + lean * (ny - H / 2)
            for j, (ang, L) in enumerate(((-25, 70), (5, 80), (35, 62))):
                a = (ang if side > 0 else 180 - ang)
                leaf(rgb, x, y, (nx + side * sw * 0.4, ny), a + 18 * side, L * (0.6 + 0.5 * depth), 7 + 5 * depth,
                     mix(hexc("#174018"), hexc("#3f8a2c"), depth), mix(hexc("#3f8a2c"), hexc("#c8ee7a"), depth))
    vign = 1 - 0.45 * (((x - W / 2) / (W * 0.7)) ** 2 + ((y - H / 2) / (H * 0.75)) ** 2)
    rgb = rgb * vign[..., None]
    rgb = border(rgb, x, y, hexc("#e8ff9a"), hexc("#5f9a2a"))
    save(down(rgb), "bambus")
    # Animation: Glühwürmchen-Schimmer, kachelbar in y (steigt auf)
    n = periodic_noise(H, W, 7, 7, 5)
    sp = np.clip((n - 0.72) / 0.2, 0, 1) ** 2
    an = np.zeros((H, W, 3)) + hexc("#d8ff8a")
    save(an, "bambus_anim", alpha=sp * 0.9)

# ---------------- Polarlicht ----------------
def polarlicht():
    x, y = grid()
    rgb = mix(hexc("#03061a"), hexc("#1b0f3c"), (y / H) ** 1.2)
    # Sterne
    st = np.zeros((H, W)); idx = rng.integers(0, H * W, 420)
    st.flat[idx] = rng.random(420) ** 3
    st = np.kron(st, np.ones((S, S)))
    rgb = rgb + st[..., None] * 1.2
    for cxs, cys in rng.integers(20, [W - 20, int(H * 0.6)], (6, 2)):
        g = np.exp(-(((x - cxs) ** 2 + (y - cys) ** 2) / 6))
        cr = np.exp(-np.abs(x - cxs) / 1.2) * np.exp(-np.abs(y - cys) / 12) + np.exp(-np.abs(y - cys) / 1.2) * np.exp(-np.abs(x - cxs) / 12)
        rgb = rgb + (g + 0.5 * cr)[..., None] * np.array([0.9, 0.95, 1.0])
    # Polarlicht-Bänder
    streak = periodic_noise(H, W, 90, 5, 3)
    streak = np.kron(streak, np.ones((S, S)))
    for c0, amp, f, ph, w, ca, cb in ((150, 40, 0.018, 0.5, 38, "#36ffb2", "#00c8ff"),
                                      (250, 30, 0.024, 2.0, 30, "#7dff6a", "#36ffb2"),
                                      (90, 25, 0.03, 4.0, 22, "#ff5ad8", "#8a5cff")):
        cy = c0 + amp * np.sin(x * f + ph)
        dy = y - cy
        curtain = np.where(dy > 0, np.exp(-(dy / (w * 2.2)) ** 2), np.exp(-(dy / (w * 0.35)) ** 2))
        inten = curtain * (0.35 + 0.9 * streak)
        col = mix(hexc(ca), hexc(cb), np.clip(-dy / w * 0.5 + 0.5, 0, 1))
        rgb = 1 - (1 - rgb) * (1 - (col * inten[..., None]) * 0.8)        # Screen-Mischung
    # Berg- und Wald-Silhouette unten
    ridge = H - 95 - 35 * np.sin(x * 0.02 + 1) - 18 * np.sin(x * 0.07)
    rgb = np.where((y > ridge)[..., None], hexc("#0a0b22") * 1.3, rgb)
    for tx in range(-10, W + 20, 16):
        th = 40 + (tx * 37 % 30); tb = H - 20 - (tx * 13 % 14)
        m = (y < tb) & (y > tb - th) & (np.abs(x - tx) < (y - (tb - th)) / th * 9)
        rgb[m] = hexc("#04040f")
    rgb = np.where((y > H - 24)[..., None], hexc("#04040f"), rgb)
    rgb = border(rgb, x, y, hexc("#e9f0ff"), hexc("#6a72a8"))
    save(down(rgb), "polarlicht")
    # Animation: wandernde Lichtvorhänge, kachelbar in x
    n = periodic_noise(H, W, 70, 9, 9)
    ys = np.arange(H)[:, None] / H
    alpha = n ** 2.2 * np.exp(-((ys - 0.35) / 0.25) ** 2) * 0.85
    an = mix(hexc("#5affc0"), hexc("#ff7ae6"), np.clip((0.3 - ys) * 3, 0, 1) * np.ones((1, W)))
    save(an, "polarlicht_anim", alpha=alpha)

# ---------------- Glut ----------------
def fire_pal(t):
    stops = [(0, "#0a0204"), (0.25, "#5a0a0a"), (0.5, "#c3260a"), (0.7, "#ff7a12"), (0.88, "#ffd34a"), (1.0, "#fff6d0")]
    out = np.zeros(t.shape + (3,))
    for (a, ca), (b, cb) in zip(stops[:-1], stops[1:]):
        m = (t >= a) & (t <= b)
        out[m] = mix(hexc(ca), hexc(cb), (t[m] - a) / (b - a))
    return out

def glut():
    x, y = grid()
    n1 = np.kron(periodic_noise(H, W, 34, 13, 21), np.ones((S, S)))
    n2 = np.kron(periodic_noise(H, W, 70, 50, 22), np.ones((S, S)))
    yy = y / H
    heat = (yy ** 1.5) * 1.15 + (n1 - 0.5) * 1.1 * (0.25 + yy) + (n2 - 0.5) * 0.35
    rgb = fire_pal(np.clip(heat, 0, 1))
    cx, cy = W / 2, H * 0.4
    halo = np.exp(-(((x - cx) / 70) ** 2 + ((y - cy) / 90) ** 2))
    rgb = rgb + halo[..., None] * hexc("#ff9a30") * 0.25
    # Funken
    sp = np.zeros((H, W)); idx = rng.integers(0, H * W, 260); sp.flat[idx] = rng.random(260) ** 2
    sp = np.kron(sp, np.ones((S, S))) * (1 - yy)
    rgb = rgb + sp[..., None] * hexc("#ffd070") * 1.5
    rgb = border(rgb, x, y, hexc("#ffe28a"), hexc("#7a2a08"))
    save(down(rgb), "glut")
    # Animation: aufsteigende Flammen, kachelbar in y
    n = periodic_noise(H, W, 30, 11, 31)
    ys = np.arange(H)[:, None] / H
    alpha = np.clip((n - 0.45) * 2.2, 0, 1) ** 1.5 * 0.7
    an = fire_pal(np.clip(0.6 + 0.4 * n, 0, 1))
    save(an, "glut_anim", alpha=alpha * 0.9)

# ---------------- Neon-Platine ----------------
def neon():
    x, y = grid()
    r = np.random.default_rng(41)
    rgb = mix(hexc("#04140c"), hexc("#020805"), y / H)
    NE = hexc("#3dff8a")
    spur = np.zeros(x.shape)
    for _ in range(26):                                     # Leiterbahnen: gerade + 45°-Knicke
        px, py = r.integers(10, W - 10), r.integers(10, H - 10)
        for _ in range(r.integers(3, 6)):
            d = r.choice([(0, 1), (0, -1), (1, 0), (-1, 0), (1, 1), (-1, 1), (1, -1), (-1, -1)])
            ln = r.integers(25, 90)
            qx, qy = np.clip(px + d[0] * ln, 8, W - 8), np.clip(py + d[1] * ln, 8, H - 8)
            vx, vy = qx - px, qy - py; ll = max(vx * vx + vy * vy, 1)
            t = np.clip(((x - px) * vx + (y - py) * vy) / ll, 0, 1)
            dd = np.hypot(x - px - t * vx, y - py - t * vy)
            spur = np.maximum(spur, np.exp(-(dd / 1.3) ** 2))
            px, py = qx, qy
        dd = np.hypot(x - px, y - py)
        spur = np.maximum(spur, ((dd < 4.5) & (dd > 2.2)) * 1.0)
    halo = np.array(Image.fromarray((spur * 255).astype(np.uint8)).filter(__import__("PIL.ImageFilter", fromlist=["x"]).GaussianBlur(9))) / 255
    rgb = rgb + NE * (spur * 0.95 + halo * 0.9)[..., None]
    # Chip in der Mitte
    cx, cy = W / 2, H * 0.42
    chip = (np.abs(x - cx) < 46) & (np.abs(y - cy) < 46)
    rgb[chip] = hexc("#0b0f0d")
    pins = (((np.abs(np.abs(x - cx) - 52) < 6) & (np.abs(y - cy) < 42)) | ((np.abs(np.abs(y - cy) - 52) < 6) & (np.abs(x - cx) < 42))) & \
           (np.mod(np.where(np.abs(x - cx) > 46, y, x), 10) < 5)
    rgb[pins] = hexc("#b8c8bc")
    logo = (np.abs(x - cx) < 30) & (np.abs(y - cy) < 30) & (np.abs(np.abs(x - cx) + np.abs(y - cy) - 22) < 3)
    rgb[logo] = NE * 1.2
    rgb = border(rgb, x, y, NE, hexc("#0f5a30"))
    save(down(rgb), "neon")
    ys = np.arange(H)[:, None] / H
    band = np.exp(-((np.mod(ys, 1.0) - 0.5) / 0.06) ** 2) * np.ones((1, W))
    save(np.zeros((H, W, 3)) + NE, "neon_anim", alpha=band * 0.45)

# ---------------- Cyber ----------------
def cyber():
    x, y = grid()
    CY, PK = hexc("#00e5ff"), hexc("#ff2bd6")
    rgb = mix(hexc("#04101e"), hexc("#0a0418"), y / H)
    # Sechseck-Gitter
    q = (2 / 3 * x) / 22; rr = (-1 / 3 * x + np.sqrt(3) / 3 * y) / 22
    cz = -q - rr
    rq, rr2, rz = np.round(q), np.round(rr), np.round(cz)
    dq, dr, dz = np.abs(rq - q), np.abs(rr2 - rr), np.abs(rz - cz)
    edge = np.maximum(np.maximum(dq, dr), dz)
    hexl = np.exp(-((edge - 0.5) / 0.03) ** 2)
    fade = np.clip(1 - y / H * 1.1, 0.15, 1)
    rgb = rgb + CY * (hexl * 0.35 * fade)[..., None]
    # Diagonale Streifen unten
    st = (np.mod(x + y, 26) < 9) & (y > H * 0.78)
    rgb[st] = rgb[st] * 0.5 + PK * 0.45
    # HUD-Kreis mit Dreieck
    cx, cy = W / 2, H * 0.38
    rd = np.hypot(x - cx, y - cy)
    ring = np.exp(-((rd - 70) / 2.2) ** 2) + 0.6 * np.exp(-((rd - 82) / 1.2) ** 2) * (np.mod(np.degrees(np.arctan2(y - cy, x - cx)) + 360, 30) < 18)
    tri = (y - cy < 40) & (y - cy > -46 + 0) & (np.abs(x - cx) < (y - cy + 46) * 0.62)
    tri_in = (y - cy < 30) & (np.abs(x - cx) < (y - cy + 30) * 0.62)
    rgb = rgb + CY * ring[..., None]
    rgb[tri & ~tri_in] = CY * 1.1
    rgb[tri_in] = rgb[tri_in] * 0.6 + PK * 0.25
    halo = np.exp(-((rd - 60) / 40) ** 2) * 0.25
    rgb = rgb + CY * halo[..., None]
    # Eck-Klammern
    for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        bx, by = W / 2 + sx * (W / 2 - 26), H / 2 + sy * (H / 2 - 26)
        k = ((np.abs(y - by) < 2) & (np.abs(x - (bx - sx * 14)) < 14)) | ((np.abs(x - bx) < 2) & (np.abs(y - (by - sy * 14)) < 14))
        rgb[k] = CY
    rgb = border(rgb, x, y, CY, hexc("#0a3a5a"))
    save(down(rgb), "cyber")
    ys = np.arange(H)[:, None]
    lines = ((np.mod(ys, 8) < 2) * 0.25 + np.exp(-((np.mod(ys / H, 1) - 0.5) / 0.03) ** 2) * 0.6) * np.ones((1, W))
    save(np.zeros((H, W, 3)) + CY, "cyber_anim", alpha=lines * 0.6)

# ---------------- Galaxie ----------------
def galaxie():
    x, y = grid()
    n1 = np.kron(periodic_noise(H, W, 60, 60, 51), np.ones((S, S)))
    n2 = np.kron(periodic_noise(H, W, 25, 25, 52), np.ones((S, S)))
    rgb = mix(hexc("#04020c"), hexc("#0a0420"), y / H)
    neb = np.clip((n1 - 0.45) * 2.5, 0, 1) * (0.6 + 0.6 * n2)
    rgb = rgb + mix(hexc("#7a2cff"), hexc("#ff4fa8"), n2)[...] * (neb * 0.55)[..., None]
    rgb = rgb + hexc("#2a6bff") * (np.clip((n2 - 0.55) * 3, 0, 1) * 0.35)[..., None]
    # Spiralgalaxie
    cx, cy = W / 2, H * 0.45
    dx, dy = (x - cx), (y - cy) / 0.62
    rr = np.hypot(dx, dy) + 1e-3; th = np.arctan2(dy, dx)
    arms = np.cos(2 * (th - np.log(rr) * 2.2)) * 0.5 + 0.5
    gal = (arms ** 3 * np.exp(-rr / 70) + np.exp(-rr / 14) * 1.4)
    rgb = rgb + mix(hexc("#b8c8ff"), hexc("#fff0d0"), np.exp(-rr / 25))[...] * gal[..., None]
    st = np.zeros((H, W)); idx = rng.integers(0, H * W, 700); st.flat[idx] = rng.random(700) ** 4
    rgb = rgb + np.kron(st, np.ones((S, S)))[..., None] * 1.3
    rgb = border(rgb, x, y, hexc("#d9c4ff"), hexc("#4a2a8a"))
    save(down(rgb), "galaxie")
    n = periodic_noise(H, W, 3, 3, 53)
    sp = np.clip((n - 0.8) / 0.15, 0, 1) ** 2
    save(np.zeros((H, W, 3)) + hexc("#e8e0ff"), "galaxie_anim", alpha=sp)

# ---------------- Synthwave ----------------
def sunset():
    x, y = grid()
    hz = H * 0.62
    sky = mix(hexc("#12052e"), hexc("#7a1a7a"), y / hz)
    sky = mix(sky, hexc("#ff6a3d"), (y - hz * 0.6) / (hz * 0.4))
    rgb = sky.copy()
    cx, cy, R0 = W / 2, hz - 40, 92
    rd = np.hypot(x - cx, y - cy)
    sun = rd < R0
    scol = mix(hexc("#ffe066"), hexc("#ff2b8a"), (y - (cy - R0)) / (2 * R0))
    cut = (y > cy - 10) & (np.mod(y - cy + 10, 16) < (2 + (y - cy) / 12))
    rgb[sun & ~cut] = scol[sun & ~cut]
    rgb = rgb + hexc("#ff4fa8") * (np.exp(-((rd - R0) / 18) ** 2) * 0.35 * (rd > R0))[..., None]
    ridge = hz - 30 - 26 * np.abs(np.sin(x * 0.03 + 0.6)) - 14 * np.abs(np.sin(x * 0.081))
    mnt = (y > ridge) & (y < hz)
    rgb[mnt] = hexc("#1a0630")
    ed = np.abs(y - ridge) < 1.6
    rgb[ed & (y < hz)] = hexc("#ff3dd8")
    fl = y >= hz
    rgb[fl] = mix(hexc("#1a0630"), hexc("#05010f"), (y - hz) / (H - hz))[fl]
    depth = (y - hz) / (H - hz) + 1e-3
    gx = (x - cx) / (depth * 40)
    lines = (np.abs(gx - np.round(gx)) < 0.04 / depth * 0.6) | (np.abs(np.mod(1 / depth * 2.2, 1) - 0.5) > 0.47)
    rgb[fl & lines] = hexc("#ff2bd6") * 1.1
    st = np.zeros((H, W)); idx = rng.integers(0, H * W, 220); st.flat[idx] = rng.random(220) ** 3
    st = np.kron(st, np.ones((S, S))) * (y < hz - 120)
    rgb = rgb + st[..., None]
    rgb = border(rgb, x, y, hexc("#ff7ae6"), hexc("#5a0f6a"))
    save(down(rgb), "sunset")
    n = periodic_noise(H, W, 3, 3, 61)
    ys = np.arange(H)[:, None] / H
    sp = np.clip((n - 0.78) / 0.15, 0, 1) ** 2 * (ys < 0.4)
    save(np.zeros((H, W, 3)) + hexc("#ffd0f5"), "sunset_anim", alpha=sp)

# ---------------- Matrix ----------------
GLYPH = np.random.default_rng(77).random((64, 7, 5)) > 0.52
def glyph_feld(r, cols, rows, cw, ch):
    out = np.zeros((rows * ch, cols * cw))
    ids = r.integers(0, 64, (rows, cols))
    for j in range(rows):
        for i in range(cols):
            g = GLYPH[ids[j, i]]
            gi = np.kron(g, np.ones((ch // 8, cw // 6)))
            out[j * ch + 1:j * ch + 1 + gi.shape[0], i * cw + 1:i * cw + 1 + gi.shape[1]] = gi
    return out

def matrix():
    r = np.random.default_rng(81)
    cw, ch = 12, 16
    cols, rows = W // cw + 1, H // ch + 1
    gf = glyph_feld(r, cols, rows, cw, ch)[:H, :W]
    hell = np.zeros((rows, cols))
    for i in range(cols):
        head = r.integers(0, rows); ln = r.integers(5, 16)
        for j in range(rows):
            d = (head - j) % rows
            hell[j, i] = max(0, 1 - d / ln) if d < ln else 0.08
    hm = np.kron(hell, np.ones((ch, cw)))[:H, :W]
    G = hexc("#39ff14")
    rgb = np.zeros((H, W, 3)) + hexc("#010802")
    rgb = rgb + G * (gf * hm * 0.95)[..., None] + hexc("#d8ffd0") * (gf * (hm > 0.93))[..., None] * 0.8
    rgb = np.array(Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8)).resize((W * S, H * S), Image.NEAREST)) / 255
    x, y = grid()
    rgb = border(rgb, x, y, G, hexc("#0a4a10"))
    save(down(rgb), "matrix")
    gf2 = glyph_feld(np.random.default_rng(82), cols, rows * 2, cw, ch)[:H, :W]
    tr = np.zeros((H, W))
    for i in range(cols):
        ph = r.random() * H; ln = 60 + r.random() * 120
        yy = np.arange(H)
        d = (ph - yy) % H
        tr[:, i * cw:(i + 1) * cw] = (np.clip(1 - d / ln, 0, 1) ** 1.5)[:, None]
    save(np.zeros((H, W, 3)) + hexc("#7dff5a"), "matrix_anim", alpha=gf2 * tr)

# ---------------- in Minecraft-Cape-Layout packen ----------------
SC = 20
DESIGNS = dict(bambus=(bambus, "#0e2a16", "#7fc243"), neon=(neon, "#04120a", "#3dff8a"), cyber=(cyber, "#06101e", "#00e5ff"),
               galaxie=(galaxie, "#0c0620", "#b48cff"), sunset=(sunset, "#1a0630", "#ff5ad8"), matrix=(matrix, "#010a03", "#39ff14"),
               polarlicht=(polarlicht, "#0a0a24", "#c8d4ff"), glut=(glut, "#2a0604", "#ffb040"))

def pack(name, innen, kante):
    front = ERGEBNIS[name].convert("RGB")
    out = Image.new("RGBA", (64 * SC, 32 * SC), (0, 0, 0, 0))
    out.paste(front.resize((10 * SC, 16 * SC), Image.LANCZOS), (1 * SC, 1 * SC))
    ic, kc = hexc(innen), hexc(kante)
    gy = np.linspace(0, 1, 16 * SC)[:, None, None]
    inner = (ic * (1.25 - 0.5 * gy) * np.ones((1, 10 * SC, 1)))
    rn = np.random.default_rng(5).normal(0, 0.015, inner.shape[:2])[..., None]
    inner = np.clip(inner + rn, 0, 1)
    out.paste(Image.fromarray((inner * 255).astype(np.uint8), "RGB"), (12 * SC, 1 * SC))
    def fl(w, h, c, k=1.0):
        return Image.fromarray((np.clip(np.ones((h, w, 1)) * c * k, 0, 1) * 255).astype(np.uint8)[..., :3], "RGB")
    out.paste(fl(SC, 16 * SC, kc, 0.8), (0, SC)); out.paste(fl(SC, 16 * SC, kc, 0.8), (11 * SC, SC))
    out.paste(fl(10 * SC, SC, kc, 0.9), (SC, 0)); out.paste(fl(10 * SC, SC, kc, 0.7), (11 * SC, 0))
    # Elytra-Bereich (22,0)-(46,22): Design, damit auch die Elytra passt
    out.paste(front.resize((24 * SC, 22 * SC), Image.LANCZOS), (22 * SC, 0))
    out.save(os.path.join(OUT, name + ".png"), optimize=True)
    an = ERGEBNIS[name + "_anim"].resize((10 * SC, 16 * SC), Image.BOX)
    an.save(os.path.join(OUT, name + "_anim.png"), optimize=True)
    front.resize((320, 512)).save(os.path.join(OUT, "_vorschau_" + name + ".png"))

for name, (f, innen, kante) in DESIGNS.items():
    if not NUR or name in NUR:
        f()
        pack(name, innen, kante)
print("fertig:", sorted(os.listdir(OUT)))

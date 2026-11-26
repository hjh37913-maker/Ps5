#!/usr/bin/env python3
"""
Asset generator v2 — PS5 Interface Simulator.

Original, procedurally generated key art (never Sony/publisher material) with a
composed cinematic look: sky gradients, atmospheric haze, layered terrains,
neon city studies, inked skies, floating objects. Deterministic per game id.

Outputs (app/src/main/assets):
  games/<id>/cover.jpg       512x512    portrait poster (title lockup included)
  games/<id>/card.jpg        480x480    switcher tile art
  games/<id>/background.jpg 1280x720    spotlight artwork (no lockup; UI draws title)
  games/<id>/shot1..3.jpg    800x450    gallery screenshots
  games/<id>/hero.png        960x540    transparent logo-free emblem (hub use)
  tiles/store.jpg            480x480    PlayStation Store tile art
  sounds/*.wav                          synthesized UI feedback
  fonts/*.ttf                           OFL fonts (Manrope / Figtree)
  catalog.json                          metadata

Usage: python3 tools/gen_assets.py [--only images|sounds|fonts] [--game id]
"""
import json
import math
import os
import random
import struct
import sys
import wave

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(ROOT, "app", "src", "main")
A = os.path.join(MAIN, "assets")
OUT_GAMES = os.path.join(A, "games")
OUT_SOUND = os.path.join(A, "sounds")
OUT_FONT = os.path.join(A, "fonts")
OUT_TILES = os.path.join(A, "tiles")

FD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts")
def F(weight, size):
    names = {400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold", 800: "ExtraBold"}
    w = min(names, key=lambda k: abs(k - weight))
    fam = "Manrope" if weight >= 700 else "Figtree"
    if fam == "Manrope":
        w = max(600, w)
    try:
        return ImageFont.truetype(f"{FD}/{fam}-{names[w]}.ttf", size)
    except OSError:
        return ImageFont.load_default()

# id, title, kind, accent, deep, sky, genre, publisher, year, sizeGb, description, tagline
GAMES = [
    ("gtavi", "Grand Theft Auto VI", "neon", (255, 108, 170), (40, 10, 40),
     ((255, 140, 180), (120, 40, 130), (24, 6, 34)), "Action-Adventure", "Rockstar Games", 2026, 112.0,
     "Vice City, Leonida. Two outlaws, one state, and no way back.", "VICE CITY"),
    ("gtav", "Grand Theft Auto V", "neon", (255, 186, 92), (30, 40, 60),
     ((255, 196, 120), (150, 110, 120), (18, 24, 40)), "Action-Adventure", "Rockstar Games", 2013, 96.0,
     "Three criminals, one sun-bleached city, one perfect score.", "LOS SANTOS"),
    ("gowragnarok", "God of War Ragnarok", "terrain", (150, 200, 235), (18, 28, 44),
     ((190, 225, 245), (90, 130, 175), (10, 16, 28)), "Action-Adventure", "Santa Monica Studio", 2022, 84.0,
     "Kratos and Atreus face the prophesied end of the Norse realms.", "NINE REALMS"),
    ("spiderman2", "Marvel's Spider-Man 2", "city", (235, 52, 62), (16, 20, 44),
     ((255, 120, 110), (120, 40, 70), (10, 10, 26)), "Action-Adventure", "Insomniac Games", 2023, 92.0,
     "Two heroes. One city. A darkness that bonds with both.", "BE GREATER"),
    ("tlou2", "The Last of Us Part II", "terrain", (176, 190, 150), (26, 30, 26),
     ((186, 200, 168), (96, 112, 92), (18, 20, 18)), "Action-Adventure", "Naughty Dog", 2020, 78.0,
     "Ellie's hunt for justice cuts across a broken, overgrown America.", "PART II"),
    ("ghostyotei", "Ghost of Yotei", "terrain", (235, 196, 140), (28, 22, 30),
     ((250, 210, 160), (170, 90, 80), (20, 12, 22)), "Action-Adventure", "Sucker Punch Productions", 2025, 88.0,
     "A new hero, a new chapter, beneath the shadow of Mount Yotei.", "YOTEI"),
    ("astrobot", "Astro Bot", "space", (120, 210, 255), (16, 40, 74),
     ((120, 210, 255), (60, 90, 200), (8, 12, 40)), "Platformer", "Team Asobi", 2024, 66.0,
     "A galaxy-sized rescue mission for the most loyal little robot alive.", "GALAXY"),
    ("minecraft", "Minecraft", "voxel", (120, 190, 105), (30, 40, 30),
     ((160, 210, 235), (120, 170, 120), (30, 32, 24)), "Sandbox", "Mojang Studios", 2011, 48.0,
     "Build, explore, mine and survive in an infinite block world.", "BLOCKS"),
    ("fortnite", "Fortnite", "city", (128, 146, 255), (20, 18, 60),
     ((150, 170, 255), (80, 60, 170), (10, 8, 30)), "Battle Royale", "Epic Games", 2017, 36.0,
     "Drop in, build up, be the last one standing on a ever-changing island.", "BATTLE ROYALE"),
    ("re4", "Resident Evil 4", "horror", (220, 96, 66), (20, 12, 12),
     ((140, 90, 70), (60, 30, 26), (10, 6, 6)), "Survival Horror", "Capcom", 2023, 68.0,
     "A rescue mission in a remote village spirals into a fight for survival.", "CHAINSAW"),
    ("horizonfw", "Horizon Forbidden West", "terrain", (110, 220, 205), (20, 40, 56),
     ((140, 230, 225), (90, 140, 180), (8, 14, 24)), "Action RPG", "Guerrilla", 2022, 96.0,
     "Aloy ventures into a forbidden frontier to stop a dying world.", "FORBIDDEN WEST"),
    ("returnal", "Returnal", "space", (160, 170, 235), (14, 16, 34),
     ((150, 160, 225), (60, 50, 130), (6, 6, 18)), "Roguelike", "Housemarque", 2021, 60.0,
     "A hostile alien planet that rewinds death itself.", "ATROPOS"),
    ("demonssouls", "Demon's Souls", "horror", (215, 200, 165), (22, 20, 18),
     ((210, 200, 180), (90, 80, 80), (8, 6, 8)), "Action RPG", "Bluepoint Games", 2020, 56.0,
     "The legendary original, rebuilt in terrifying fidelity.", "BO LETARIA"),
    ("ratchet", "Ratchet & Clank: Rift Apart", "space", (170, 140, 255), (28, 16, 56),
     ((190, 160, 255), (90, 60, 190), (10, 6, 30)), "Platformer", "Insomniac Games", 2021, 42.0,
     "Two lombaxes, infinite dimensions, one hair-raising adventure.", "RIFT APART"),
    ("eldenring", "Elden Ring", "terrain", (235, 200, 120), (26, 24, 20),
     ((245, 214, 140), (150, 120, 80), (10, 8, 8)), "Action RPG", "FromSoftware", 2022, 72.0,
     "Rise, Tarnished, and claim the shattered Elden Ring.", "THE LANDS BETWEEN"),
    ("cyberpunk", "Cyberpunk 2077", "neon", (250, 226, 60), (12, 24, 40),
     ((255, 226, 90), (60, 140, 200), (6, 10, 22)), "Action RPG", "CD Projekt RED", 2020, 88.0,
     "Night City: a neon dream that always ends in blood.", "NIGHT CITY"),
    ("ghostoftsushima", "Ghost of Tsushima", "terrain", (225, 225, 210), (26, 30, 36),
     ((250, 240, 220), (200, 150, 140), (12, 10, 14)), "Action-Adventure", "Sucker Punch Productions", 2020, 64.0,
     "A samurai rejects honour to save his island.", "TSUSHIMA"),
    ("witcher3", "The Witcher 3: Wild Hunt", "terrain", (190, 190, 200), (20, 24, 32),
     ((200, 205, 215), (100, 90, 110), (8, 10, 14)), "Action RPG", "CD Projekt RED", 2015, 54.0,
     "A monster hunter searches a war-torn world for his ward.", "WILD HUNT"),
    ("ff7remake", "Final Fantasy VII Remake", "city", (120, 190, 225), (18, 22, 36),
     ((140, 200, 235), (50, 60, 120), (6, 8, 16)), "Action RPG", "Square Enix", 2020, 88.0,
     "Midgar reimagined, and a destiny that refuses to be fixed.", "MIDGAR"),
    ("deathstranding2", "Death Stranding 2", "terrain", (205, 220, 235), (16, 22, 28),
     ((215, 230, 240), (90, 130, 150), (6, 10, 12)), "Adventure", "Kojima Productions", 2025, 104.0,
     "Fragile connections across a world on the brink.", "ON THE BEACH"),
]

PALETTE_KINDS = {
    # kind: (haze, base-dark, detail-light, motes)
    "terrain": dict(haze=0.55, motes=0.35, layers=6),
    "city": dict(haze=0.42, motes=0.25, layers=4),
    "neon": dict(haze=0.50, motes=0.30, layers=4),
    "space": dict(haze=0.30, motes=0.85, layers=3),
    "voxel": dict(haze=0.35, motes=0.15, layers=4),
    "horror": dict(haze=0.65, motes=0.20, layers=4),
}

SEED = {}


def rnd_for(gid, salt=0):
    base = sum((i + 1) * ord(c) for i, c in enumerate(gid))
    return random.Random(base * 7919 + salt * 104729)


def lerp(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


def vgrad(w, h, stops):
    """Multi-stop vertical gradient using a 1px column then resized."""
    col = Image.new("RGB", (1, h))
    px = col.load()
    n = len(stops) - 1
    for y in range(h):
        t = y / max(1, h - 1) * n
        i = min(n - 1, int(t))
        px[0, y] = lerp(stops[i], stops[i + 1], t - i)
    return col.resize((w, h), Image.BILINEAR).convert("RGBA")


def glow(w, h, cx, cy, rad, color, alpha, power=1.7, blur=0.08):
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    steps = 18
    for i in range(steps, 0, -1):
        t = i / steps
        r = rad * t
        a = int(alpha * ((1 - t) ** power))
        if a <= 0:
            continue
        d.ellipse([cx - r, cy - r * 0.86, cx + r, cy + r * 0.86],
                  fill=(color[0], color[1], color[2], a))
    return layer.filter(ImageFilter.GaussianBlur(max(2, int(rad * blur))))


def haze_band(w, h, y0, y1, color, alpha):
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for y in range(int(y0), min(h, int(y1))):
        t = (y - y0) / max(1, (y1 - y0))
        a = int(alpha * (1 - t))
        d.line([(0, y), (w, y)], fill=(color[0], color[1], color[2], a))
    return layer.filter(ImageFilter.GaussianBlur(max(3, h // 90)))


def ridgeline(rnd, w, y_base, amp, color, alpha=255, step=None):
    """One mountain/terrain silhouette layer as an RGBA image."""
    step = step or max(2, w // 220)
    pts = []
    phase1, phase2, phase3 = (rnd.uniform(0, 6.28) for _ in range(3))
    f1, f2, f3 = (rnd.uniform(1.2, 2.6), rnd.uniform(3.0, 6.0), rnd.uniform(7.0, 12.0))
    for x in range(0, w + step, step):
        u = x / w
        y = (math.sin(u * f1 * math.pi + phase1) * 0.55 +
             math.sin(u * f2 * math.pi + phase2) * 0.30 +
             math.sin(u * f3 * math.pi + phase3) * 0.15)
        pts.append((x, y_base - y * amp))
    layer = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.polygon(pts + [(w, w), (0, w)], fill=(color[0], color[1], color[2], alpha))
    return layer


def terrain_scene(g, w, h, salt=0):
    gid, title, kind, accent, deep, sky, *_ = g
    rnd = rnd_for(gid, salt)
    cfg = PALETTE_KINDS[kind]
    im = vgrad(w, h, sky)
    # sun / light source
    sx = w * rnd.uniform(0.58, 0.78)
    sy = h * rnd.uniform(0.30, 0.46)
    sun = (255, 238, 205) if kind != "horror" else (255, 160, 120)
    im = Image.alpha_composite(im, glow(w, h, sx, sy, min(w, h) * 0.55, sun, 150))
    im = Image.alpha_composite(im, glow(w, h, sx, sy, min(w, h) * 0.10, (255, 255, 255), 210, 0.9, 0.04))
    # god rays
    rays = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rays)
    n = rnd.randint(4, 7)
    for i in range(n):
        ang = rnd.uniform(-2.5, -0.6)
        ln = max(w, h) * rnd.uniform(0.7, 1.3)
        wid = rnd.uniform(8, 40)
        ex, ey = sx + math.cos(ang) * ln, sy - math.sin(ang) * -ln
        rd.line([(sx, sy), (ex, ey)], fill=(255, 245, 220, rnd.randint(10, 26)), width=int(wid))
    rays = rays.filter(ImageFilter.GaussianBlur(int(min(w, h) * 0.03)))
    im = Image.alpha_composite(im, rays)
    # terrain layers back->front
    n_layers = cfg["layers"]
    for i in range(n_layers):
        t = i / (n_layers - 1)
        y_base = h * (0.52 + 0.40 * t) + rnd.uniform(-0.02, 0.02) * h
        amp = h * (0.22 * (1 - t) + 0.04)
        col = lerp(lerp(sky[-1], accent, 0.15), (6, 6, 10), 0.15 + 0.62 * t)
        layer = ridgeline(rnd, w, y_base, amp, col)
        layer = layer.crop((0, 0, w, h))
        # atmospheric fade on distant layers
        if i < n_layers - 1:
            fade = haze_band(w, h, y_base - amp, y_base + h * 0.1, sky[1], int(90 * (1 - t)))
            layer = Image.alpha_composite(layer, fade.crop((0, 0, w, h)) if fade.size != (w, h)
                                          else fade)
            layer = ImageChops.multiply(layer, Image.new("RGBA", (w, h), (255, 255, 255, 255)))
        im = Image.alpha_composite(im, layer.filter(ImageFilter.GaussianBlur(0.6 if t > 0.6 else 1.6)))
    # foreground haze
    im = Image.alpha_composite(im, haze_band(w, h, h * 0.45, h * 0.85, sky[1], 70))
    # lone figure silhouette on the ridge (cinematic scale cue)
    fx = w * rnd.uniform(0.62, 0.78)
    fy = h * rnd.uniform(0.62, 0.70)
    im = Image.alpha_composite(im, figure(fx, fy, h * rnd.uniform(0.075, 0.11), w, h))
    return im, rnd


def city_scene(g, w, h, salt=0):
    gid, title, kind, accent, deep, sky, *_ = g
    rnd = rnd_for(gid, salt)
    cfg = PALETTE_KINDS[kind]
    im = vgrad(w, h, sky)
    # horizon glow
    im = Image.alpha_composite(im, glow(w, h, w * 0.5, h * 0.62, w * 0.6, accent, 90, 2.0))
    im = Image.alpha_composite(im, glow(w, h, w * rnd.uniform(0.2, 0.8), h * 0.5, w * 0.35,
                                        (255, 255, 255), 60))
    # skyline: three depth bands of buildings
    for band in range(3):
        t = band / 2
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        base_y = h * (0.60 + 0.13 * t)
        col = lerp(lerp(sky[-1], (10, 10, 18), 0.3), (4, 4, 8), 0.28 + 0.30 * t)
        x = -20
        while x < w + 20:
            bw = rnd.uniform(0.02, 0.075) * w
            bh = rnd.uniform(0.05, 0.30) * h * (1 - 0.25 * t) * (0.6 if band == 0 else 1.0)
            d.rectangle([x, base_y - bh, x + bw, base_y + h], fill=(*col, 255))
            # antenna / roof details
            if rnd.random() < 0.35:
                ax = x + bw * rnd.uniform(0.2, 0.8)
                d.line([(ax, base_y - bh), (ax, base_y - bh - h * 0.02)], fill=(*col, 255), width=2)
            # lit windows
            if band == 2:
                wn = int(bh * bw / (w * 0.0016))
                for _ in range(min(wn, 26)):
                    wx = x + rnd.uniform(0.08, 0.9) * bw
                    wy = base_y - rnd.uniform(0.05, 0.95) * bh
                    wc = accent if rnd.random() < 0.55 else (255, 240, 200)
                    a = rnd.randint(90, 210)
                    d.rectangle([wx, wy, wx + 1.6, wy + 2.4], fill=(*wc, a))
            x += bw + rnd.uniform(0.002, 0.012) * w
        if band < 2:
            fog = haze_band(w, h, base_y - h * 0.25, base_y + h * 0.2, sky[1], 120)
            layer = Image.alpha_composite(layer, fog)
        im = Image.alpha_composite(im, layer)
    # street glow strip + wet reflection
    im = Image.alpha_composite(im, glow(w, h, w * 0.5, h * 0.82, w * 0.55, accent, 80, 2.2))
    refl = im.crop((0, int(h * 0.55), w, int(h * 0.9))).transpose(Image.FLIP_TOP_BOTTOM)
    refl = refl.filter(ImageFilter.GaussianBlur(int(h * 0.02)))
    refl.putalpha(90)
    im.alpha_composite(refl, (0, int(h * 0.72)))
    im = Image.alpha_composite(im, haze_band(w, h, h * 0.62, h, sky[1], 80))
    # rooftop ledge + crouched figure (avoids oversized blob, keeps depth)
    ledge = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(ledge)
    ly = h * rnd.uniform(0.88, 0.94)
    ld.rectangle([0, ly, w * 0.42, h], fill=(4, 4, 9, 255))
    ld.rectangle([0, ly - h * 0.008, w * 0.42, ly], fill=(*lerp(accent, (255, 255, 255), 0.15), 40))
    im = Image.alpha_composite(im, ledge)
    fx = w * rnd.uniform(0.16, 0.30)
    fh = h * rnd.uniform(0.20, 0.26)
    im = Image.alpha_composite(im, glow(w, h, fx, ly - fh * 0.55, fh * 1.4, accent, 52))
    im = Image.alpha_composite(im, figure(fx, ly, fh, w, h, alpha=252))
    im = Image.alpha_composite(im, haze_band(w, h, h * 0.70, h, sky[1], 60))
    return im, rnd


def neon_scene(g, w, h, salt=0):
    """Palm-lined boulevard, sunset, wet road — 'Vice City' language, abstract."""
    gid, title, kind, accent, deep, sky, *_ = g
    rnd = rnd_for(gid, salt)
    im = vgrad(w, h, sky)
    sx, sy = w * 0.5, h * 0.42
    im = Image.alpha_composite(im, glow(w, h, sx, sy, w * 0.42, (255, 250, 235), 190, 1.2, 0.05))
    # horizontal retro stripes across the sun
    stripes = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(stripes)
    for i in range(7):
        y = sy + i * (h * 0.016) + h * 0.02
        sd.rectangle([0, y, w, y + h * 0.005], fill=(0, 0, 0, 0))
    # road
    road_top = h * 0.62
    rd = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rdd = ImageDraw.Draw(rd)
    rdd.polygon([(w * 0.5 - w * 0.03, road_top), (w * 0.5 + w * 0.03, road_top),
                 (w * 1.1, h), (-w * 0.1, h)], fill=(14, 8, 20, 255))
    # lane dashes with perspective
    for i in range(1, 8):
        t = (i / 8) ** 1.8
        y = road_top + t * (h - road_top)
        cx = w * 0.5
        half = w * 0.03 + t * w * 0.55
        lw = 2 + t * 8
        rdd.line([(cx, y), (cx, y + lw * 3)], fill=(255, 210, 120, int(60 + 160 * t)), width=int(lw))
    im = Image.alpha_composite(im, rd)
    # palms (stylized, original): curved trunk + arc fronds against the sun
    def palm(px, py, s, col, alpha=255):
        lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        tx, ty = px, py - s * 0.85
        # trunk as a quadratic curve
        pts = []
        for i in range(24):
            t = i / 23
            cx = px + math.sin(t * 1.2) * s * 0.18
            cy = py - t * s * 0.85
            pts.append((cx, cy))
        d.line(pts, fill=(*col, alpha), width=max(2, int(s * 0.045)))
        # fronds
        for k in range(8):
            ang = math.pi + (k / 7) * math.pi + rnd.uniform(-0.06, 0.06)
            ln = s * rnd.uniform(0.34, 0.52)
            pts2 = []
            for i in range(12):
                t = i / 11
                fx2 = tx + math.cos(ang) * ln * t
                fy2 = ty + math.sin(ang) * ln * t * 0.30 + (t ** 2) * ln * 0.62
                pts2.append((fx2, fy2))
            d.line(pts2, fill=(*col, alpha), width=max(1, int(s * 0.014)))
            # leaflets
            for i in range(2, 10):
                t = i / 11
                lx = tx + math.cos(ang) * ln * t
                ly = ty + math.sin(ang) * ln * t * 0.30 + (t ** 2) * ln * 0.62
                ll = s * 0.045 * (1 - t * 0.5)
                d.line([(lx, ly), (lx - ll * 0.6, ly + ll * 1.2)], fill=(*col, alpha),
                       width=max(1, int(s * 0.006)))
        lay = lay.filter(ImageFilter.GaussianBlur(0.8))
        return lay
    for i, (px, s) in enumerate(((w * 0.08, h * 0.72), (w * 0.20, h * 0.62),
                                 (w * 0.84, h * 0.70), (w * 0.94, h * 0.60))):
        im = Image.alpha_composite(im, palm(px, h * 0.80 + i % 2 * h * 0.04, s, (8, 4, 14)))
    # distant towers
    tw = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    td = ImageDraw.Draw(tw)
    x = w * 0.25
    while x < w * 0.78:
        bw = rnd.uniform(0.02, 0.05) * w
        bh = rnd.uniform(0.05, 0.16) * h
        td.rectangle([x, road_top - bh, x + bw, road_top], fill=(26, 10, 34, 235))
        for _ in range(int(bh / 6)):
            wx = x + rnd.uniform(0.1, 0.85) * bw
            wy = road_top - rnd.uniform(0.05, 0.95) * bh
            td.rectangle([wx, wy, wx + 1.5, wy + 2], fill=(*accent, rnd.randint(120, 220)))
        x += bw + rnd.uniform(0.004, 0.02) * w
    im = Image.alpha_composite(im, tw)
    im = Image.alpha_composite(im, haze_band(w, h, h * 0.40, h * 0.68, sky[1], 80))
    # wet road reflections of the sun
    refl = gl = glow(w, h, w * 0.5, h * 0.86, w * 0.22, accent, 120, 2.4)
    im = Image.alpha_composite(im, refl)
    return im, rnd


def space_scene(g, w, h, salt=0):
    gid, title, kind, accent, deep, sky, *_ = g
    rnd = rnd_for(gid, salt)
    im = vgrad(w, h, sky)
    # nebula clouds
    for i in range(5):
        cx, cy = rnd.uniform(0, w), rnd.uniform(0, h)
        col = lerp(accent, sky[1], rnd.uniform(0.2, 0.7))
        im = Image.alpha_composite(im, glow(w, h, cx, cy, rnd.uniform(0.25, 0.6) * w, col,
                                            rnd.randint(70, 130), 2.2, 0.16))
    # stars
    st = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(st)
    for _ in range(int(w * h / 900)):
        x, y = rnd.uniform(0, w), rnd.uniform(0, h)
        r = rnd.choice((0.6, 0.8, 1.0, 1.4))
        a = rnd.randint(70, 235)
        sd.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, a))
    im = Image.alpha_composite(im, st)
    # hero planet limb bottom-right
    pr = w * rnd.uniform(0.42, 0.55)
    pcx, pcy = w * rnd.uniform(0.55, 0.8), h + pr * 0.55
    planet = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    pd = ImageDraw.Draw(planet)
    pd.ellipse([pcx - pr, pcy - pr, pcx + pr, pcy + pr], fill=(12, 10, 26, 255))
    for i in range(26):
        t = i / 25
        pd.arc([pcx - pr * (1 - t * 0.9), pcy - pr * (1 - t * 0.9), pcx + pr * (1 - t * 0.9),
                pcy + pr * (1 - t * 0.9)], 200, 340,
               fill=(accent[0], accent[1], accent[2], int(30 * (1 - t))), width=3)
    im = Image.alpha_composite(im, planet)
    im = Image.alpha_composite(im, glow(w, h, pcx - pr * 0.5, pcy - pr * 0.85, pr * 0.7, accent, 110))
    # floating debris / platforms (Astro-like playful shapes)
    for i in range(rnd.randint(3, 6)):
        cx, cy = rnd.uniform(0.1, 0.9) * w, rnd.uniform(0.2, 0.7) * h
        s = rnd.uniform(0.02, 0.06) * min(w, h)
        lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        d.ellipse([cx - s, cy - s * 0.45, cx + s, cy + s * 0.45], fill=(*lerp(accent, (255, 255, 255), 0.2), 200))
        d.ellipse([cx - s * 0.7, cy - s * 0.85, cx + s * 0.7, cy - s * 0.05],
                  fill=(*lerp(accent, (10, 10, 20), 0.35), 235))
        im = Image.alpha_composite(im, lay)
    return im, rnd


def voxel_scene(g, w, h, salt=0):
    gid, title, kind, accent, deep, sky, *_ = g
    rnd = rnd_for(gid, salt)
    im = vgrad(w, h, sky)
    im = Image.alpha_composite(im, glow(w, h, w * 0.75, h * 0.25, w * 0.3, (255, 250, 220), 120))
    # blocky terrain from back to front
    cell = max(12, w // 26)
    for band in range(4):
        t = band / 3
        y0 = h * (0.52 + 0.10 * t)
        col = lerp((70, 130, 70), (24, 44, 26), t + 0.1)
        lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        x = -cell
        while x < w + cell:
            hgt = rnd.randint(1, 3) * cell * (0.6 + 0.8 * (1 - t))
            d.rectangle([x, y0 - hgt, x + cell, h], fill=(*col, 255))
            # grass top
            d.rectangle([x, y0 - hgt, x + cell, y0 - hgt + cell * 0.25],
                        fill=(*lerp(col, (150, 210, 110), 0.55 - t * 0.3), 255))
            x += cell
        if band < 3:
            lay = Image.alpha_composite(lay, haze_band(w, h, y0 - h * 0.2, y0 + h * 0.1, sky[1], 90))
        im = Image.alpha_composite(im, lay)
    # floating islands (grass cap + dirt body)
    for _ in range(5):
        bx, by = rnd.uniform(0.08, 0.86) * w, rnd.uniform(0.10, 0.42) * h
        s_ = rnd.uniform(0.05, 0.11) * min(w, h)
        lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        d.rectangle([bx, by, bx + s_ * 2.2, by + s_ * 0.42], fill=(70, 120, 66, 255))
        d.polygon([(bx + s_ * 0.2, by + s_ * 0.42), (bx + s_ * 2.0, by + s_ * 0.42),
                   (bx + s_ * 1.3, by + s_ * 1.25)], fill=(74, 56, 40, 255))
        d.rectangle([bx, by, bx + s_ * 2.2, by + s_ * 0.14], fill=(140, 206, 104, 255))
        im = Image.alpha_composite(im, lay)
    # blocky hero tree
    tx, ty = w * 0.70, h * 0.66
    lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    d.rectangle([tx - cell * 0.5, ty - cell * 2, tx + cell * 0.5, ty], fill=(84, 60, 40, 255))
    for dx, dy, n in ((0, -3, 3), (-1, -2, 3), (1, -2, 3), (-1, -3, 3), (1, -3, 3), (0, -4, 2)):
        d.rectangle([tx + dx * cell, ty + dy * cell, tx + (dx + 1) * cell, ty + (dy + 1) * cell],
                    fill=(62, 130, 58, 255))
    im = Image.alpha_composite(im, lay)
    return im, rnd


def horror_scene(g, w, h, salt=0):
    gid, title, kind, accent, deep, sky, *_ = g
    rnd = rnd_for(gid, salt)
    im = vgrad(w, h, sky)
    im = Image.alpha_composite(im, glow(w, h, w * 0.7, h * 0.35, w * 0.4, accent, 70, 2.0))
    # dead trees
    def branch(d, x, y, ang, ln, wd, col, depth):
        ex = x + math.cos(ang) * ln
        ey = y + math.sin(ang) * ln
        d.line([(x, y), (ex, ey)], fill=(*col, 255), width=max(1, int(wd)))
        if depth <= 0 or ln < w * 0.004:
            return
        for _ in range(rnd.randint(2, 3)):
            branch(d, ex, ey, ang + rnd.uniform(-0.75, 0.75), ln * rnd.uniform(0.55, 0.78),
                   wd * 0.62, col, depth - 1)
    def tree(x, y, s_, col):
        lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        d.line([(x, y), (x + s_ * 0.04, y - s_ * 0.55)], fill=(*col, 255), width=max(3, int(s_ * 0.055)))
        branch(d, x + s_ * 0.04, y - s_ * 0.55, -math.pi / 2 + rnd.uniform(-0.15, 0.15),
               s_ * 0.34, s_ * 0.045, col, 4)
        return lay
    for i in range(5):
        x = w * (0.04 + i * 0.235) + rnd.uniform(-20, 20)
        im = Image.alpha_composite(im, tree(x, h * 0.88, h * rnd.uniform(0.55, 0.85), (9, 5, 7)))
    # distant mansion silhouette
    md = ImageDraw.Draw(im)
    mx, my = w * 0.78, h * 0.62
    md.rectangle([mx, my, mx + w * 0.16, h * 0.9], fill=(8, 5, 7, 255))
    md.polygon([(mx - 10, my), (mx + w * 0.08, my - h * 0.09), (mx + w * 0.16 + 10, my)],
               fill=(8, 5, 7, 255))
    for k in range(3):
        wx = mx + w * (0.03 + 0.04 * k)
        md.rectangle([wx, my + h * 0.08, wx + w * 0.016, my + h * 0.13],
                     fill=(255, 176, 96, 120))
    # fog banks
    im = Image.alpha_composite(im, haze_band(w, h, h * 0.5, h * 0.95, (190, 180, 170), 110))
    # lamppost light
    lx = w * 0.18
    im = Image.alpha_composite(im, glow(w, h, lx, h * 0.42, w * 0.18, (255, 190, 120), 120))
    d = ImageDraw.Draw(im)
    d.line([(lx, h * 0.42), (lx, h * 0.92)], fill=(12, 8, 8, 255), width=max(2, w // 400))
    im = Image.alpha_composite(im, haze_band(w, h, h * 0.3, h * 0.7, (60, 30, 20), 60))
    return im, rnd


def figure(cx, base_y, height, w, h, alpha=238, col=(6, 6, 10)):
    """Human silhouette with correct proportions: head r~h/13, shoulders h/3.6."""
    lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    hh = height
    head_r = hh * 0.077
    head_cy = base_y - hh + head_r
    d.ellipse([cx - head_r, head_cy - head_r, cx + head_r, head_cy + head_r], fill=(*col, alpha))
    neck_y = head_cy + head_r * 0.85
    shoulder_y = neck_y + head_r * 0.5
    sh_w = hh * 0.155
    hip_y = base_y - hh * 0.46
    hip_w = hh * 0.105
    d.polygon([(cx - sh_w, shoulder_y + head_r * 0.2), (cx + sh_w, shoulder_y + head_r * 0.2),
               (cx + hip_w, hip_y), (cx - hip_w, hip_y)], fill=(*col, alpha))
    # arms
    d.polygon([(cx - sh_w, shoulder_y), (cx - sh_w * 1.12, hip_y + hh * 0.10),
               (cx - sh_w * 0.78, hip_y + hh * 0.10), (cx - sh_w * 0.62, shoulder_y + head_r * 0.6)],
              fill=(*col, alpha))
    d.polygon([(cx + sh_w, shoulder_y), (cx + sh_w * 1.12, hip_y + hh * 0.10),
               (cx + sh_w * 0.78, hip_y + hh * 0.10), (cx + sh_w * 0.62, shoulder_y + head_r * 0.6)],
              fill=(*col, alpha))
    # legs with a gap
    lw = hip_w * 0.52
    d.polygon([(cx - hip_w, hip_y), (cx - hip_w * 0.10, hip_y), (cx - hip_w * 0.22, base_y),
               (cx - hip_w * 0.92, base_y)], fill=(*col, alpha))
    d.polygon([(cx + hip_w * 0.10, hip_y), (cx + hip_w, hip_y), (cx + hip_w * 0.92, base_y),
               (cx + hip_w * 0.22, base_y)], fill=(*col, alpha))
    # light rim from behind
    edge = lay.filter(ImageFilter.GaussianBlur(max(1, height * 0.02)))
    return lay


SCENES = {"terrain": terrain_scene, "city": city_scene, "neon": neon_scene,
          "space": space_scene, "voxel": voxel_scene, "horror": horror_scene}


def finish(im, w, h, heavy=True):
    """Atmosphere: bloom, cool-shadow grade, vignette, grain."""
    # bloom: blurred bright pass added in screen space
    rgb = im.convert("RGB")
    bright = rgb.point(lambda v: max(0, v - 158) * 2)
    bloom = bright.filter(ImageFilter.GaussianBlur(max(3, min(w, h) // 42)))
    rgb = ImageChops.screen(rgb, bloom.point(lambda v: int(v * 0.55)))
    im = rgb.convert("RGBA")
    # slight cool shadows / warm highlights grade
    r, g, b = im.convert("RGB").split()
    r = r.point(lambda v: min(255, int(v * 1.03 + 2)))
    b = b.point(lambda v: min(255, int(v * 1.02 + 3)))
    im = Image.merge("RGB", (r, g, b))
    # vignette
    mask = Image.new("L", (w, h), 0)
    md = ImageDraw.Draw(mask)
    for i in range(20):
        t = i / 19
        px, py = -(w * 0.14) + (w * 0.64) * t, -(h * 0.10) + (h * 0.60) * t
        x0, y0, x1, y1 = px, py, w - px, h - py
        if x1 - x0 < 2 or y1 - y0 < 2:
            continue
        md.ellipse([x0, y0, x1, y1], fill=int(255 * (0.80 + 0.20 * t)))
    mask = mask.filter(ImageFilter.GaussianBlur(int(min(w, h) * 0.13)))
    im = Image.composite(im, Image.new("RGB", (w, h), (0, 0, 0)), mask)
    if heavy:
        rnd = random.Random(w * 31 + h)
        px = im.load()
        for _ in range(w * h // 22):
            x, y = rnd.randrange(w), rnd.randrange(h)
            n = rnd.randint(-6, 6)
            r0, g0, b0 = px[x, y]
            px[x, y] = (max(0, min(255, r0 + n)), max(0, min(255, g0 + n)), max(0, min(255, b0 + n)))
    return im


def lockup(draw, w, h, g, scale=1.0, y_frac=0.80, centered=False, small=False):
    """Title lockup drawn into key art: large condensed title + accent tagline."""
    gid, title, kind, accent, deep, sky, genre, pub, year, size, desc, tag = g
    # main title, auto-fit to 2 lines
    words = title.split()
    lines, cur = [], ""
    for wd in words:
        trial = (cur + " " + wd).strip()
        f = F(800, 10)
        if draw.textlength(trial, font=f) > 0:  # font load ok
            lines_trial = lines + [trial]
        cur = trial
    # simple greedy split into <=2 lines based on width using a reference size
    lines = []
    if len(words) > 2:
        mid = len(words) // 2
        lines = [" ".join(words[:mid]), " ".join(words[mid:])]
    else:
        lines = [title]
    size0 = int(h * (0.080 if not small else 0.11) * scale)
    fnt = F(800, size0)
    while max(draw.textlength(l, font=fnt) for l in lines) > w * 0.80 and fnt.size > 10:
        fnt = F(800, fnt.size - 2)
    line_h = fnt.size * 1.02
    total_h = line_h * len(lines) + fnt.size * 0.30
    y = h * y_frac - total_h
    x = w * 0.5 if centered else w * 0.055
    for i, ln in enumerate(lines):
        if centered:
            draw.text((x, y + i * line_h), ln, font=fnt, fill=(255, 255, 255, 240), anchor="ma")
        else:
            draw.text((x, y + i * line_h), ln, font=fnt, fill=(255, 255, 255, 240))
    # tagline
    ft = F(700, max(9, int(fnt.size * 0.30)))
    ty = y + total_h + fnt.size * 0.16
    tx = x
    if centered:
        draw.text((x, ty), tag, font=ft, fill=(*accent, 250), anchor="ma")
    else:
        # letter-spaced tagline
        sp = ft.size * 0.26
        cx = tx
        for ch in tag:
            draw.text((cx, ty), ch, font=ft, fill=(*accent, 255))
            cx += draw.textlength(ch, font=ft) + sp


def make_game_assets(g):
    gid = g[0]
    out = os.path.join(OUT_GAMES, gid)
    os.makedirs(out, exist_ok=True)
    scene = SCENES[g[2]]
    # ---- background 1280x720 (no lockup; UI draws its own text)
    bg, _ = scene(g, 1280, 720, 3)
    bg = finish(bg, 1280, 720)
    # soft bottom shade so PS5 UI text sits cleanly
    bg = bottom_shade(bg, 0.46, 200)
    bg.save(os.path.join(out, "background.jpg"), quality=84, optimize=True, progressive=True)
    # ---- cover 512x512 with lockup
    cov, _ = scene(g, 512, 512, 11)
    cov = finish(cov, 512, 512)
    cov = bottom_shade(cov, 0.55, 235)
    d = ImageDraw.Draw(cov, "RGBA")
    lockup(d, 512, 512, g, scale=0.92, y_frac=0.88)
    strip = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    sd = ImageDraw.Draw(strip)
    sd.rectangle([0, 0, 512 - 1, 512 - 1], outline=(255, 255, 255, 26), width=2)
    cov = Image.alpha_composite(cov.convert("RGBA"), strip).convert("RGB")
    cov.save(os.path.join(out, "cover.jpg"), quality=88, optimize=True, progressive=True)
    # ---- card 480x480 (tighter art, short word only)
    card, _ = scene(g, 480, 480, 21)
    card = finish(card, 480, 480)
    card = bottom_shade(card, 0.40, 170)
    d = ImageDraw.Draw(card, "RGBA")
    wd = g[11]
    f = F(800, int(480 * 0.085))
    while d.textlength(wd, font=f) > 480 * 0.86 and f.size > 10:
        f = F(800, f.size - 2)
    # letter-spaced word
    sp = f.size * 0.10
    tw = sum(d.textlength(c, font=f) + sp for c in wd) - sp
    cx = (480 - tw) / 2
    for ch in wd:
        d.text((cx, 480 * 0.855), ch, font=f, fill=(255, 255, 255, 246))
        cx += d.textlength(ch, font=f) + sp
    card.save(os.path.join(out, "card.jpg"), quality=85, optimize=True, progressive=True)
    # ---- screenshots
    for k in range(3):
        sc, _ = scene(g, 800, 450, 40 + k * 7)
        sc = finish(sc, 800, 450)
        sc.save(os.path.join(out, f"shot{k + 1}.jpg"), quality=82, optimize=True, progressive=True)
    # ---- hero emblem: transparent version of the tagline mark (used on Game Hub)
    hw, hh = 640, 360
    hero = Image.new("RGBA", (hw, hh), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hero)
    tag = g[11]
    f = F(800, int(hh * 0.30))
    while hd.textlength(tag, font=f) > hw * 0.92 and f.size > 10:
        f = F(800, f.size - 2)
    # keyline box + tagline
    hd.rectangle([0, 0, hw - 1, hh - 1], outline=(255, 255, 255, 18), width=2)
    sp = f.size * 0.16
    tw = sum(hd.textlength(c, font=f) + sp for c in tag) - sp
    cx = (hw - tw) / 2
    for ch in tag:
        hd.text((cx, hh * 0.36), ch, font=f, fill=(255, 255, 255, 235))
        cx += hd.textlength(ch, font=f) + sp
    hero.save(os.path.join(out, "hero.png"), optimize=True)
    return gid


def bottom_shade(im, frac, max_alpha, tint=(2, 3, 8)):
    w, h = im.size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    start = int(h * (1 - frac))
    for y in range(start, h):
        t = (y - start) / max(1, h - start)
        d.line([(0, y), (w, y)], fill=(*tint, int(max_alpha * (t ** 1.6))))
    return Image.alpha_composite(im.convert("RGBA"), layer).convert("RGB")


# ---------------------------------------------------------------- sounds & fonts ------
SR = 44100


def write_wav(name, samples):
    with wave.open(os.path.join(OUT_SOUND, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 32767)))) for s in samples))


def tone(freqs, dur, vol=0.30, attack=0.006, release=0.05, harmonics=((1, 1.0), (2, 0.18))):
    n = int(SR * dur)
    a = max(1, int(SR * attack))
    r = max(1, int(SR * release))
    seg = max(1, n // max(1, len(freqs)))
    phase = 0.0
    out = []
    for i in range(n):
        k = min(len(freqs) - 1, i // seg)
        f = freqs[k]
        if k + 1 < len(freqs) and (i % seg) > seg * 0.8:
            t = (i % seg - seg * 0.8) / (seg * 0.2)
            f = freqs[k] * (1 - t) + freqs[k + 1] * t
        phase += 2 * math.pi * f / SR
        e = 1.0
        if i < a:
            e = i / a
        if i > n - r:
            e = min(e, (n - i) / r)
        v = sum(math.sin(phase * m) * amp for m, amp in harmonics)
        out.append(v * max(0.0, e) * vol)
    tail = int(SR * 0.07)
    for i in range(tail):
        out.append(out[-1 - i] * 0.22 * (1 - i / tail))
    return out


def make_sounds():
    os.makedirs(OUT_SOUND, exist_ok=True)
    write_wav("nav", tone([1280, 1215], 0.030, 0.14, 0.002, 0.016, ((1, 1.0), (2, 0.22), (3.1, 0.05))))
    write_wav("nav_alt", tone([1120, 1060], 0.030, 0.13, 0.002, 0.016, ((1, 1.0), (2.2, 0.2))))
    write_wav("select", tone([900, 1350], 0.10, 0.20, 0.003, 0.045, ((1, 1.0), (2, 0.26), (3, 0.08))))
    write_wav("back", tone([1000, 750], 0.09, 0.18, 0.003, 0.045, ((1, 1.0), (2, 0.2))))
    write_wav("open", tone([520, 780, 1040], 0.17, 0.18, 0.010, 0.07, ((1, 1.0), (2, 0.18), (4, 0.04))))
    write_wav("close", tone([1040, 780, 520], 0.15, 0.16, 0.006, 0.07, ((1, 1.0), (2, 0.16))))
    write_wav("error", tone([300, 240], 0.15, 0.20, 0.004, 0.07, ((1, 1.0), (3, 0.26))))
    write_wav("launch", tone([196, 247, 294, 392, 494, 587], 0.58, 0.22, 0.03, 0.22,
                             ((1, 1.0), (2, 0.32), (3, 0.10))))


def make_fonts():
    os.makedirs(OUT_FONT, exist_ok=True)
    keep = [("Manrope-Regular.ttf", "Manrope-Regular.ttf"), ("Manrope-Medium.ttf", "Manrope-Medium.ttf"),
            ("Manrope-SemiBold.ttf", "Manrope-SemiBold.ttf"), ("Manrope-Bold.ttf", "Manrope-Bold.ttf"),
            ("Manrope-ExtraBold.ttf", "Manrope-ExtraBold.ttf"),
            ("Figtree-Regular.ttf", "Figtree-Regular.ttf"), ("Figtree-Medium.ttf", "Figtree-Medium.ttf"),
            ("Figtree-SemiBold.ttf", "Figtree-SemiBold.ttf"), ("Figtree-Bold.ttf", "Figtree-Bold.ttf")]
    for src, dst in keep:
        p = os.path.join(FD, src)
        if os.path.exists(p):
            with open(p, "rb") as f:
                data = f.read()
            with open(os.path.join(OUT_FONT, dst), "wb") as f:
                f.write(data)
    for name in ("OFL-Manrope.txt", "OFL-Figtree.txt"):
        p = os.path.join(FD, name)
        if os.path.exists(p):
            with open(p, "rb") as f:
                data = f.read()
            with open(os.path.join(OUT_FONT, name), "wb") as f:
                f.write(data)


def make_catalog():
    items = []
    for idx, g in enumerate(GAMES):
        items.append({
            "id": g[0], "title": g[1], "genre": g[6], "publisher": g[7], "year": g[8],
            "sizeGb": g[9], "description": g[10], "tagline": g[11],
            "accent": "#%02X%02X%02X" % g[3], "deep": "#%02X%02X%02X" % g[4], "index": idx,
        })
    with open(os.path.join(A, "catalog.json"), "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=1)
    print("catalog.json:", len(items))


def main():
    only = None
    if "--only" in sys.argv:
        only = sys.argv[sys.argv.index("--only") + 1]
    game = None
    if "--game" in sys.argv:
        game = sys.argv[sys.argv.index("--game") + 1]
    if only in (None, "fonts"):
        make_fonts()
    if only in (None, "sounds"):
        make_sounds()
    if only in (None, "images"):
        for g in GAMES:
            if game and g[0] != game:
                continue
            make_game_assets(g)
            print("  art:", g[0])
        if not game:
            make_catalog()
    total = sum(os.path.getsize(os.path.join(dp, f)) for dp, _, fs in os.walk(A) for f in fs)
    print(f"assets total: {total / 1024 / 1024:.2f} MB")


if __name__ == "__main__":
    main()

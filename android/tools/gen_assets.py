#!/usr/bin/env python3
"""Generate ORIGINAL concept art + sounds for the PS5 Mobile Simulator APK."""
import json, math, os, random, struct, sys, wave
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else "/tmp/ps5build"
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

# id, title, primary, secondary, genre, publisher, year, size (MB), short description
GAMES = [
    ("minecraft", "Minecraft", (78, 168, 92), (34, 78, 46), "Sandbox", "Mojang", 2011, 1.2, "Build, explore and survive in an infinite block world."),
    ("gta5", "Grand Theft Auto V", (196, 122, 52), (46, 62, 92), "Action", "Rockstar", 2013, 96.0, "An open-world crime epic across Los Santos."),
    ("rdr2", "Red Dead Redemption 2", (176, 62, 48), (58, 34, 30), "Adventure", "Rockstar", 2018, 108.0, "An epic tale of honor and loyalty at the end of the wild west."),
    ("witcher3", "The Witcher 3", (150, 160, 168), (44, 52, 66), "RPG", "CD Projekt RED", 2015, 52.0, "Hunt monsters and shape a war-torn fantasy world."),
    ("cyberpunk2077", "Cyberpunk 2077", (232, 208, 48), (34, 132, 152), "RPG", "CD Projekt RED", 2020, 72.0, "A neon-soaked open world in Night City."),
    ("eldenring", "Elden Ring", (206, 178, 108), (62, 54, 44), "Action RPG", "FromSoftware", 2022, 48.0, "A vast fantasy realm of gods, ruins and golden grace."),
    ("godofwar", "God of War", (126, 158, 196), (48, 40, 62), "Action", "Santa Monica", 2018, 46.0, "A father and son journey through Norse mythology."),
    ("ghostoftsushima", "Ghost of Tsushima", (214, 214, 204), (74, 86, 104), "Action", "Sucker Punch", 2020, 55.0, "A samurai legend on a windswept island."),
    ("spiderman2", "Marvel's Spider-Man 2", (198, 44, 62), (34, 42, 96), "Action", "Insomniac", 2023, 68.0, "Two Spider-Men defend New York from a new threat."),
    ("horizonfw", "Horizon Forbidden West", (72, 178, 158), (36, 68, 96), "Action RPG", "Guerrilla", 2022, 56.0, "Machine-filled wilds and a journey to the frontier."),
    ("tlou2", "The Last of Us Part II", (116, 132, 108), (40, 46, 44), "Adventure", "Naughty Dog", 2020, 58.0, "A brutal, emotional story of survival and consequence."),
    ("uncharted4", "Uncharted 4", (198, 152, 78), (62, 72, 92), "Adventure", "Naughty Dog", 2016, 44.0, "Treasure, pirates and one last great adventure."),
    ("ff16", "Final Fantasy XVI", (128, 116, 176), (38, 34, 62), "RPG", "Square Enix", 2023, 62.0, "Crystals, Eikons and a kingdom at war."),
    ("ff7rebirth", "Final Fantasy VII Rebirth", (86, 186, 176), (30, 56, 76), "RPG", "Square Enix", 2024, 88.0, "A journey beyond the walls of Midgar."),
    ("re4", "Resident Evil 4", (146, 60, 56), (48, 44, 44), "Horror", "Capcom", 2023, 42.0, "A rescue mission turns into a fight for survival."),
    ("re8", "Resident Evil Village", (164, 154, 138), (46, 42, 50), "Horror", "Capcom", 2021, 38.0, "A snowbound village hiding a terrible secret."),
    ("forzah5", "Forza Horizon 5", (216, 96, 72), (76, 132, 186), "Racing", "Playground", 2021, 62.0, "An open-world racing festival in Mexico."),
    ("f123", "F1 23", (220, 56, 56), (34, 38, 48), "Racing", "Codemasters", 2023, 48.0, "The official racing experience of Formula 1."),
    ("eafc24", "EA Sports FC 24", (70, 158, 220), (28, 52, 92), "Sports", "EA Sports", 2023, 52.0, "The next chapter of the world's game."),
    ("nba2k24", "NBA 2K24", (222, 122, 46), (44, 56, 104), "Sports", "2K", 2023, 58.0, "Build your legacy on the court."),
    ("codmw3", "Call of Duty: MW III", (124, 142, 88), (38, 44, 40), "Shooter", "Activision", 2023, 78.0, "A global campaign and competitive multiplayer."),
    ("battlefield2042", "Battlefield 2042", (76, 152, 196), (36, 46, 66), "Shooter", "EA", 2021, 64.0, "All-out warfare at massive scale."),
    ("acmirage", "Assassin's Creed Mirage", (202, 166, 92), (54, 58, 74), "Action", "Ubisoft", 2023, 40.0, "A stealthy origin story in Baghdad."),
    ("acvalhalla", "Assassin's Creed Valhalla", (96, 146, 178), (40, 48, 68), "Action RPG", "Ubisoft", 2020, 66.0, "Viking raids across a Nordic England."),
    ("hogwartslegacy", "Hogwarts Legacy", (146, 104, 168), (44, 36, 66), "Action RPG", "Avalanche", 2023, 68.0, "An open-world wizarding adventure in the 1800s."),
    ("baldursgate3", "Baldur's Gate 3", (168, 122, 66), (42, 38, 46), "RPG", "Larian", 2023, 92.0, "A vast D&D adventure of choice and consequence."),
    ("stardewvalley", "Stardew Valley", (96, 176, 96), (46, 92, 66), "Simulation", "ConcernedApe", 2016, 0.6, "Farm, fish and build a life in the valley."),
    ("hollowknight", "Hollow Knight", (110, 126, 168), (26, 30, 48), "Metroidvania", "Team Cherry", 2017, 1.4, "Explore a beautifully haunted insect kingdom."),
    ("hades", "Hades", (206, 82, 66), (52, 32, 52), "Roguelike", "Supergiant", 2020, 2.2, "Battle out of the Underworld, one escape at a time."),
    ("nomanssky", "No Man's Sky", (72, 176, 196), (38, 54, 96), "Exploration", "Hello Games", 2016, 12.0, "An infinite universe of planets to discover."),
]

def font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()

def gradient(w, h, c1, c2, diagonal=True):
    im = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / max(1, h - 1)
        xshift = t * 0.35 if diagonal else 0.0
        for_x = None
        base = tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=base)
    return im

def art(w, h, g, variant, seed):
    """Original generative concept art: gradient, glow, geometry, typography."""
    rnd = random.Random(seed * 7919 + variant * 104729)
    p, s = g[2], g[3]
    im = gradient(w, h, tuple(int(c * 0.22) for c in s), tuple(int(c * 0.55) for c in p))
    # soft radial glows
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for i in range(3):
        cx = int(w * rnd.uniform(0.15, 0.85))
        cy = int(h * rnd.uniform(0.1, 0.8))
        rad = int(min(w, h) * rnd.uniform(0.35, 0.8))
        col = p if i % 2 == 0 else s
        gd.ellipse([cx - rad, cy - rad, cx + rad, cy + rad],
                   fill=(col[0], col[1], col[2], 70))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=int(min(w, h) * 0.09)))
    im = Image.alpha_composite(im.convert("RGBA"), glow)
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    # geometric shards / arcs
    for i in range(9):
        x = rnd.uniform(-0.2, 1.0) * w
        y = rnd.uniform(-0.2, 1.0) * h
        rw = rnd.uniform(0.12, 0.5) * w
        rh = rnd.uniform(0.12, 0.6) * h
        col = (*p, rnd.randint(30, 90)) if i % 3 else (*s, rnd.randint(30, 85))
        d.polygon([(x, y), (x + rw, y - rh * 0.25), (x + rw * 0.7, y + rh), (x - rw * 0.15, y + rh * 0.8)], fill=col)
    for i in range(5):
        cx, cy = rnd.uniform(0, w), rnd.uniform(0, h)
        r = rnd.uniform(0.15, 0.55) * min(w, h)
        d.arc([cx - r, cy - r, cx + r, cy + r], rnd.uniform(0, 360), rnd.uniform(90, 360),
              fill=(*p, 70), width=max(2, int(min(w, h) * 0.006)))
    # big translucent initials
    initials = "".join([x[0] for x in g[1].replace("'", "").split()[:2]]).upper()
    big = font(FONT_B, int(min(w, h) * 0.62))
    d.text((w * 0.5, h * 0.36), initials, font=big, fill=(255, 255, 255, 52), anchor="mm")
    # faint grid
    step = max(24, w // 24)
    for x in range(0, w, step):
        d.line([(x, 0), (x, h)], fill=(255, 255, 255, 14), width=1)
    for y in range(0, h, step):
        d.line([(0, y), (w, y)], fill=(255, 255, 255, 14), width=1)
    im = Image.alpha_composite(im, layer)
    # bottom vignette + title band
    band = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(band)
    bh = int(h * 0.30)
    for y in range(h - bh, h):
        a = int(205 * (y - (h - bh)) / max(1, bh))
        bd.line([(0, y), (w, y)], fill=(4, 8, 18, a))
    im = Image.alpha_composite(im, band)
    d = ImageDraw.Draw(im, "RGBA")
    tf = font(FONT_B, max(14, int(min(w, h) * 0.072)))
    title = g[1]
    # shrink to fit
    while tf.getbbox(title)[2] > w * 0.88 and tf.size > 12:
        tf = font(FONT_B, tf.size - 2)
    d.text((w * 0.055, h * 0.855), title, font=tf, fill=(245, 248, 255, 245))
    sf = font(FONT_R, max(9, int(min(w, h) * 0.032)))
    d.text((w * 0.055, h * 0.945), "SIMULATOR CONCEPT ART", font=sf, fill=(200, 214, 238, 150))
    return im.convert("RGB")

def icon():
    S = 192
    im = gradient(S, S, (14, 26, 54), (26, 66, 132))
    d = ImageDraw.Draw(im, "RGBA")
    d.rounded_rectangle([8, 8, S - 8, S - 8], radius=36, outline=(255, 255, 255, 55), width=3)
    d.ellipse([S * 0.15, S * 0.12, S * 0.85, S * 0.88], fill=(255, 255, 255, 12))
    f = font(FONT_B, 92)
    d.text((S * 0.5, S * 0.42), "PS", font=f, fill=(240, 248, 255, 245), anchor="mm")
    d.text((S * 0.5, S * 0.78), "SIM", font=font(FONT_B, 28), fill=(120, 190, 255, 240), anchor="mm")
    im.save(os.path.join(ROOT, "res", "mipmap-xxxhdpi", "ic_launcher.png"))

def tone(name, freqs, dur, vol=0.35):
    sr = 44100
    n = int(sr * dur)
    frames = bytearray()
    for i in range(n):
        t = i / sr
        k = int(t / dur * len(freqs))
        k = min(k, len(freqs) - 1)
        f = freqs[k]
        env = min(1.0, t / 0.008) * min(1.0, (dur - t) / 0.045)
        env = max(0.0, env)
        v = math.sin(2 * math.pi * f * t) * 0.72 + math.sin(4 * math.pi * f * t) * 0.18
        frames += struct.pack("<h", int(max(-1.0, min(1.0, v * env * vol)) * 32767))
    with wave.open(os.path.join(ROOT, "assets", "sounds", name + ".wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes(bytes(frames))

def main():
    os.makedirs(os.path.join(ROOT, "assets", "games"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "assets", "sounds"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "res", "mipmap-xxxhdpi"), exist_ok=True)
    catalog = []
    for idx, g in enumerate(GAMES):
        gid = g[0]
        gdir = os.path.join(ROOT, "assets", "games", gid)
        os.makedirs(gdir, exist_ok=True)
        art(512, 512, g, 0, idx + 1).save(os.path.join(gdir, "cover.jpg"), quality=84, optimize=True)
        art(1280, 720, g, 1, idx + 1).save(os.path.join(gdir, "background.jpg"), quality=80, optimize=True)
        for k in range(1, 4):
            art(640, 360, g, k + 1, idx + 1).save(os.path.join(gdir, "shot%d.jpg" % k), quality=78, optimize=True)
        catalog.append({
            "id": gid, "title": g[1], "primary": "#%02X%02X%02X" % g[2],
            "secondary": "#%02X%02X%02X" % g[3], "genre": g[4], "publisher": g[5],
            "year": g[6], "size": g[7], "description": g[8],
        })
    with open(os.path.join(ROOT, "assets", "catalog.json"), "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)
    icon()
    tone("nav", [520, 660], 0.075)
    tone("select", [660, 880, 1170], 0.16)
    tone("back", [520, 390], 0.11)
    tone("launch", [220, 330, 440, 660, 880], 0.55, 0.42)
    total = sum(os.path.getsize(os.path.join(dp, f)) for dp, _, fs in os.walk(os.path.join(ROOT, "assets")) for f in fs)
    print("games:", len(catalog), "assets bytes:", total)

if __name__ == "__main__":
    main()

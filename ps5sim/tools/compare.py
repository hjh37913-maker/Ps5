#!/usr/bin/env python3
"""
Visual comparison harness for fidelity checks.

Usage:
  python3 tools/compare.py ref.png shot.png [--crop x0,y0,x1,y1] [--out diff.png]

Both images are normalised to the same size, then we report:
  - mean absolute channel error (MAE) over the whole frame,
  - MAE over edges only (structure proxy, ignores flat colour drift),
  - a side-by-side + amplified difference strip for eyeballing.

The point is honesty: numbers are reported alongside the images, never instead of them.
"""
import sys
from PIL import Image, ImageChops, ImageFilter, ImageDraw


def load(path, size):
    im = Image.open(path).convert("RGB")
    return im.resize(size, Image.LANCZOS)


def mae(a, b):
    diff = ImageChops.difference(a, b)
    h = diff.convert("L").histogram()
    total = 0
    count = 0
    for v, n in enumerate(h):
        total += v * n
        count += n
    return total / count / 255.0


def edge(im):
    return im.convert("L").filter(ImageFilter.FIND_EDGES)


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    ref_path, shot_path = sys.argv[1], sys.argv[2]
    out = "docs/compare/diff.png"
    if "--out" in sys.argv:
        out = sys.argv[sys.argv.index("--out") + 1]
    size = (960, 540)
    ref = load(ref_path, size)
    shot = load(shot_path, size)
    box = None
    if "--crop" in sys.argv:
        box = tuple(int(v) for v in sys.argv[sys.argv.index("--crop") + 1].split(","))
        ref = ref.crop(box)
        shot = shot.crop(box)
    overall = mae(ref, shot)
    structural = mae(edge(ref), edge(shot))
    diff = ImageChops.difference(ref, shot).point(lambda v: min(255, v * 3))
    w, h = ref.size
    canvas = Image.new("RGB", (w * 3 + 20, h + 28), (12, 12, 14))
    canvas.paste(ref, (0, 28))
    canvas.paste(shot, (w + 10, 28))
    canvas.paste(diff, (w * 2 + 20, 28))
    d = ImageDraw.Draw(canvas)
    d.text((6, 8), f"reference   |   reconstruction   |   x3 diff   MAE={overall:.3f}  edgeMAE={structural:.3f}",
           fill=(230, 230, 235))
    import os
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    canvas.save(out)
    print(f"MAE={overall:.4f} edgeMAE={structural:.4f} -> {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

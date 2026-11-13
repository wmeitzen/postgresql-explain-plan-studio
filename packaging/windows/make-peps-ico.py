"""Generate packaging/windows/peps.ico (multi-size) and runtime PNG from Option D mark.

Option D: indigo-blue tile; fan-in plan (two smaller nodes → arrows → one large node
with a merge/Y mark). Matches packaging/windows/peps-icon-option-d.jpg.
"""
from __future__ import annotations

import io
import struct
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
OUT_ICO = HERE / "peps.ico"
OUT_PNG = ROOT / "src" / "main" / "resources" / "images" / "app-icon.png"
SOURCE = HERE / "peps-icon-option-d.jpg"

# Option D palette (programmatic fallback)
BG_TOP = (45, 70, 150, 255)
BG_BOT = (28, 48, 110, 255)
NODE = (140, 185, 230, 255)
NODE_EDGE = (230, 240, 255, 255)
ARROW = (245, 250, 255, 255)
MERGE = (255, 255, 255, 255)


def _lerp(a: int, b: int, t: float) -> int:
    return int(round(a + (b - a) * t))


def draw_option_d(size: int) -> Image.Image:
    """Vector-ish Option D when the JPG source is missing."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    pad = max(1, size // 18)
    tile_r = max(2, size * 14 // 64)
    # Vertical gradient fill inside rounded tile
    tile = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    td = ImageDraw.Draw(tile)
    for y in range(pad, size - pad):
        t = (y - pad) / max(1, size - 2 * pad - 1)
        color = (
            _lerp(BG_TOP[0], BG_BOT[0], t),
            _lerp(BG_TOP[1], BG_BOT[1], t),
            _lerp(BG_TOP[2], BG_BOT[2], t),
            255,
        )
        td.line([(pad, y), (size - pad - 1, y)], fill=color)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [pad, pad, size - pad - 1, size - pad - 1],
        radius=tile_r,
        fill=255,
    )
    img.paste(tile, (0, 0), mask)
    d = ImageDraw.Draw(img)

    def u(n: float) -> int:
        return int(round(size * n / 64))

    small = u(10)
    large = u(22)
    sr = max(1, size // 28)
    lr = max(2, size // 18)
    edge_w = max(1, size // 48)
    arrow_w = max(2, size // 28)

    # Left stack
    lx = u(12)
    y1 = u(20)
    y2 = u(44)
    # Right destination
    rx = u(38)
    ry = u(32)

    def box(x: int, y: int, w: int, r: int) -> None:
        d.rounded_rectangle(
            [x, y - w // 2, x + w - 1, y + w // 2 - 1],
            radius=r,
            fill=NODE,
            outline=NODE_EDGE,
            width=edge_w,
        )

    # Arrows from left nodes into right node
    for y in (y1, y2):
        d.line([(lx + small, y), (rx - 1, ry)], fill=ARROW, width=arrow_w)
        # arrowhead
        head = max(2, size * 3 // 64)
        tip_x, tip_y = rx - 1, ry
        d.polygon(
            [
                (tip_x - head, tip_y - head // 2),
                (tip_x, tip_y),
                (tip_x - head, tip_y + head // 2),
            ],
            fill=ARROW,
        )

    box(lx, y1, small, sr)
    box(lx, y2, small, sr)
    box(rx, ry, large, lr)

    # Merge / Y mark inside large node
    if size >= 24:
        cx = rx + large // 2
        top = ry - large // 4
        mid = ry + large // 8
        bot = ry + large // 3
        mw = max(2, size // 32)
        d.line([(cx - large // 5, top), (cx, mid)], fill=MERGE, width=mw)
        d.line([(cx + large // 5, top), (cx, mid)], fill=MERGE, width=mw)
        d.line([(cx, mid), (cx, bot)], fill=MERGE, width=mw)

    return img


def load_source(size: int) -> Image.Image:
    if SOURCE.is_file():
        src = Image.open(SOURCE).convert("RGBA")
        return src.resize((size, size), Image.Resampling.LANCZOS)
    return draw_option_d(size)


def make(size: int) -> Image.Image:
    return load_source(size)


def write_ico(sizes: list[int]) -> None:
    entries: list[tuple[int, int, bytes]] = []
    for size in sizes:
        buf = io.BytesIO()
        make(size).save(buf, format="PNG")
        entries.append((size, size, buf.getvalue()))

    num = len(entries)
    header = struct.pack("<HHH", 0, 1, num)
    offset = 6 + 16 * num
    dir_entries = b""
    blobs = b""
    for width, height, data in entries:
        wb = 0 if width >= 256 else width
        hb = 0 if height >= 256 else height
        dir_entries += struct.pack("<BBBBHHII", wb, hb, 0, 0, 1, 32, len(data), offset)
        offset += len(data)
        blobs += data

    OUT_ICO.write_bytes(header + dir_entries + blobs)
    print(f"wrote {OUT_ICO} ({OUT_ICO.stat().st_size} bytes)")


def main() -> None:
    write_ico([16, 32, 48, 64, 128, 256])
    OUT_PNG.parent.mkdir(parents=True, exist_ok=True)
    make(256).save(OUT_PNG, format="PNG")
    print(f"wrote {OUT_PNG} (from {SOURCE.name if SOURCE.is_file() else 'vector fallback'})")


if __name__ == "__main__":
    main()

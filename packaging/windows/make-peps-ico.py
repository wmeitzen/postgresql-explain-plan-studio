"""Generate packaging/windows/peps.ico (multi-size PNG-in-ICO) for jpackage / WiX."""
from __future__ import annotations

import io
import struct
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).with_name("peps.ico")


def make(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = max(1, size // 16)
    radius = max(2, size // 6)
    bg = (41, 98, 155, 255)
    accent = (240, 236, 182, 255)
    accent2 = (102, 107, 209, 255)
    line = (173, 223, 243, 255)

    d.rounded_rectangle(
        [pad, pad, size - pad - 1, size - pad - 1],
        radius=radius,
        fill=bg,
    )

    def box(cx: int, cy: int, w: int, h: int, fill) -> None:
        x0, y0 = cx - w // 2, cy - h // 2
        rr = max(1, size // 20)
        d.rounded_rectangle([x0, y0, x0 + w, y0 + h], radius=rr, fill=fill)

    nw, nh = max(6, size * 22 // 64), max(4, size * 14 // 64)
    root = (size // 2, size * 18 // 64)
    left = (size * 20 // 64, size * 42 // 64)
    right = (size * 44 // 64, size * 42 // 64)

    lw = max(1, size // 32)
    d.line([root, left], fill=line, width=lw)
    d.line([root, right], fill=line, width=lw)

    box(*root, nw, nh, accent)
    box(*left, nw, nh, accent2)
    box(*right, nw, nh, accent)
    return img


def main() -> None:
    sizes = [16, 32, 48, 64, 128, 256]
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

    OUT.write_bytes(header + dir_entries + blobs)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()

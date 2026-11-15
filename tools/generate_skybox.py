"""
Generates the faces of the demo skybox (resources/cubemaps/sky/right|left|top|bottom|front|back.png), drawn by the
code: no third-party licence. A sky gradient above the horizon, the ground below, and the name of each face written
on it to check the orientation of the cubemap (each word readable from inside, the horizon continuous from a face to
the next).

The faces follow the convention of the cubemaps (the one of the usual skybox pictures): seen from inside, in the
right-handed space of OpenGL, a picture of a side appears mirrored left to right, those of the top and the bottom
upside down (looking up or down from the direction -Z). The words are written that way, to be read right from inside.

    python tools/generate_skybox.py
"""
import os
import struct
import zlib

SIZE = 256
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resources", "cubemaps", "sky")

ZENITH = (40, 90, 200)
HORIZON = (190, 215, 245)
GROUND_NEAR = (90, 110, 60)
GROUND_FAR = (60, 75, 40)
INK = (30, 30, 60)

# 5x7 letters of the face names
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["01110", "00100", "00100", "00100", "00100", "00100", "01110"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
}


def write_png(path, pixels):
    """pixels: rows of (r, g, b) tuples."""
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("3B", *p) for p in row) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as file:
        file.write(png)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def side():
    """Row 0 is the top: sky above the horizon (middle row), ground below."""
    half = SIZE // 2
    rows = []
    for y in range(SIZE):
        if y < half:
            color = mix(ZENITH, HORIZON, (y / half) ** 2)
        else:
            color = mix(GROUND_FAR, GROUND_NEAR, (y - half) / half)
        rows.append([color] * SIZE)
    return rows


def plain(color):
    return [[color] * SIZE for _ in range(SIZE)]


def write_text(pixels, text, scale, center_y, mirror_x, flip_y):
    width = len(text) * 6 * scale - scale
    x0 = (SIZE - width) // 2
    y0 = center_y - 7 * scale // 2
    for index, letter in enumerate(text):
        glyph = FONT[letter]
        for row, line in enumerate(glyph):
            for column, bit in enumerate(line):
                if bit != "1":
                    continue
                for dy in range(scale):
                    for dx in range(scale):
                        x = x0 + (index * 6 + column) * scale + dx
                        y = y0 + row * scale + dy
                        pixels[SIZE - 1 - y if flip_y else y][SIZE - 1 - x if mirror_x else x] = INK


FACES = {
    "right": side,
    "left": side,
    "top": lambda: plain(ZENITH),
    "bottom": lambda: plain(GROUND_NEAR),
    "front": side,
    "back": side,
}

if __name__ == "__main__":
    os.makedirs(ROOT, exist_ok=True)
    for name, make in FACES.items():
        pixels = make()
        # The words of the sides in the sky, above the horizon
        write_text(pixels, name.upper(), 6, SIZE // 4 if make is side else SIZE // 2,
                   mirror_x=make is side, flip_y=make is not side)
        write_png(os.path.join(ROOT, name + ".png"), pixels)
    print("--Skybox written in " + os.path.normpath(ROOT))

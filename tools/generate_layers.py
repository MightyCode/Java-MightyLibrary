"""
Generates the layers of the demo character (resources/layered/hero/*.png), drawn by the code: no third-party licence.
Each layer is a transparent 32x32 pixel art picture; the scene LayeredTexture2D draws them stacked, sorted by name.

    python tools/generate_layers.py
"""
import os
import struct
import zlib

SIZE = 32
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resources", "layered", "hero")


def write_png(path, width, height, pixels):
    """pixels: rows of (r, g, b, a) tuples."""
    raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *p) for p in row) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as file:
        file.write(png)


def layer(*rectangles):
    """rectangles: (x, y, width, height, color); later ones are drawn over earlier ones."""
    pixels = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]
    for x, y, width, height, color in rectangles:
        for row in range(y, y + height):
            for column in range(x, x + width):
                pixels[row][column] = color
    return pixels


SKIN = (240, 190, 150, 255)
EYES = (30, 30, 50, 255)
SHIRT = (60, 160, 80, 255)
TROUSERS = (50, 60, 120, 255)
HAT = (180, 40, 40, 255)
BAND = (240, 210, 60, 255)

LAYERS = {
    # Head, arms and legs
    "1_body.png": layer((11, 6, 10, 9, SKIN), (13, 9, 2, 2, EYES), (17, 9, 2, 2, EYES),
                        (7, 15, 18, 3, SKIN), (12, 24, 3, 7, SKIN), (17, 24, 3, 7, SKIN)),
    # Shirt and trousers over the body
    "2_clothes.png": layer((11, 15, 10, 9, SHIRT), (7, 15, 4, 3, SHIRT), (21, 15, 4, 3, SHIRT),
                           (11, 22, 10, 6, TROUSERS)),
    # Hat over the head
    "3_hat.png": layer((9, 4, 14, 2, HAT), (12, 0, 8, 4, HAT), (12, 3, 8, 1, BAND)),
}

if __name__ == "__main__":
    os.makedirs(ROOT, exist_ok=True)
    for name, pixels in LAYERS.items():
        write_png(os.path.join(ROOT, name), SIZE, SIZE, pixels)
    print("--Layers written in " + os.path.normpath(ROOT))

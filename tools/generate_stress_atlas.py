"""
Generates the atlas of the acceptance scene of the 2D batch (BatchStress2D, mode mix): the tileset on top, the frames
of the slime below it, without margin, in one texture (resources/textures/world/stressAtlas.png), and the animation of
the slime read in it (resources/animations/stressSlime.anim, the frames of slime.anim moved down). Derived from
tileset.png and slime.png: same licences. Without dependency (PNG read and written by the script).

    python tools/generate_stress_atlas.py
"""
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resources")
TEXTURES = os.path.join(ROOT, "textures", "world")
TILESET = os.path.join(TEXTURES, "tileset.png")
SLIME = os.path.join(TEXTURES, "slime.png")
SLIME_ANIMATION = os.path.join(ROOT, "animations", "slime.anim")
ATLAS = os.path.join(TEXTURES, "stressAtlas.png")
ATLAS_ANIMATION = os.path.join(ROOT, "animations", "stressSlime.anim")
ATLAS_NAME = "stressAtlas"


def read_png(path):
    """(width, height, rows of bytes RGBA) of an 8 bits RGBA picture, not interlaced."""
    with open(path, "rb") as file:
        data = file.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(path + ": not a PNG")
    position, compressed, header = 8, b"", None
    while position < len(data):
        length, kind = struct.unpack(">I4s", data[position:position + 8])
        body = data[position + 8:position + 8 + length]
        if kind == b"IHDR":
            header = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            compressed += body
        position += 12 + length
    width, height, depth, color, _, _, interlace = header
    if depth != 8 or color != 6 or interlace != 0:
        raise ValueError(path + ": only 8 bits RGBA without interlace is read")

    raw = zlib.decompress(compressed)
    stride = width * 4
    rows, previous = [], bytearray(stride)
    for y in range(height):
        start = y * (stride + 1)
        kind, line = raw[start], bytearray(raw[start + 1:start + 1 + stride])
        for i in range(stride):
            left = line[i - 4] if i >= 4 else 0
            up = previous[i]
            up_left = previous[i - 4] if i >= 4 else 0
            if kind == 1:
                line[i] = (line[i] + left) & 0xFF
            elif kind == 2:
                line[i] = (line[i] + up) & 0xFF
            elif kind == 3:
                line[i] = (line[i] + (left + up) // 2) & 0xFF
            elif kind == 4:
                estimate = left + up - up_left
                distances = (abs(estimate - left), abs(estimate - up), abs(estimate - up_left))
                predictor = left if distances[0] <= distances[1] and distances[0] <= distances[2] \
                    else up if distances[1] <= distances[2] else up_left
                line[i] = (line[i] + predictor) & 0xFF
        rows.append(bytes(line))
        previous = line
    return width, height, rows


def write_png(path, width, height, rows):
    raw = b"".join(b"\x00" + row for row in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as file:
        file.write(png)


def main():
    tile_width, tile_height, tiles = read_png(TILESET)
    slime_width, slime_height, slime = read_png(SLIME)
    width = max(tile_width, slime_width)
    rows = [row + bytes((width - tile_width) * 4) for row in tiles]
    rows += [row + bytes((width - slime_width) * 4) for row in slime]
    write_png(ATLAS, width, tile_height + slime_height, rows)

    # slime.anim: texture name, frame count, then x,y,width,height,hotPointX,hotPointY,frameTime per frame
    with open(SLIME_ANIMATION, encoding="utf-8") as file:
        lines = [line.strip() for line in file if line.strip()]
    frames = []
    for line in lines[2:]:
        values = line.split(",")
        values[1] = str(int(values[1]) + tile_height)
        frames.append(",".join(values))
    with open(ATLAS_ANIMATION, "w", encoding="utf-8", newline="\n") as file:
        file.write("\n".join([ATLAS_NAME, str(len(frames))] + frames))

    print("--Atlas %dx%d (tileset %dx%d, slime at y %d) written in %s, animation in %s"
          % (width, tile_height + slime_height, tile_width, tile_height, tile_height, os.path.normpath(ATLAS),
             os.path.normpath(ATLAS_ANIMATION)))


if __name__ == "__main__":
    main()

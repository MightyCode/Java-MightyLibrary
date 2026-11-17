#!/usr/bin/env python3
"""Regenerates the BMFont atlases of the engine (assets/mightylib/textures/fonts/) from the .bmfc next to the .ttf.

    python mightylib/fonts/generate.py [path/to/bmfont64.exe]

BMFont writes an absolute page path in the .fnt and widens the advance of a glyph that overhangs its cell
(Liberation Mono: A, W, %, _ get 21 instead of 19), which breaks the alignment of a monospace console. This script
puts back the file name of the page, and gives every glyph of Liberation Mono that is not narrower the advance of
the cell. The atlas is a modified version of the font (OFL: a change of format is one), so its name is
engineSans / engineMono, not the reserved name "Liberation": the .bmfc keep the real name of the font (BMFont
finds it by its name, the .ttf is installed or given by fontFile), the "face" of the .fnt is renamed here. The .ttf and their license stay out of the jar; the
jar only gets the atlas and OFL.txt.
"""
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT.parent / "src/main/resources/assets/mightylib/textures/fonts"
DEFAULT_BMFONT = Path.home() / "Downloads/bmfont64_1.14b_beta/bmfont64.exe"
# Advance of a Liberation Mono glyph at fontSize=-32 (1229 / 2048 em)
MONO_CELL = 19


def fix_fnt(fnt: Path, mono: bool) -> None:
    text = fnt.read_text(encoding="utf-8")
    text = re.sub(r'face="Liberation (Sans|Mono)"', r'face="Engine \1"', text, count=1)
    text = re.sub(r'file="[^"]*"', f'file="{fnt.stem}_0.png"', text, count=1)
    if mono:
        text = re.sub(r"xadvance=(\d+)",
                      lambda m: f"xadvance={MONO_CELL}" if int(m.group(1)) >= MONO_CELL else m.group(0), text)
    fnt.write_text(text, encoding="utf-8", newline="\n")


def main() -> None:
    bmfont = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_BMFONT
    OUT.mkdir(parents=True, exist_ok=True)
    for bmfc in sorted(ROOT.glob("liberation-*/*.bmfc")):
        fnt = OUT / (bmfc.stem + ".fnt")
        # fontFile= is relative to the folder of the .bmfc
        subprocess.run([str(bmfont), "-c", bmfc.name, "-o", str(fnt)], cwd=bmfc.parent, check=True)
        fix_fnt(fnt, "Mono" in bmfc.stem)
        print("generated", fnt.name)


if __name__ == "__main__":
    main()

package io.github.mightycode.mightylib.graphics.text;

import org.joml.Vector4f;

/**
 * The placement of the characters of a text, without object per text or per character: shared by Text and the texts
 * drawn without renderer (DebugDraw2D).
 */
public final class TextLayout {
    /** Receives the characters placed by Place, one call each. */
    public interface ICharacterSink {
        /**
         * A character: its quad (left, right, top, bottom; y grows to the next lines) and the coordinates of its
         * glyph in the atlas of the font (u0, u1, v0, v1, normalized).
         */
        void character(float left, float right, float top, float bottom, float u0, float u1, float v0, float v1);
    }

    private TextLayout() {}

    /**
     * The quad of a character whose pen is at (left, top): left, right, top, bottom in quad (the glyph scaled by
     * fontSize, its offsets not applied, like Text).
     */
    public static void CharacterQuad(FontChar character, float fontSize, float left, float top, Vector4f quad) {
        quad.set(left, (float) (character.getWidth() * fontSize) + left, top,
                (float) (character.getHeight() * fontSize) + top);
    }

    /**
     * The characters of text placed from (x, y), the top left corner of its first line, left aligned, a new line at
     * each '\n', given to sink. bounds and glyph are filled in place for each character (reused: no allocation).
     */
    public static void Place(FontFile font, CharSequence text, float fontSize, float x, float y, ICharacterSink sink,
                             Vector4f bounds, Vector4f glyph) {
        float penX = x, penY = y;
        float lineHeight = (float) (font.getLineHeight() * fontSize);
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\n') {
                penX = x;
                penY += lineHeight;
                continue;
            }

            FontChar character = font.getCharacter(c);
            CharacterQuad(character, fontSize, penX, penY, bounds);
            font.textureBounds(character, glyph);
            sink.character(bounds.x, bounds.y, bounds.z, bounds.w, glyph.x, glyph.y, glyph.z, glyph.w);
            penX += (float) (character.getxAdvance() * fontSize);
        }
    }
}

package io.github.mightycode.mightylib.resources.animation;

import org.joml.Vector4f;
import org.joml.Vector4i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Texture coordinates of a frame of a sheet: inside the frame, never on the texels of a neighbor frame
class FrameTexturePositionTest {
    private static final float EPSILON = 1e-6f;
    // Coordinates scaled back to texels lose the precision of the float
    private static final float TEXEL_EPSILON = 1e-4f;

    // slime.png of the demo: 84 x 90, frames of 28 x 30 without margin
    private static Vector4f slimeFrame(int column, int row) {
        return Animation.FrameTexturePosition(new Vector4i(column * 28, row * 30, 28, 30), 84, 90, new Vector4f());
    }

    @Test
    void frameIsInsetByHalfATexelOnEachSide() {
        Vector4f uv = slimeFrame(1, 1);

        assertEquals((28 + 0.5f) / 84f, uv.x, EPSILON);
        assertEquals((56 - 0.5f) / 84f, uv.y, EPSILON);
        assertEquals((30 + 0.5f) / 90f, uv.z, EPSILON);
        assertEquals((60 - 0.5f) / 90f, uv.w, EPSILON);
    }

    // The bug: the top of a quad read the last row of the frame above it (a line above each animation)
    @Test
    void noFrameReachesTheTexelsOfItsNeighbors() {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 3; ++column) {
                Vector4f uv = slimeFrame(column, row);

                assertTrue(uv.x * 84 >= column * 28 + Animation.FRAME_INSET - TEXEL_EPSILON);
                assertTrue(uv.y * 84 <= (column + 1) * 28 - Animation.FRAME_INSET + TEXEL_EPSILON);
                assertTrue(uv.z * 90 >= row * 30 + Animation.FRAME_INSET - TEXEL_EPSILON);
                assertTrue(uv.w * 90 <= (row + 1) * 30 - Animation.FRAME_INSET + TEXEL_EPSILON);
            }
        }
    }

    @Test
    void onePixelFrameKeepsItsCenter() {
        Vector4f uv = Animation.FrameTexturePosition(new Vector4i(3, 5, 1, 1), 10, 10, new Vector4f());

        assertEquals(0.35f, uv.x, EPSILON);
        assertEquals(0.35f, uv.y, EPSILON);
        assertEquals(0.55f, uv.z, EPSILON);
        assertEquals(0.55f, uv.w, EPSILON);
    }

    @Test
    void resultIsWrittenInOut() {
        Vector4f out = new Vector4f();

        assertSame(out, Animation.FrameTexturePosition(new Vector4i(0, 0, 4, 4), 8, 8, out));
    }
}

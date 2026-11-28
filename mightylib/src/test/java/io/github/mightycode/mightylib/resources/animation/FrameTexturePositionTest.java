package io.github.mightycode.mightylib.resources.animation;

import org.joml.Vector4f;
import org.joml.Vector4i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Texture coordinates of a frame of a sheet: its exact edges (the renderer reads only inside them, its region of the
 * texture), the edges of a frame those of its neighbors, never inside the texels of a neighbor frame
 */
class FrameTexturePositionTest {
    private static final float EPSILON = 1e-6f;
    // Coordinates scaled back to texels lose the precision of the float
    private static final float TEXEL_EPSILON = 1e-4f;

    // slime.png of the demo: 84 x 90, frames of 28 x 30 without margin
    private static Vector4f slimeFrame(int column, int row) {
        return Animation.FrameTexturePosition(new Vector4i(column * 28, row * 30, 28, 30), 84, 90, new Vector4f());
    }

    @Test
    void theFrameIsItsExactEdges() {
        Vector4f uv = slimeFrame(1, 1);

        assertEquals(28 / 84f, uv.x, EPSILON);
        assertEquals(56 / 84f, uv.y, EPSILON);
        assertEquals(30 / 90f, uv.z, EPSILON);
        assertEquals(60 / 90f, uv.w, EPSILON);
    }

    // A frame ends where its neighbor starts: inside the frame, on its edges, never inside a neighbor
    @Test
    void framesShareTheirEdgesWithoutOverlapping() {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 3; ++column) {
                Vector4f uv = slimeFrame(column, row);

                assertEquals(column * 28, uv.x * 84, TEXEL_EPSILON);
                assertEquals((column + 1) * 28, uv.y * 84, TEXEL_EPSILON);
                assertEquals(row * 30, uv.z * 90, TEXEL_EPSILON);
                assertEquals((row + 1) * 30, uv.w * 90, TEXEL_EPSILON);
                if (column < 2)
                    assertEquals(uv.y, slimeFrame(column + 1, row).x, EPSILON);
                if (row < 2)
                    assertEquals(uv.w, slimeFrame(column, row + 1).z, EPSILON);
            }
        }
    }

    // A frame of one texel: its texel, min before max (the region of the renderer keeps its center)
    @Test
    void aFrameOfOneTexelIsThatTexel() {
        Vector4f uv = Animation.FrameTexturePosition(new Vector4i(3, 5, 1, 1), 10, 10, new Vector4f());

        assertEquals(0.3f, uv.x, EPSILON);
        assertEquals(0.4f, uv.y, EPSILON);
        assertEquals(0.5f, uv.z, EPSILON);
        assertEquals(0.6f, uv.w, EPSILON);
    }

    @Test
    void resultIsWrittenInOut() {
        Vector4f out = new Vector4f();

        assertSame(out, Animation.FrameTexturePosition(new Vector4i(0, 0, 4, 4), 8, 8, out));
    }
}

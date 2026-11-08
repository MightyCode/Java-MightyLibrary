package io.github.mightycode.mightylib.graphics.game;

import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

// Texture coordinates of a tile (no GL): x/y = left/right, z/w = top/bottom of the tile in the atlas
class TileMapRendererTest {
    private static final float X = 1, Y = 2, Z = 3, W = 4;

    private static float[] corners(int rotation, int flip) {
        float[] result = new float[10];
        TileMapRenderer.setTexturePosition(result, 1, new Vector4f(X, Y, Z, W), rotation, flip);
        return result;
    }

    @Test
    void eachQuarterTurnStartsOneCornerLater() {
        assertArrayEquals(new float[]{ 0, X, Z, X, W, Y, W, Y, Z, 0 }, corners(0, 0));
        assertArrayEquals(new float[]{ 0, X, W, Y, W, Y, Z, X, Z, 0 }, corners(1, 0));
        assertArrayEquals(new float[]{ 0, Y, W, Y, Z, X, Z, X, W, 0 }, corners(2, 0));
        assertArrayEquals(new float[]{ 0, Y, Z, X, Z, X, W, Y, W, 0 }, corners(3, 0));
    }

    @Test
    void anUnknownRotationIsDrawnWithoutRotation() {
        assertArrayEquals(corners(0, 0), corners(4, 0));
        assertArrayEquals(corners(0, 0), corners(-1, 0));
    }

    @Test
    void flipsMirrorTheTile() {
        assertArrayEquals(new float[]{ 0, Y, Z, Y, W, X, W, X, Z, 0 }, corners(0, 1));
        assertArrayEquals(new float[]{ 0, X, W, X, Z, Y, Z, Y, W, 0 }, corners(0, 2));
        assertArrayEquals(new float[]{ 0, Y, W, Y, Z, X, Z, X, W, 0 }, corners(0, 3));
    }
}

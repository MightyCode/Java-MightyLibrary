package io.github.mightycode.mightylib.graphics.game;

import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Texture coordinates of a tile (no GL): x/y = left/right, z/w = top/bottom of the tile in the atlas; places of the
// tiles for each reference of the map
class TileMapRendererTest {
    private static final Vector2i TILE_SIZE = new Vector2i(16, 8);
    // A map of 5 x 3 tiles: 80 x 24 pixels
    private static final Vector2i MAP_SIZE = new Vector2i(80, 24);

    private static Vector4f edges(EDirection reference, int x, int y) {
        Vector2f offset = TileMapRenderer.ReferenceOffset(reference, MAP_SIZE, new Vector2f());
        return TileMapRenderer.TileEdges(x, y, TILE_SIZE, offset, new Vector4f());
    }

    @Test
    void everyTileHasTheSizeOfATileWhateverTheReference() {
        for (EDirection reference : EDirection.values()) {
            for (int x = 0; x < 5; ++x) {
                for (int y = 0; y < 3; ++y) {
                    Vector4f tile = edges(reference, x, y);
                    assertEquals(TILE_SIZE.x, tile.y - tile.x, reference + " " + x + ", " + y);
                    assertEquals(TILE_SIZE.y, tile.w - tile.z, reference + " " + x + ", " + y);
                }
            }
        }
    }

    // The point of reference (the position of the renderer, 0 in its space) at the corner or center of the map
    @Test
    void theReferenceIsAtItsCornerOrCenterOfTheMap() {
        float[][] expected = {
                // reference: left, top of the map
                {0, 0}, {-40, 0}, {-80, 0},
                {0, -12}, {-40, -12}, {-80, -12},
                {0, -24}, {-40, -24}, {-80, -24}};
        EDirection[] references = {EDirection.LeftUp, EDirection.Up, EDirection.RightUp,
                EDirection.Left, EDirection.None, EDirection.Right,
                EDirection.LeftDown, EDirection.Down, EDirection.RightDown};
        for (int i = 0; i < references.length; ++i) {
            Vector4f first = edges(references[i], 0, 0);
            Vector4f last = edges(references[i], 4, 2);
            assertEquals(expected[i][0], first.x, references[i].toString());
            assertEquals(expected[i][1], first.z, references[i].toString());
            // The map keeps its size
            assertEquals(MAP_SIZE.x, last.y - first.x, references[i].toString());
            assertEquals(MAP_SIZE.y, last.w - first.z, references[i].toString());
        }
    }

    // An odd size: the offset in whole pixels, toward zero
    @Test
    void theOffsetOfAMapOfAnOddSizeIsInWholePixels() {
        Vector2f offset = TileMapRenderer.ReferenceOffset(EDirection.None, new Vector2i(25, 15), new Vector2f());
        assertEquals(new Vector2f(-12, -7), offset);
    }

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

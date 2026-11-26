package io.github.mightycode.mightylib.graphics.game;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The tiles a view shows, without OpenGL: box of the view in the space of the map, ranges, rows of the compact arrays
class TileVisibilityTest {
    private static final int TILE = 16;

    // Like Camera2D: an orthographic projection of a 640 x 360 screen (y down), a view moving and zooming the world
    private static Matrix4f clip(float cameraX, float cameraY, float zoom, float angle, Matrix4f model) {
        Matrix4f projection = new Matrix4f().setOrtho(0, 640, 360, 0, -10, 10);
        Matrix4f view = new Matrix4f().translate(320, 180, 0).rotate(angle, new Vector3f(0, 0, 1))
                .scale(zoom, zoom, 1).translate(-cameraX, -cameraY, 0);
        return projection.mul(view).mul(model);
    }

    private static int[] visible(Matrix4f clip, int columns, int rows) {
        Vector4f box = new Vector4f();
        assertTrue(TileVisibility.LocalBox(clip, box));
        int[] range = new int[4];
        TileVisibility.Range(box.x, box.z, TILE, 0, TILE, columns, range, 0);
        TileVisibility.Range(box.y, box.w, TILE, 0, TILE, rows, range, 2);
        return range;
    }

    @Test
    void theBoxIsTheScreenInTheMap() {
        Vector4f box = new Vector4f();
        assertTrue(TileVisibility.LocalBox(clip(320, 180, 1, 0, new Matrix4f()), box));
        assertEquals(0, box.x, 1e-3f);
        assertEquals(0, box.y, 1e-3f);
        assertEquals(640, box.z, 1e-3f);
        assertEquals(360, box.w, 1e-3f);
    }

    // A map of 100 x 100 tiles of 16 pixels, its corner at the corner of the screen: 40 x 22.5 tiles shown
    @Test
    void theRangeCoversTheTilesOfTheScreen() {
        assertArrayEquals(new int[]{0, 39, 0, 22}, visible(clip(320, 180, 1, 0, new Matrix4f()), 100, 100));
    }

    // A tile that only touches the edge of the screen has no pixel in it: not in the range
    @Test
    void touchingTilesAreNotInTheRange() {
        int[] range = new int[2];
        TileVisibility.Range(32, 64, TILE, 0, TILE, 100, range, 0);
        assertArrayEquals(new int[]{2, 3}, range);
        TileVisibility.Range(31.5f, 64.5f, TILE, 0, TILE, 100, range, 0);
        assertArrayEquals(new int[]{1, 4}, range);
    }

    // Tiles shifted by the reference of the map (start and end moved together): the range follows them
    @Test
    void theRangeFollowsTilesThatDoNotStartAtZero() {
        int[] range = new int[2];
        // Tiles from -40: tile 0 covers [-40, -24]; the screen [0, 32] shows tiles 2 to 4 ([-8, 8] to [24, 40])
        TileVisibility.Range(0, 32, TILE, -40, TILE - 40, 100, range, 0);
        assertArrayEquals(new int[]{2, 4}, range);
        // The screen left of the map: nothing
        TileVisibility.Range(-100, -40, TILE, -40, TILE - 40, 100, range, 0);
        assertTrue(range[0] > range[1], range[0] + " to " + range[1]);
    }

    // Bounded to the map
    @Test
    void theRangeStaysInTheMap() {
        assertArrayEquals(new int[]{0, 9, 0, 9}, visible(clip(320, 180, 1, 0, new Matrix4f()), 10, 10));
        // Only the part of the map right of and under the center of the screen
        assertArrayEquals(new int[]{0, 19, 0, 11}, visible(clip(0, 0, 1, 0, new Matrix4f()), 100, 100));
    }

    // The map moved by its model: the same tiles as the camera moved the other way
    @Test
    void aMovedMapShowsOtherTiles() {
        Matrix4f moved = new Matrix4f().translate(-800, -400, 0);
        assertArrayEquals(new int[]{50, 89, 25, 47}, visible(clip(320, 180, 1, 0, moved), 100, 100));
        assertArrayEquals(visible(clip(1120, 580, 1, 0, new Matrix4f()), 100, 100),
                visible(clip(320, 180, 1, 0, moved), 100, 100));
    }

    // Zoom 2: half the tiles; zoom 0.5: twice as many; a scaled map: fewer tiles cover the screen
    @Test
    void theZoomChangesTheRange() {
        assertArrayEquals(new int[]{10, 29, 5, 16}, visible(clip(320, 180, 2, 0, new Matrix4f()), 100, 100));
        assertArrayEquals(new int[]{0, 59, 0, 33}, visible(clip(320, 180, 0.5f, 0, new Matrix4f()), 100, 100));
        assertArrayEquals(new int[]{0, 19, 0, 11},
                visible(clip(320, 180, 1, 0, new Matrix4f().scale(2, 2, 1)), 100, 100));
    }

    // A turned camera: the box of the turned screen, larger than the screen
    @Test
    void aTurnedCameraShowsTheBoxOfItsScreen() {
        int[] range = visible(clip(800, 800, 1, (float) Math.PI / 4, new Matrix4f()), 100, 100);
        // Half diagonal of the screen turned by 45 degrees: (640 + 360) / 2 / sqrt(2) = 353.6 on each axis
        assertArrayEquals(new int[]{(int) Math.floor((800 - 353.6) / TILE), (int) Math.floor((800 + 353.6) / TILE),
                (int) Math.floor((800 - 353.6) / TILE), (int) Math.floor((800 + 353.6) / TILE)}, range);
    }

    // Out of view: an empty range
    @Test
    void outOfViewIsEmpty() {
        int[] range = visible(clip(-1000, 180, 1, 0, new Matrix4f()), 100, 100);
        assertTrue(range[0] > range[1]);
        range = visible(clip(320, 5000, 1, 0, new Matrix4f()), 100, 100);
        assertTrue(range[2] > range[3]);
    }

    // A transform that is not 2D affine (perspective, flat): unknown, the whole map is shown
    @Test
    void aPerspectiveOrAFlatScaleIsUnknown() {
        Vector4f box = new Vector4f();
        assertFalse(TileVisibility.LocalBox(new Matrix4f().perspective(1f, 1.5f, 0.1f, 100f), box));
        assertFalse(TileVisibility.LocalBox(new Matrix4f().scale(0, 1, 1), box));
    }

    // A tile wider than its step (the end given): the previous columns reach into the view too
    @Test
    void aLongerTileWidensTheRange() {
        int[] range = new int[2];
        TileVisibility.Range(100, 120, TILE, 0, TILE + 40, 100, range, 0);
        // Tile 3 spans [48, 104], tile 2 ends at 88
        assertArrayEquals(new int[]{3, 7}, range);
    }

    // The tiles of a row are its non empty ones, by column: the first one at or after a column
    @Test
    void theFirstTileOfARowFromAColumn() {
        // Row 1 holds the tiles 3 to 6 (columns 2, 5, 6, 9), row 2 nothing
        int[] columns = {0, 1, 7, 2, 5, 6, 9, 4};
        assertEquals(3, TileVisibility.FirstInRow(columns, 3, 7, 0));
        assertEquals(3, TileVisibility.FirstInRow(columns, 3, 7, 2));
        assertEquals(4, TileVisibility.FirstInRow(columns, 3, 7, 3));
        assertEquals(6, TileVisibility.FirstInRow(columns, 3, 7, 7));
        assertEquals(7, TileVisibility.FirstInRow(columns, 3, 7, 10));
        assertEquals(7, TileVisibility.FirstInRow(columns, 7, 7, 0));
    }
}

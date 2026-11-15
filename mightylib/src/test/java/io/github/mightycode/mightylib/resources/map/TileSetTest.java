package io.github.mightycode.mightylib.resources.map;

import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

// What a tileset says about its tile ids, without a texture: the number of tiles is set as the load would
class TileSetTest {
    private static TileSet tileSet(int tilesX, int tilesY, boolean rotation, boolean flip) throws ReflectiveOperationException {
        TileSet tileSet = new TileSet("set", "set.tileset");
        Field field = TileSet.class.getDeclaredField("tileNumber");
        field.setAccessible(true);
        ((Vector2i) field.get(tileSet)).set(tilesX, tilesY);
        tileSet.setTileParameters(rotation, flip);
        return tileSet;
    }

    @Test
    void aNewTileSetIsEmpty() {
        TileSet tileSet = new TileSet("set", "set.tileset");

        assertEquals("error", tileSet.texture());
        assertEquals(0, tileSet.getTotalNumberTile());
        assertEquals(new Vector2i(0, 0), tileSet.tileSize());
        assertEquals(new Vector2i(0, 0), tileSet.tilesNumberAxis());
        assertTrue(tileSet.getAnimations().isEmpty());
    }

    @Test
    void totalNumberOfTilesCountsTheRotationsAndFlips() throws ReflectiveOperationException {
        assertEquals(12, tileSet(4, 3, false, false).getTotalNumberTile());
        assertEquals(48, tileSet(4, 3, true, false).getTotalNumberTile(), "four rotations of each tile");
        assertEquals(48, tileSet(4, 3, false, true).getTotalNumberTile(), "four flips of each tile");
        assertEquals(192, tileSet(4, 3, true, true).getTotalNumberTile());
    }

    @Test
    void tilesNumberAxisIsACopy() throws ReflectiveOperationException {
        TileSet tileSet = tileSet(4, 3, false, false);
        tileSet.tilesNumberAxis().set(99, 99);
        assertEquals(new Vector2i(4, 3), tileSet.tilesNumberAxis());
    }

    @Test
    void withoutRotationNorFlipTheIdIsTheTile() throws ReflectiveOperationException {
        TileSet tileSet = tileSet(4, 3, false, false);

        assertEquals(7, tileSet.getConvertedId(7));
        assertEquals(0, tileSet.getTileRotation(7));
        assertEquals(0, tileSet.getTileFlip(7));
    }

    @Test
    void rotationUsesTheLastTwoBitsOfTheId() throws ReflectiveOperationException {
        TileSet tileSet = tileSet(4, 3, true, false);

        // tile 5 turned 3 times is the id 5 * 4 + 3
        assertEquals(5, tileSet.getConvertedId(23));
        assertEquals(3, tileSet.getTileRotation(23));
        assertEquals(0, tileSet.getTileFlip(23));
        assertEquals(0, tileSet.getTileRotation(20));
    }

    @Test
    void flipUsesTheLastTwoBitsOfTheIdWhenThereIsNoRotation() throws ReflectiveOperationException {
        TileSet tileSet = tileSet(4, 3, false, true);

        assertEquals(5, tileSet.getConvertedId(22));
        assertEquals(2, tileSet.getTileFlip(22));
        assertEquals(0, tileSet.getTileRotation(22));
    }

    @Test
    void rotationAndFlipShareTheIdWhenBothAreOn() throws ReflectiveOperationException {
        TileSet tileSet = tileSet(4, 3, true, true);

        // tile 5, flip 2, rotation 3: 5 * 16 + 2 * 4 + 3
        int id = 5 * 16 + 2 * 4 + 3;
        assertEquals(5, tileSet.getConvertedId(id));
        assertEquals(3, tileSet.getTileRotation(id));
        assertEquals(2, tileSet.getTileFlip(id));
    }

    @Test
    void everyCombinationRoundTrips() throws ReflectiveOperationException {
        TileSet tileSet = tileSet(4, 3, true, true);
        for (int tile = 0; tile < 12; ++tile)
            for (int flip = 0; flip < 4; ++flip)
                for (int rotation = 0; rotation < 4; ++rotation) {
                    int id = tile * 16 + flip * 4 + rotation;
                    assertEquals(tile, tileSet.getConvertedId(id));
                    assertEquals(flip, tileSet.getTileFlip(id));
                    assertEquals(rotation, tileSet.getTileRotation(id));
                }
    }

    @Test
    void anEmptyTileStaysEmpty() throws ReflectiveOperationException {
        for (boolean rotation : new boolean[]{false, true}) {
            for (boolean flip : new boolean[]{false, true}) {
                TileSet tileSet = tileSet(4, 3, rotation, flip);
                assertEquals(-1, tileSet.getConvertedId(-1));
                assertEquals(0, tileSet.getTileRotation(-1));
                assertEquals(0, tileSet.getTileFlip(-1));
            }
        }
    }

    @Test
    void animationsAreFoundByTheirTile() {
        TileSet tileSet = new TileSet("set", "set.tileset");
        TileSet.TileAnimation animation = new TileSet.TileAnimation(3, 2);
        animation.ids[0] = 3;
        animation.ids[1] = 4;
        animation.times[0] = 0.5f;
        animation.times[1] = 0.25f;

        tileSet.addAnimation(3, animation);

        assertSame(animation, tileSet.getAnimation(3));
        assertNull(tileSet.getAnimation(4));
        assertEquals(1, tileSet.getAnimations().size());
        assertEquals(3, animation.getReferenceId());
        assertEquals(2, animation.getFrameCount());
        assertEquals(4, animation.getTileId(1));
        assertEquals(0.25f, animation.getTime(1));
    }

    @Test
    void contentIsExchangedWithTheReloadedCopy() throws ReflectiveOperationException {
        TileSet current = tileSet(4, 3, true, false);
        current.addAnimation(1, new TileSet.TileAnimation(1, 1));
        TileSet reloaded = tileSet(2, 2, false, true);

        current.swapContent(reloaded);

        assertEquals(new Vector2i(2, 2), current.tilesNumberAxis());
        assertEquals(16, current.getTotalNumberTile());
        assertTrue(current.getAnimations().isEmpty());
        assertEquals(new Vector2i(4, 3), reloaded.tilesNumberAxis());
        assertEquals(1, reloaded.getAnimations().size());
    }

    @Test
    void reloadCopyIsANewTileSetOfTheSameSource() {
        TileSet tileSet = new TileSet("set", "res/set.tileset");

        TileSet copy = (TileSet) tileSet.createReloadCopy();

        assertNotSame(tileSet, copy);
        assertEquals("set", copy.getDataName());
        assertEquals("res/set.tileset", copy.path());
    }

    @Test
    void notLoadableWithoutATextureAndATileSize() {
        assertFalse(new TileSet("set", "set.tileset").internLoad());
    }

    @Test
    void emptyAnimationListIsNotNull() {
        assertEquals(new ArrayList<>(), new ArrayList<>(new TileSet("set", "set.tileset").getAnimations()));
    }
}
